package com.armiplugin;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class WeaponListener implements Listener {

    // Tempo minimo (ms) tra un colpo e l'altro, per evitare spam-click
    private static final long FIRE_COOLDOWN_MS = 250;

    // Distanza massima di sparo
    private static final double MAX_DISTANCE = 60.0;

    // Soglia (in blocchi) entro cui un colpo è considerato "alla testa"
    private static final double HEADSHOT_THRESHOLD = 0.25;

    private final double bodyDamage;
    private final double headDamage;

    private final HashMap<UUID, Long> lastShot = new HashMap<>();

    public WeaponListener(double bodyDamage, double headDamage) {
        this.bodyDamage = bodyDamage;
        this.headDamage = headDamage;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // Evitiamo di gestire due volte l'evento (mano principale + secondaria)
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        switch (event.getAction()) {
            case LEFT_CLICK_AIR:
            case LEFT_CLICK_BLOCK:
                if (ItemFactory.isGlock(item)) {
                    event.setCancelled(true);
                    tryShoot(player, item);
                }
                break;

            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
                if (ItemFactory.isGlock(item)) {
                    event.setCancelled(true);
                    toggleMagazine(player, item);
                } else if (ItemFactory.isCaricatoreGlock(item)) {
                    event.setCancelled(true);
                    reloadMagazineFromAmmo(player, item);
                }
                break;

            default:
                break;
        }
    }

    // Left-click su un entità: non passa da PlayerInteractEvent ma da EntityDamageByEntityEvent
    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (ItemFactory.isGlock(item)) {
            // Il danno lo gestiamo noi via raytrace, non con il colpo melee vanilla
            event.setCancelled(true);
            tryShoot(player, item);
        }
    }

    // ---------------------------------------------------------------
    // SPARO
    // ---------------------------------------------------------------

    private void tryShoot(Player player, ItemStack weapon) {
        if (!player.isSneaking()) {
            player.sendActionBar("§cDevi essere accovacciato per sparare!");
            return;
        }

        long now = System.currentTimeMillis();
        long last = lastShot.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < FIRE_COOLDOWN_MS) {
            return; // troppo presto, ignora il click
        }

        ItemMeta meta = weapon.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        byte hasMag = pdc.getOrDefault(Keys.HAS_MAG, PersistentDataType.BYTE, (byte) 0);
        if (hasMag != 1) {
            player.sendActionBar("§cNessun caricatore inserito!");
            playEmptySound(player);
            return;
        }

        int ammo = pdc.getOrDefault(Keys.MAG_AMMO, PersistentDataType.INTEGER, 0);
        if (ammo <= 0) {
            player.sendActionBar("§cCaricatore vuoto!");
            playEmptySound(player);
            return;
        }

        // Consuma un colpo
        ammo--;
        pdc.set(Keys.MAG_AMMO, PersistentDataType.INTEGER, ammo);
        updateWeaponLore(meta, true, ammo);
        weapon.setItemMeta(meta);

        lastShot.put(player.getUniqueId(), now);

        fireEffectsAndDamage(player);
    }

    private void fireEffectsAndDamage(Player player) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection();

        player.getWorld().playSound(eye, Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 1.4f);
        player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, eye.clone().add(direction.clone().multiply(0.5)), 6, 0.02, 0.02, 0.02, 0.01);

        RayTraceResult result = player.getWorld().rayTraceEntities(
                eye,
                direction,
                MAX_DISTANCE,
                0.25,
                entity -> entity instanceof LivingEntity && !entity.equals(player)
        );

        if (result == null || result.getHitEntity() == null) {
            return;
        }

        Entity hitEntity = result.getHitEntity();
        if (!(hitEntity instanceof LivingEntity)) return;
        LivingEntity target = (LivingEntity) hitEntity;

        double hitY = result.getHitPosition().getY();
        double headY = target.getEyeLocation().getY();
        boolean headshot = Math.abs(hitY - headY) <= HEADSHOT_THRESHOLD;

        double damage = headshot ? headDamage : bodyDamage;
        target.damage(damage, player);

        if (headshot) {
            player.sendActionBar("§6§lHEADSHOT! §7(" + damage + " danni)");
        } else {
            player.sendActionBar("§7Colpito (" + damage + " danni)");
        }
    }

    private void playEmptySound(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_CROSSBOW_LOADING_MIDDLE, 1.0f, 1.8f);
    }

    // ---------------------------------------------------------------
    // CARICATORE: inserimento / espulsione
    // ---------------------------------------------------------------

    private void toggleMagazine(Player player, ItemStack weapon) {
        ItemMeta meta = weapon.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        byte hasMag = pdc.getOrDefault(Keys.HAS_MAG, PersistentDataType.BYTE, (byte) 0);

        if (hasMag == 1) {
            // ESPELLI il caricatore attuale
            int ammoLeft = pdc.getOrDefault(Keys.MAG_AMMO, PersistentDataType.INTEGER, 0);

            pdc.set(Keys.HAS_MAG, PersistentDataType.BYTE, (byte) 0);
            pdc.remove(Keys.MAG_AMMO);
            updateWeaponLore(meta, false, 0);
            weapon.setItemMeta(meta);

            ItemStack ejectedMag = ItemFactory.createCaricatoreGlock(ammoLeft);
            giveOrDrop(player, ejectedMag);

            player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 1.2f);
            player.sendActionBar("§7Caricatore espulso (" + ammoLeft + " colpi)");

        } else {
            // INSERISCI il caricatore che il giocatore tiene nella mano secondaria
            ItemStack offhand = player.getInventory().getItemInOffHand();

            if (!ItemFactory.isCaricatoreGlock(offhand)) {
                player.sendActionBar("§cMetti un Caricatore Glock nella mano secondaria!");
                return;
            }

            int ammo = getMagAmmo(offhand);

            pdc.set(Keys.HAS_MAG, PersistentDataType.BYTE, (byte) 1);
            pdc.set(Keys.MAG_AMMO, PersistentDataType.INTEGER, ammo);
            updateWeaponLore(meta, true, ammo);
            weapon.setItemMeta(meta);

            consumeOne(player, offhand, true);

            player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 0.8f);
            player.sendActionBar("§aCaricatore inserito! (" + ammo + "/" + ItemFactory.GLOCK_MAG_CAPACITY + ")");
        }
    }

    // ---------------------------------------------------------------
    // CARICATORE: riempimento con munizioni 9mm
    // ---------------------------------------------------------------

    private void reloadMagazineFromAmmo(Player player, ItemStack magazine) {
        ItemStack offhand = player.getInventory().getItemInOffHand();

        if (!ItemFactory.isMunizioni9mm(offhand)) {
            player.sendActionBar("§7Tieni dei proiettili 9mm nella mano secondaria per ricaricare!");
            return;
        }

        int current = getMagAmmo(magazine);
        if (current >= ItemFactory.GLOCK_MAG_CAPACITY) {
            player.sendActionBar("§7Il caricatore è già pieno!");
            return;
        }

        int spazioLibero = ItemFactory.GLOCK_MAG_CAPACITY - current;
        int daAggiungere = Math.min(spazioLibero, offhand.getAmount());

        int nuovoTotale = current + daAggiungere;
        ItemMeta meta = magazine.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(Keys.MAG_AMMO, PersistentDataType.INTEGER, nuovoTotale);
        updateMagazineLore(meta, nuovoTotale);
        magazine.setItemMeta(meta);

        consumeAmount(player, offhand, daAggiungere, true);

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_BUNDLE_INSERT, 1.0f, 1.5f);
        player.sendActionBar("§aRicaricati " + daAggiungere + " colpi (" + nuovoTotale + "/" + ItemFactory.GLOCK_MAG_CAPACITY + ")");
    }

    // ---------------------------------------------------------------
    // Utility
    // ---------------------------------------------------------------

    private int getMagAmmo(ItemStack magazine) {
        ItemMeta meta = magazine.getItemMeta();
        return meta.getPersistentDataContainer().getOrDefault(Keys.MAG_AMMO, PersistentDataType.INTEGER, 0);
    }

    private void updateWeaponLore(ItemMeta meta, boolean hasMag, int ammo) {
        List<String> lore = new ArrayList<>();
        if (hasMag) {
            lore.add("§7Caricatore: §b" + ammo + "/" + ItemFactory.GLOCK_MAG_CAPACITY);
        } else {
            lore.add("§7Caricatore: §cNessuno");
        }
        lore.add("§8Tasto sinistro: spara (solo accovacciato)");
        lore.add("§8Tasto destro: inserisci/espelli caricatore");
        meta.setLore(lore);
    }

    private void updateMagazineLore(ItemMeta meta, int ammo) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Colpi: §f" + ammo + "/" + ItemFactory.GLOCK_MAG_CAPACITY);
        meta.setLore(lore);
    }

    private void consumeOne(Player player, ItemStack item, boolean offhand) {
        consumeAmount(player, item, 1, offhand);
    }

    private void consumeAmount(Player player, ItemStack item, int amount, boolean offhand) {
        int remaining = item.getAmount() - amount;
        if (remaining <= 0) {
            if (offhand) {
                player.getInventory().setItemInOffHand(null);
            } else {
                item.setAmount(0);
            }
        } else {
            item.setAmount(remaining);
            if (offhand) {
                player.getInventory().setItemInOffHand(item);
            }
        }
    }

    private void giveOrDrop(Player player, ItemStack item) {
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        } else {
            player.getInventory().addItem(item);
        }
    }
}

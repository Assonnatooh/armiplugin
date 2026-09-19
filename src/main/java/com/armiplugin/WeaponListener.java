package com.armiplugin;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class WeaponListener implements Listener {

    // Delay minimo tra un colpo e l'altro (richiesto: almeno 0.3s)
    private static final long FIRE_COOLDOWN_MS = 300;

    // Distanza massima di sparo
    private static final double MAX_DISTANCE = 60.0;

    // Soglia (in blocchi) entro cui un colpo è considerato "alla testa"
    private static final double HEADSHOT_THRESHOLD = 0.25;

    // Ogni quanti tick viene aggiunto un colpo durante la ricarica del caricatore (4 tick = 0.2s, più veloce di prima)
    private static final long RELOAD_INTERVAL_TICKS = 4L;

    private final JavaPlugin plugin;
    private final double bodyDamage;
    private final double headDamage;

    private final HashMap<UUID, Long> lastShot = new HashMap<>();
    private final HashMap<UUID, BukkitTask> reloadTasks = new HashMap<>();

    // --- Stato mira (ADS) ---
    private final Set<UUID> aimingPlayers = new HashSet<>();
    private final HashMap<UUID, AimState> aimStates = new HashMap<>();

    private static class AimState {
        final int slot;
        final ItemStack originalOffhand;

        AimState(int slot, ItemStack originalOffhand) {
            this.slot = slot;
            this.originalOffhand = originalOffhand;
        }
    }

    public WeaponListener(JavaPlugin plugin, double bodyDamage, double headDamage) {
        this.plugin = plugin;
        this.bodyDamage = bodyDamage;
        this.headDamage = headDamage;
    }

    // ---------------------------------------------------------------
    // MIRA (ADS): si attiva/disattiva insieme all'accovacciarsi
    // ---------------------------------------------------------------

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if (event.isSneaking()) {
            int slot = player.getInventory().getHeldItemSlot();
            ItemStack main = player.getInventory().getItem(slot);
            if (ItemFactory.isGlock(main)) {
                startAiming(player, main, slot);
            }
        } else {
            if (aimingPlayers.contains(player.getUniqueId())) {
                stopAiming(player);
            }
        }
    }

    // Se il giocatore cambia slot in hotbar mentre è accovacciato, ripristiniamo e ricontrolliamo
    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();

        if (aimingPlayers.contains(id)) {
            stopAiming(player);
        }

        if (player.isSneaking()) {
            ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
            if (ItemFactory.isGlock(newItem)) {
                startAiming(player, newItem, event.getNewSlot());
            }
        }
    }

    private void startAiming(Player player, ItemStack weaponItem, int slot) {
        UUID id = player.getUniqueId();
        if (aimingPlayers.contains(id)) return;

        ItemStack offhandBefore = player.getInventory().getItemInOffHand();
        aimStates.put(id, new AimState(slot, offhandBefore));

        ItemStack weaponClone = weaponItem.clone();
        player.getInventory().setItem(slot, ItemFactory.createSightItem());
        player.getInventory().setItemInOffHand(weaponClone);

        aimingPlayers.add(id);
    }

    private void stopAiming(Player player) {
        UUID id = player.getUniqueId();
        AimState state = aimStates.remove(id);
        aimingPlayers.remove(id);
        if (state == null) return;

        ItemStack offhandNow = player.getInventory().getItemInOffHand();
        ItemStack atSlot = player.getInventory().getItem(state.slot);

        if (ItemFactory.isGlock(offhandNow) && ItemFactory.isSightItem(atSlot)) {
            // caso normale: rimettiamo l'arma nello slot originale e ripristiniamo l'offhand
            player.getInventory().setItem(state.slot, offhandNow);
            player.getInventory().setItemInOffHand(state.originalOffhand);
        } else {
            // caso limite (il giocatore ha spostato gli item mentre mirava): best-effort
            if (ItemFactory.isGlock(offhandNow)) {
                giveOrDrop(player, offhandNow);
                player.getInventory().setItemInOffHand(state.originalOffhand);
            }
            if (ItemFactory.isSightItem(atSlot)) {
                player.getInventory().setItem(state.slot, null);
            }
        }
    }

    private boolean isAiming(Player player) {
        return aimingPlayers.contains(player.getUniqueId());
    }

    /** Ritorna l'ItemStack dell'arma attualmente "attiva" (mano principale normalmente, mano secondaria se in mira). */
    private ItemStack getActiveWeapon(Player player) {
        if (isAiming(player)) {
            ItemStack off = player.getInventory().getItemInOffHand();
            return ItemFactory.isGlock(off) ? off : null;
        } else {
            ItemStack main = player.getInventory().getItemInMainHand();
            return ItemFactory.isGlock(main) ? main : null;
        }
    }

    private void saveActiveWeapon(Player player, ItemStack weapon) {
        if (isAiming(player)) {
            player.getInventory().setItemInOffHand(weapon);
        } else {
            player.getInventory().setItemInMainHand(weapon);
        }
    }

    private boolean isHoldingWeaponOrSight(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (ItemFactory.isGlock(main)) return true;
        return isAiming(player) && ItemFactory.isSightItem(main);
    }

    // ---------------------------------------------------------------
    // INTERAZIONI
    // ---------------------------------------------------------------

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        switch (event.getAction()) {
            case LEFT_CLICK_AIR:
            case LEFT_CLICK_BLOCK:
                if (ItemFactory.isGlock(item) || (isAiming(player) && ItemFactory.isSightItem(item))) {
                    event.setCancelled(true);
                    tryShoot(player);
                }
                break;

            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
                if (ItemFactory.isGlock(item)) {
                    event.setCancelled(true);
                    toggleMagazine(player, item);
                } else if (ItemFactory.isCaricatoreGlock(item)) {
                    event.setCancelled(true);
                    toggleReload(player);
                } else if (ItemFactory.isSightItem(item)) {
                    event.setCancelled(true); // il mirino non fa nulla al tasto destro
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

        if (isHoldingWeaponOrSight(player)) {
            event.setCancelled(true);
            tryShoot(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        stopReload(player.getUniqueId(), null);
        if (isAiming(player)) {
            stopAiming(player);
        }
    }

    // ---------------------------------------------------------------
    // SPARO
    // ---------------------------------------------------------------

    private void tryShoot(Player player) {
        ItemStack weapon = getActiveWeapon(player);
        if (weapon == null) return;

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
        saveActiveWeapon(player, weapon);

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
                entity -> entity instanceof Player && !entity.equals(player)
        );

        if (result == null || result.getHitEntity() == null) {
            return;
        }

        // Danno SOLO ai giocatori, i mob non vengono colpiti
        if (!(result.getHitEntity() instanceof Player)) {
            return;
        }
        Player target = (Player) result.getHitEntity();

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
    // CARICATORE: inserimento / espulsione nell'arma
    // ---------------------------------------------------------------

    private void toggleMagazine(Player player, ItemStack weapon) {
        ItemMeta meta = weapon.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        byte hasMag = pdc.getOrDefault(Keys.HAS_MAG, PersistentDataType.BYTE, (byte) 0);

        if (hasMag == 1) {
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

            consumeOneOffhand(player, offhand);

            player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 0.8f);
            player.sendActionBar("§aCaricatore inserito! (" + ammo + "/" + ItemFactory.GLOCK_MAG_CAPACITY + ")");
        }
    }

    // ---------------------------------------------------------------
    // CARICATORE: riempimento graduale con munizioni 9mm dall'inventario
    // ---------------------------------------------------------------

    private void toggleReload(Player player) {
        UUID id = player.getUniqueId();

        if (reloadTasks.containsKey(id)) {
            stopReload(id, "§7Ricarica interrotta.");
            return;
        }

        int heldSlot = player.getInventory().getHeldItemSlot();
        ItemStack magazine = player.getInventory().getItem(heldSlot);

        if (!ItemFactory.isCaricatoreGlock(magazine)) {
            return;
        }

        int current = getMagAmmo(magazine);
        if (current >= ItemFactory.GLOCK_MAG_CAPACITY) {
            player.sendActionBar("§7Il caricatore è già pieno!");
            return;
        }
        if (!hasAnyAmmo(player)) {
            player.sendActionBar("§cNon hai proiettili 9mm nell'inventario!");
            return;
        }

        player.sendActionBar("§eInizio la ricarica...");

        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    reloadTasks.remove(id);
                    cancel();
                    return;
                }

                ItemStack current = player.getInventory().getItem(heldSlot);
                if (!ItemFactory.isCaricatoreGlock(current) || player.getInventory().getHeldItemSlot() != heldSlot) {
                    reloadTasks.remove(id);
                    player.sendActionBar("§7Ricarica interrotta.");
                    cancel();
                    return;
                }

                int ammoNow = getMagAmmo(current);

                if (ammoNow >= ItemFactory.GLOCK_MAG_CAPACITY) {
                    reloadTasks.remove(id);
                    player.sendActionBar("§aCaricatore pieno! (" + ammoNow + "/" + ItemFactory.GLOCK_MAG_CAPACITY + ")");
                    cancel();
                    return;
                }

                if (!hasAnyAmmo(player)) {
                    reloadTasks.remove(id);
                    player.sendActionBar("§cMunizioni 9mm finite! (" + ammoNow + "/" + ItemFactory.GLOCK_MAG_CAPACITY + ")");
                    cancel();
                    return;
                }

                consumeOneAmmoFromInventory(player);
                int newAmmo = ammoNow + 1;

                ItemMeta magMeta = current.getItemMeta();
                magMeta.getPersistentDataContainer().set(Keys.MAG_AMMO, PersistentDataType.INTEGER, newAmmo);
                updateMagazineLore(magMeta, newAmmo);
                current.setItemMeta(magMeta);
                player.getInventory().setItem(heldSlot, current);

                player.getWorld().playSound(player.getLocation(), Sound.ITEM_BUNDLE_INSERT, 1.0f, 1.8f);
                player.sendActionBar("§7Ricarica... " + newAmmo + "/" + ItemFactory.GLOCK_MAG_CAPACITY);

                if (newAmmo >= ItemFactory.GLOCK_MAG_CAPACITY) {
                    reloadTasks.remove(id);
                    player.sendActionBar("§aCaricatore pieno! (" + newAmmo + "/" + ItemFactory.GLOCK_MAG_CAPACITY + ")");
                    cancel();
                }
            }
        };

        BukkitTask task = runnable.runTaskTimer(plugin, RELOAD_INTERVAL_TICKS, RELOAD_INTERVAL_TICKS);
        reloadTasks.put(id, task);
    }

    private void stopReload(UUID id, String message) {
        BukkitTask task = reloadTasks.remove(id);
        if (task != null) {
            task.cancel();
        }
        if (message != null) {
            Player player = plugin.getServer().getPlayer(id);
            if (player != null) {
                player.sendActionBar(message);
            }
        }
    }

    // ---------------------------------------------------------------
    // Utility
    // ---------------------------------------------------------------

    private boolean hasAnyAmmo(Player player) {
        PlayerInventory inv = player.getInventory();
        for (ItemStack it : inv.getStorageContents()) {
            if (ItemFactory.isMunizioni9mm(it) && it.getAmount() > 0) return true;
        }
        ItemStack off = inv.getItemInOffHand();
        return ItemFactory.isMunizioni9mm(off) && off.getAmount() > 0;
    }

    private void consumeOneAmmoFromInventory(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getStorageContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (ItemFactory.isMunizioni9mm(it) && it.getAmount() > 0) {
                it.setAmount(it.getAmount() - 1);
                contents[i] = it.getAmount() <= 0 ? null : it;
                inv.setStorageContents(contents);
                return;
            }
        }

        ItemStack off = inv.getItemInOffHand();
        if (ItemFactory.isMunizioni9mm(off) && off.getAmount() > 0) {
            off.setAmount(off.getAmount() - 1);
            inv.setItemInOffHand(off.getAmount() <= 0 ? null : off);
        }
    }

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
        lore.add("§8Tasto destro: ricarica dai 9mm nell'inventario");
        meta.setLore(lore);
    }

    private void consumeOneOffhand(Player player, ItemStack offhandItem) {
        int remaining = offhandItem.getAmount() - 1;
        if (remaining <= 0) {
            player.getInventory().setItemInOffHand(null);
        } else {
            offhandItem.setAmount(remaining);
            player.getInventory().setItemInOffHand(offhandItem);
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

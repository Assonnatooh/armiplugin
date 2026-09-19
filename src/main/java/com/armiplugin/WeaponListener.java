package com.armiplugin;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
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

    private static final long FIRE_COOLDOWN_MS = 300;
    private static final double MAX_DISTANCE = 60.0;
    private static final double HEADSHOT_THRESHOLD = 0.25;
    private static final long RELOAD_INTERVAL_TICKS = 4L;

    private final JavaPlugin plugin;
    private final double defaultBodyDamage;
    private final double defaultHeadDamage;

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

    public WeaponListener(JavaPlugin plugin, double defaultBodyDamage, double defaultHeadDamage) {
        this.plugin = plugin;
        this.defaultBodyDamage = defaultBodyDamage;
        this.defaultHeadDamage = defaultHeadDamage;
    }

    // ---------------------------------------------------------------
    // MIRA (ADS)
    // ---------------------------------------------------------------

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if (event.isSneaking()) {
            int slot = player.getInventory().getHeldItemSlot();
            ItemStack main = player.getInventory().getItem(slot);
            if (isAnyWeapon(main)) {
                startAiming(player, main, slot);
            }
        } else {
            if (aimingPlayers.contains(player.getUniqueId())) {
                stopAiming(player);
            }
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();

        if (aimingPlayers.contains(id)) {
            stopAiming(player);
        }

        if (player.isSneaking()) {
            ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
            if (isAnyWeapon(newItem)) {
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
        
        // Assegna il mirino corrispondente
        ItemStack sightItem;
        if (ItemFactory.isPx4(weaponItem)) {
            sightItem = ItemFactory.createPx4SightItem();
        } else if (ItemFactory.isBeretta(weaponItem)) {
            sightItem = ItemFactory.createBerettaSightItem();
        } else {
            sightItem = ItemFactory.createSightItem();
        }
        
        player.getInventory().setItem(slot, sightItem);
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

        if (isAnyWeapon(offhandNow) && isAnySightItem(atSlot)) {
            player.getInventory().setItem(state.slot, offhandNow);
            player.getInventory().setItemInOffHand(state.originalOffhand);
        } else {
            if (isAnyWeapon(offhandNow)) {
                giveOrDrop(player, offhandNow);
                player.getInventory().setItemInOffHand(state.originalOffhand);
            }
            if (isAnySightItem(atSlot)) {
                player.getInventory().setItem(state.slot, null);
            }
        }
    }

    private boolean isAiming(Player player) {
        return aimingPlayers.contains(player.getUniqueId());
    }

    private ItemStack getActiveWeapon(Player player) {
        if (isAiming(player)) {
            ItemStack off = player.getInventory().getItemInOffHand();
            return isAnyWeapon(off) ? off : null;
        } else {
            ItemStack main = player.getInventory().getItemInMainHand();
            return isAnyWeapon(main) ? main : null;
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
        if (isAnyWeapon(main)) return true;
        return isAiming(player) && isAnySightItem(main);
    }

    private boolean isAnyWeapon(ItemStack item) {
        return ItemFactory.isGlock(item) || ItemFactory.isBeretta(item) || ItemFactory.isPx4(item);
    }

    private boolean isAnySightItem(ItemStack item) {
        return ItemFactory.isSightItem(item) || ItemFactory.isBerettaSightItem(item) || ItemFactory.isPx4SightItem(item);
    }

    private boolean isAnyMagazine(ItemStack item) {
        return ItemFactory.isCaricatoreGlock(item) || ItemFactory.isCaricatoreBeretta(item) || ItemFactory.isCaricatorePx4(item);
    }

    private int getMaxCapacity(ItemStack weaponOrMag) {
        if (ItemFactory.isPx4(weaponOrMag) || ItemFactory.isCaricatorePx4(weaponOrMag)) {
            return ItemFactory.PX4_MAG_CAPACITY;
        } else if (ItemFactory.isBeretta(weaponOrMag) || ItemFactory.isCaricatoreBeretta(weaponOrMag)) {
            return ItemFactory.BERETTA_MAG_CAPACITY;
        }
        return ItemFactory.GLOCK_MAG_CAPACITY;
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
                if (isAnyWeapon(item) || (isAiming(player) && isAnySightItem(item))) {
                    event.setCancelled(true);
                    tryShoot(player);
                }
                break;

            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
                if (isAnyWeapon(item)) {
                    event.setCancelled(true);
                    toggleMagazine(player, item);
                } else if (isAnyMagazine(item)) {
                    event.setCancelled(true);
                    toggleReload(player);
                } else if (isAnySightItem(item)) {
                    event.setCancelled(true);
                }
                break;

            default:
                break;
        }
    }

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
        if (weapon == null || !player.isSneaking()) return;

        long now = System.currentTimeMillis();
        long last = lastShot.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < FIRE_COOLDOWN_MS) return;

        ItemMeta meta = weapon.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        byte hasMag = pdc.getOrDefault(Keys.HAS_MAG, PersistentDataType.BYTE, (byte) 0);
        if (hasMag != 1) {
            playEmptySound(player);
            return;
        }

        int ammo = pdc.getOrDefault(Keys.MAG_AMMO, PersistentDataType.INTEGER, 0);
        if (ammo <= 0) {
            player.sendActionBar("§cCaricatore vuoto!");
            playEmptySound(player);
            return;
        }

        ammo--;
        pdc.set(Keys.MAG_AMMO, PersistentDataType.INTEGER, ammo);
        
        int maxCapacity = getMaxCapacity(weapon);
        updateWeaponLore(meta, true, ammo, maxCapacity);
        
        weapon.setItemMeta(meta);
        saveActiveWeapon(player, weapon);

        lastShot.put(player.getUniqueId(), now);

        fireEffectsAndDamage(player, weapon);
    }

    private void fireEffectsAndDamage(Player player, ItemStack weapon) {
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

        if (result == null || result.getHitEntity() == null) return;
        if (!(result.getHitEntity() instanceof Player)) return;

        Player target = (Player) result.getHitEntity();

        double hitY = result.getHitPosition().getY();
        double headY = target.getEyeLocation().getY();
        boolean headshot = Math.abs(hitY - headY) <= HEADSHOT_THRESHOLD;

        // Calcolo danni differenziati
        double damage;
        if (ItemFactory.isPx4(weapon)) {
            damage = headshot ? 2.3 : 1.6; // Danni Beretta PX4
        } else if (ItemFactory.isBeretta(weapon)) {
            damage = headshot ? 1.8 : 1.2; // Danni Beretta 92FS
        } else {
            damage = headshot ? defaultHeadDamage : defaultBodyDamage; // Danni Glock
        }

        target.damage(damage, player);
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
        int maxCapacity = getMaxCapacity(weapon);

        if (hasMag == 1) {
            int ammoLeft = pdc.getOrDefault(Keys.MAG_AMMO, PersistentDataType.INTEGER, 0);

            pdc.set(Keys.HAS_MAG, PersistentDataType.BYTE, (byte) 0);
            pdc.remove(Keys.MAG_AMMO);
            updateWeaponLore(meta, false, 0, maxCapacity);
            weapon.setItemMeta(meta);

            ItemStack ejectedMag;
            if (ItemFactory.isPx4(weapon)) {
                ejectedMag = ItemFactory.createCaricatorePx4(ammoLeft);
            } else if (ItemFactory.isBeretta(weapon)) {
                ejectedMag = ItemFactory.createCaricatoreBeretta(ammoLeft);
            } else {
                ejectedMag = ItemFactory.createCaricatoreGlock(ammoLeft);
            }
            
            giveOrDrop(player, ejectedMag);

            player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 1.2f);
            player.sendActionBar("§7Caricatore espulso (" + ammoLeft + " colpi)");

        } else {
            ItemStack offhand = player.getInventory().getItemInOffHand();

            if (ItemFactory.isPx4(weapon) && !ItemFactory.isCaricatorePx4(offhand)) {
                player.sendActionBar("§cMetti un Caricatore PX4 nella mano secondaria!");
                return;
            } else if (ItemFactory.isBeretta(weapon) && !ItemFactory.isCaricatoreBeretta(offhand)) {
                player.sendActionBar("§cMetti un Caricatore 92FS nella mano secondaria!");
                return;
            } else if (ItemFactory.isGlock(weapon) && !ItemFactory.isCaricatoreGlock(offhand)) {
                player.sendActionBar("§cMetti un Caricatore Glock nella mano secondaria!");
                return;
            }

            int ammo = getMagAmmo(offhand);

            pdc.set(Keys.HAS_MAG, PersistentDataType.BYTE, (byte) 1);
            pdc.set(Keys.MAG_AMMO, PersistentDataType.INTEGER, ammo);
            updateWeaponLore(meta, true, ammo, maxCapacity);
            weapon.setItemMeta(meta);

            consumeOneOffhand(player, offhand);

            player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 0.8f);
            player.sendActionBar("§aCaricatore inserito! (" + ammo + "/" + maxCapacity + ")");
        }
    }

    // ---------------------------------------------------------------
    // CARICATORE: riempimento graduale
    // ---------------------------------------------------------------

    private void toggleReload(Player player) {
        UUID id = player.getUniqueId();

        if (reloadTasks.containsKey(id)) {
            stopReload(id, "§7Ricarica interrotta.");
            return;
        }

        int heldSlot = player.getInventory().getHeldItemSlot();
        ItemStack magazine = player.getInventory().getItem(heldSlot);

        if (!isAnyMagazine(magazine)) return;

        int maxCapacity = getMaxCapacity(magazine);

        int current = getMagAmmo(magazine);
        if (current >= maxCapacity) {
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

                ItemStack currentItem = player.getInventory().getItem(heldSlot);
                if (!isAnyMagazine(currentItem) || player.getInventory().getHeldItemSlot() != heldSlot) {
                    reloadTasks.remove(id);
                    player.sendActionBar("§7Ricarica interrotta.");
                    cancel();
                    return;
                }

                int ammoNow = getMagAmmo(currentItem);

                if (ammoNow >= maxCapacity) {
                    reloadTasks.remove(id);
                    player.sendActionBar("§aCaricatore pieno! (" + ammoNow + "/" + maxCapacity + ")");
                    cancel();
                    return;
                }

                if (!hasAnyAmmo(player)) {
                    reloadTasks.remove(id);
                    player.sendActionBar("§cMunizioni 9mm finite! (" + ammoNow + "/" + maxCapacity + ")");
                    cancel();
                    return;
                }

                consumeOneAmmoFromInventory(player);
                int newAmmo = ammoNow + 1;

                ItemMeta magMeta = currentItem.getItemMeta();
                magMeta.getPersistentDataContainer().set(Keys.MAG_AMMO, PersistentDataType.INTEGER, newAmmo);
                updateMagazineLore(magMeta, newAmmo, maxCapacity);
                currentItem.setItemMeta(magMeta);
                player.getInventory().setItem(heldSlot, currentItem);

                player.getWorld().playSound(player.getLocation(), Sound.ITEM_BUNDLE_INSERT, 1.0f, 1.8f);
                player.sendActionBar("§7Ricarica... " + newAmmo + "/" + maxCapacity);

                if (newAmmo >= maxCapacity) {
                    reloadTasks.remove(id);
                    player.sendActionBar("§aCaricatore pieno! (" + newAmmo + "/" + maxCapacity + ")");
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

    private void updateWeaponLore(ItemMeta meta, boolean hasMag, int ammo, int maxCapacity) {
        List<String> lore = new ArrayList<>();
        if (hasMag) {
            lore.add("§7Caricatore: §b" + ammo + "/" + maxCapacity);
        } else {
            lore.add("§7Caricatore: §cNessuno");
        }
        lore.add("§8Tasto sinistro: spara (solo accovacciato)");
        lore.add("§8Tasto destro: inserisci/espelli caricatore");
        meta.setLore(lore);
    }

    private void updateMagazineLore(ItemMeta meta, int ammo, int maxCapacity) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Colpi: §f" + ammo + "/" + maxCapacity);
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

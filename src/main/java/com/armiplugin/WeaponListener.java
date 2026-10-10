package com.armiplugin;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
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

    private static final double MAX_DISTANCE = 50.0;
    private static final double HEADSHOT_THRESHOLD = 0.35;
    private static final long RELOAD_INTERVAL_TICKS = 5L;

    private final JavaPlugin plugin;
    private final HashMap<UUID, Long> lastShot = new HashMap<>();
    private final HashMap<UUID, BukkitTask> reloadTasks = new HashMap<>();
    private final Set<UUID> aimingPlayers = new HashSet<>();
    private final HashMap<UUID, AimState> aimStatesMap = new HashMap<>();

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
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (event.isSneaking()) {
            int slot = player.getInventory().getHeldItemSlot();
            ItemStack main = player.getInventory().getItem(slot);
            if (isAnyWeapon(main)) startAiming(player, main, slot);
        } else {
            if (aimingPlayers.contains(player.getUniqueId())) stopAiming(player);
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (aimingPlayers.contains(id)) stopAiming(player);
        if (player.isSneaking()) {
            ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
            if (isAnyWeapon(newItem)) startAiming(player, newItem, event.getNewSlot());
        }
    }

    private void startAiming(Player player, ItemStack weaponItem, int slot) {
        UUID id = player.getUniqueId();
        if (aimingPlayers.contains(id)) return;
        ItemStack offhandBefore = player.getInventory().getItemInOffHand();
        aimStatesMap.put(id, new AimState(slot, offhandBefore));
        ItemStack weaponClone = weaponItem.clone();
        ItemStack sightItem;
        if (ItemFactory.isPx4(weaponItem)) sightItem = ItemFactory.createPx4SightItem();
        else if (ItemFactory.isBeretta(weaponItem)) sightItem = ItemFactory.createBerettaSightItem();
        else sightItem = ItemFactory.createSightItem();
        player.getInventory().setItem(slot, sightItem);
        player.getInventory().setItemInOffHand(weaponClone);
        aimingPlayers.add(id);
    }

    private void stopAiming(Player player) {
        UUID id = player.getUniqueId();
        AimState state = aimStatesMap.remove(id);
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
            if (isAnySightItem(atSlot)) player.getInventory().setItem(state.slot, null);
        }
    }

    private boolean isAiming(Player player) {
        return aimingPlayers.contains(player.getUniqueId());
    }

    private ItemStack getActiveWeapon(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (isAnyWeapon(main)) return main;
        ItemStack off = player.getInventory().getItemInOffHand();
        if (isAnyWeapon(off)) return off;
        return null;
    }

    private void saveActiveWeapon(Player player, ItemStack weapon) {
        if (isAiming(player)) player.getInventory().setItemInOffHand(weapon);
        else player.getInventory().setItemInMainHand(weapon);
    }

    private boolean isHoldingWeaponOrSight(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        return isAnyWeapon(main) || isAnySightItem(main) || isAnyWeapon(off);
    }

    private boolean isAnyWeapon(ItemStack item) {
        if (item == null) return false;
        return ItemFactory.isGlock(item) || ItemFactory.isBeretta(item) || ItemFactory.isPx4(item);
    }

    private boolean isAnySightItem(ItemStack item) {
        if (item == null) return false;
        return ItemFactory.isSightItem(item) || ItemFactory.isBerettaSightItem(item) || ItemFactory.isPx4SightItem(item);
    }

    private boolean isAnyMagazine(ItemStack item) {
        if (item == null) return false;
        return ItemFactory.isCaricatoreGlock(item) || ItemFactory.isCaricatoreBeretta(item) || ItemFactory.isCaricatorePx4(item);
    }

    private int getMaxCapacity(ItemStack weaponOrMag) {
        if (ItemFactory.isPx4(weaponOrMag) || ItemFactory.isCaricatorePx4(weaponOrMag)) return ItemFactory.PX4_MAG_CAPACITY;
        else if (ItemFactory.isBeretta(weaponOrMag) || ItemFactory.isCaricatoreBeretta(weaponOrMag)) return ItemFactory.BERETTA_MAG_CAPACITY;
        return ItemFactory.GLOCK_MAG_CAPACITY;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (isHoldingWeaponOrSight(player)) {
                event.setCancelled(true);
                tryShoot(player);
            }
        } else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (isHoldingWeaponOrSight(player)) event.setCancelled(true);
            if (item != null) {
                if (isAnyWeapon(item)) {
                    toggleMagazine(player, item);
                } else if (isAnyMagazine(item)) {
                    // Se il giocatore fa shift + click destro sul caricatore, lo scarica completamente
                    if (player.isSneaking()) {
                        unloadMagazine(player, item);
                    } else {
                        toggleReload(player);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
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
        stopReload(player.getUniqueId());
        if (isAiming(player)) stopAiming(player);
    }

    private void tryShoot(Player player) {
        if (!player.isSneaking()) return;
        ItemStack weapon = getActiveWeapon(player);
        if (weapon == null) return;

        long cooldownMs = ItemFactory.isPx4(weapon) ? 400 : (ItemFactory.isGlock(weapon) ? 200 : 300);
        long now = System.currentTimeMillis();
        long last = lastShot.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < cooldownMs) return;

        ItemMeta meta = weapon.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        byte hasMag = pdc.getOrDefault(Keys.HAS_MAG, PersistentDataType.BYTE, (byte) 0);
        if (hasMag != 1) {
            playEmptySound(player);
            lastShot.put(player.getUniqueId(), now);
            return;
        }

        int ammo = pdc.getOrDefault(Keys.MAG_AMMO, PersistentDataType.INTEGER, 0);
        if (ammo <= 0) {
            playEmptySound(player);
            lastShot.put(player.getUniqueId(), now);
            return;
        }

        ammo--;
        pdc.set(Keys.MAG_AMMO, PersistentDataType.INTEGER, ammo);
        updateWeaponLore(meta, true, ammo, getMaxCapacity(weapon));
        weapon.setItemMeta(meta);
        saveActiveWeapon(player, weapon);
        lastShot.put(player.getUniqueId(), now);

        fireEffectsAndDamage(player, weapon);
    }

    private void fireEffectsAndDamage(Player player, ItemStack weapon) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        player.getWorld().playSound(eye, "bulletlow", 1.0f, 1.0f);

        for (double d = 0.5; d <= MAX_DISTANCE; d += 0.5) {
            Location point = eye.clone().add(direction.clone().multiply(d));
            player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, point, 1, 0.01, 0.01, 0.01, 0.0);
        }

        RayTraceResult result = player.getWorld().rayTraceEntities(
                eye, direction, MAX_DISTANCE, 2.0,
                entity -> entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())
        );

        if (result == null || result.getHitEntity() == null) return;
        if (!(result.getHitEntity() instanceof LivingEntity)) return;

        LivingEntity target = (LivingEntity) result.getHitEntity();
        boolean headshot = Math.abs(result.getHitPosition().getY() - target.getEyeLocation().getY()) <= HEADSHOT_THRESHOLD;

        // Danni richiesti (in mezzi cuori):
        // Glock: Corpo = 2.0 (1 cuore), Testa = 4.0 (2 cuori)
        // Beretta (FS): Corpo = 4.0 (2 cuori), Testa = 6.0 (3 cuori)
        // PX4: Corpo = 6.0 (3 cuori), Testa = 8.0 (4 cuori)
        double damage;
        if (ItemFactory.isPx4(weapon)) {
            damage = headshot ? 8.0 : 6.0;
        } else if (ItemFactory.isBeretta(weapon)) {
            damage = headshot ? 6.0 : 4.0;
        } else {
            damage = headshot ? 4.0 : 2.0;
        }

        // Applicazione danno con zero knockback (annullando la spinta) e tilt damage pulito
        new BukkitRunnable() {
            @Override
            public void run() {
                if (target.isValid() && !target.isDead()) {
                    double newHealth = Math.max(0, target.getHealth() - damage);
                    target.setNoDamageTicks(0);
                    target.setHealth(newHealth);
                    target.playEffect(org.bukkit.EntityEffect.HURT);
                    
                    // Rimuove totalmente il knockback azzerando la velocità orizzontale della spinta
                    target.setVelocity(new Vector(0, target.getVelocity().getY(), 0));
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    private void playEmptySound(Player player) {
        player.getWorld().playSound(player.getLocation(), "click", 1.0f, 1.0f);
    }

    private void toggleMagazine(Player player, ItemStack weapon) {
        ItemMeta meta = weapon.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        byte hasMag = pdc.getOrDefault(Keys.HAS_MAG, PersistentDataType.BYTE, (byte) 0);
        int maxCapacity = getMaxCapacity(weapon);

        if (hasMag == 1) {
            int ammoLeft = pdc.getOrDefault(Keys.MAG_AMMO, PersistentDataType.INTEGER, 0);
            pdc.set(Keys.HAS_MAG, PersistentDataType.BYTE, (byte) 0);
            pdc.remove(Keys.MAG_AMMO);
            updateWeaponLore(meta, false, 0, maxCapacity);
            weapon.setItemMeta(meta);

            ItemStack ejectedMag = ItemFactory.isPx4(weapon) ? ItemFactory.createCaricatorePx4(ammoLeft) :
                                  (ItemFactory.isBeretta(weapon) ? ItemFactory.createCaricatoreBeretta(ammoLeft) : ItemFactory.createCaricatoreGlock(ammoLeft));
            giveOrDrop(player, ejectedMag);
            player.getWorld().playSound(player.getLocation(), "click", 1.0f, 1.0f);
        } else {
            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (ItemFactory.isPx4(weapon) && !ItemFactory.isCaricatorePx4(offhand)) return;
            if (ItemFactory.isBeretta(weapon) && !ItemFactory.isCaricatoreBeretta(offhand)) return;
            if (ItemFactory.isGlock(weapon) && !ItemFactory.isCaricatoreGlock(offhand)) return;

            int ammo = getMagAmmo(offhand);
            pdc.set(Keys.HAS_MAG, PersistentDataType.BYTE, (byte) 1);
            pdc.set(Keys.MAG_AMMO, PersistentDataType.INTEGER, ammo);
            updateWeaponLore(meta, true, ammo, maxCapacity);
            weapon.setItemMeta(meta);
            consumeOneOffhand(player, offhand);
            player.getWorld().playSound(player.getLocation(), "click", 1.0f, 1.0f);
        }
    }

    // Metodo per scaricare il caricatore (Shift + Click destro sul caricatore in mano)
    private void unloadMagazine(Player player, ItemStack magazine) {
        ItemMeta meta = magazine.getItemMeta();
        if (meta == null) return;
        int currentAmmo = getMagAmmo(magazine);
        if (currentAmmo <= 0) return;

        // Resetta i colpi nel caricatore a 0
        meta.getPersistentDataContainer().set(Keys.MAG_AMMO, PersistentDataType.INTEGER, 0);
        updateMagazineLore(meta, 0, getMaxCapacity(magazine));
        magazine.setItemMeta(meta);

        // Restituisce le munizioni 9mm all'inventario del player sotto forma di item
        ItemStack ammoDrop = ItemFactory.createMunizioni9mm();
        ammoDrop.setAmount(currentAmmo);
        giveOrDrop(player, ammoDrop);

        player.getWorld().playSound(player.getLocation(), "entity.item.break", 1.0f, 1.0f);
        player.sendMessage("§aCaricatore scaricato con successo!");
    }

    private void toggleReload(Player player) {
        UUID id = player.getUniqueId();
        if (reloadTasks.containsKey(id)) {
            stopReload(id);
            return;
        }

        int heldSlot = player.getInventory().getHeldItemSlot();
        ItemStack magazine = player.getInventory().getItem(heldSlot);
        if (!isAnyMagazine(magazine)) return;

        int maxCapacity = getMaxCapacity(magazine);
        int current = getMagAmmo(magazine);
        if (current >= maxCapacity || !hasAnyAmmo(player)) return;

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
                    cancel();
                    return;
                }

                int ammoNow = getMagAmmo(currentItem);
                if (ammoNow >= maxCapacity || !hasAnyAmmo(player)) {
                    reloadTasks.remove(id);
                    cancel();
                    return;
                }

                consumeOneAmmoFromInventory(player);
                int newAmmo = ammoNow + 1;

                ItemMeta magMeta = currentItem.getItemMeta();
                if (magMeta != null) {
                    magMeta.getPersistentDataContainer().set(Keys.MAG_AMMO, PersistentDataType.INTEGER, newAmmo);
                    updateMagazineLore(magMeta, newAmmo, maxCapacity);
                    currentItem.setItemMeta(magMeta);
                    player.getInventory().setItem(heldSlot, currentItem);
                }

                player.getWorld().playSound(player.getLocation(), "click", 1.0f, 1.2f);
                if (newAmmo >= maxCapacity) {
                    reloadTasks.remove(id);
                    cancel();
                }
            }
        };

        BukkitTask task = runnable.runTaskTimer(plugin, RELOAD_INTERVAL_TICKS, RELOAD_INTERVAL_TICKS);
        reloadTasks.put(id, task);
    }

    private void stopReload(UUID id) {
        BukkitTask task = reloadTasks.remove(id);
        if (task != null) task.cancel();
    }

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
        if (magazine == null || magazine.getItemMeta() == null) return 0;
        ItemMeta meta = magazine.getItemMeta();
        return meta.getPersistentDataContainer().getOrDefault(Keys.MAG_AMMO, PersistentDataType.INTEGER, 0);
    }

    private void updateWeaponLore(ItemMeta meta, boolean hasMag, int ammo, int maxCapacity) {
        List<String> lore = new ArrayList<>();
        if (hasMag) lore.add("§7Caricatore: §b" + ammo + "/" + maxCapacity);
        else lore.add("§7Caricatore: §cNessuno");
        meta.setLore(lore);
    }

    private void updateMagazineLore(ItemMeta meta, int ammo, int maxCapacity) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Colpi: §f" + ammo + "/" + maxCapacity);
        meta.setLore(lore);
    }

    private void consumeOneOffhand(Player player, ItemStack offhandItem) {
        int remaining = offhandItem.getAmount() - 1;
        if (remaining <= 0) player.getInventory().setItemInOffHand(null);
        else {
            offhandItem.setAmount(remaining);
            player.getInventory().setItemInOffHand(offhandItem);
        }
    }

    private void giveOrDrop(Player player, ItemStack item) {
        if (player.getInventory().firstEmpty() == -1) player.getWorld().dropItemNaturally(player.getLocation(), item);
        else player.getInventory().addItem(item);
    }
}

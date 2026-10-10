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
                if (isAnyWeapon(item)) toggleMagazine(player, item);
                else if (isAnyMagazine(item)) toggleReload(player);
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
        player.sendMessage("§a[DEBUG] Sparo partito!");

        for (double d = 0.5; d <= MAX_DISTANCE; d += 0.5) {
            Location point = eye.clone().add(direction.clone().multiply(d));
            player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, point, 1, 0.01, 0.01, 0.01, 0.0);
        }

        RayTraceResult result = player.getWorld().rayTraceEntities(
                eye, direction, MAX_DISTANCE, 2.0,
                entity -> entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())
        );

        if (result == null || result.getHitEntity() == null) {
            player.sendMessage("§c[DEBUG] RayTrace fallito: nessun mob trovato sulla linea di mira!");
            return;
        }

        if (!(result.getHitEntity() instanceof LivingEntity)) {
            player.sendMessage("§c[DEBUG] L'entità colpita non è un LivingEntity!");
            return;
        }

        LivingEntity target = (LivingEntity) result.getHitEntity();
        player.sendMessage("§e[DEBUG] Bersaglio trovato: " + target.getType().name());

        boolean headshot = Math.abs(result.getHitPosition().getY() - target.getEyeLocation().getY()) <= HEADSHOT_THRESHOLD;

        double damage = ItemFactory.isPx4(weapon) ? (headshot ? 3.2 : 2.5) :
                        (ItemFactory.isBeretta(weapon) ? (headshot ? 2.5 : 1.8) : (headshot ? 1.8 : 1.2));

        double oldHealth = target.getHealth();
        double newHealth = oldHealth - damage

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
        ItemStack sightItem = ItemFactory.isPx4(weaponItem) ? ItemFactory.createPx4SightItem() :
                              (ItemFactory.isBeretta(weaponItem) ? ItemFactory.createBerettaSightItem() : ItemFactory.createSightItem());
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
        return item != null && (ItemFactory.isGlock(item) || ItemFactory.isBeretta(item) || ItemFactory.isPx4(item));
    }

    private boolean isAnySightItem(ItemStack item) {
        return item != null && (ItemFactory.isSightItem(item) || ItemFactory.isBerettaSightItem(item) || ItemFactory.isPx4SightItem(item));
    }

    private boolean isAnyMagazine(ItemStack item) {
        return item != null && (ItemFactory.isCaricatoreGlock(item) || ItemFactory.isCaricatoreBeretta(item) || ItemFactory.isCaricatorePx4(item));
    }

    private int getMaxCapacity(ItemStack item) {
        if (ItemFactory.isPx4(item) || ItemFactory.isCaricatorePx4(item)) return ItemFactory.PX4_MAG_CAPACITY;
        if (ItemFactory.isBeretta(item) || ItemFactory.isCaricatoreBeretta(item)) return ItemFactory.BERETTA_MAG_CAPACITY;
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
        if (hasMag != 1 || pdc.getOrDefault(Keys.MAG_AMMO, PersistentDataType.INTEGER, 0) <= 0) {
            player.getWorld().playSound(player.getLocation(), "click", 1.0f, 1.0f);
            lastShot.put(player.getUniqueId(), now);
            return;
        }

        int ammo = pdc.get(Keys.MAG_AMMO, PersistentDataType.INTEGER) - 1;
        pdc.set(Keys.MAG_AMMO, PersistentDataType.INTEGER, ammo);
        updateWeaponLore(meta, true
    private void toggleMagazine(Player player, ItemStack weapon) {
        ItemMeta meta = weapon.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.getOrDefault(Keys.HAS_MAG, PersistentDataType.BYTE, (byte) 0) == 1) {
            int ammoLeft = pdc.getOrDefault(Keys.MAG_AMMO, PersistentDataType.INTEGER, 0);
            pdc.set(Keys.HAS_MAG, PersistentDataType.BYTE, (byte) 0);
            pdc.remove(Keys.MAG_AMMO);
            updateWeaponLore(meta, false, 0, getMaxCapacity(weapon));
            weapon.setItemMeta(meta);
            giveOrDrop(player, ItemFactory.isPx4(weapon) ? ItemFactory.createCaricatorePx4(ammoLeft) :
                              (ItemFactory.isBeretta(weapon) ? ItemFactory.createCaricatoreBeretta(ammoLeft) : ItemFactory.createCaricatoreGlock(ammoLeft)));
        } else {
            ItemStack off = player.getInventory().getItemInOffHand();
            if ((ItemFactory.isPx4(weapon) && !ItemFactory.isCaricatorePx4(off)) ||
                (ItemFactory.isBeretta(weapon) && !ItemFactory.isCaricatoreBeretta(off)) ||
                (ItemFactory.isGlock(weapon) && !ItemFactory.isCaricatoreGlock(off))) return;
            int ammo = off.getItemMeta().getPersistentDataContainer().getOrDefault(Keys.MAG_AMMO, PersistentDataType.INTEGER, 0);
            pdc.set(Keys.HAS_MAG, PersistentDataType.BYTE, (byte) 1);
            pdc.set(Keys.MAG_AMMO, PersistentDataType.INTEGER, ammo);
            updateWeaponLore(meta, true, ammo, getMaxCapacity(weapon));
            weapon.setItemMeta(meta);
            if (off.getAmount() <= 1) player.getInventory().setItemInOffHand(null);
            else off.setAmount(off.getAmount() - 1);
        }
    }

    private void toggleReload(Player player) {
        int slot = player.getInventory().getHeldItemSlot();
        ItemStack mag = player.getInventory().getItem(slot);
        if (!isAnyMagazine(mag)) return;
        int max = getMaxCapacity(mag);
        ItemMeta meta = mag.getItemMeta();
        int cur = meta.getPersistentDataContainer().getOrDefault(Keys.MAG_AMMO, PersistentDataType.INTEGER, 0);
        if (cur >= max) return;
        meta.getPersistentDataContainer().set(Keys.MAG_AMMO, PersistentDataType.INTEGER, cur + 1);
        mag.setItemMeta(meta);
    }

    private void stopReload(UUID id) {
        BukkitTask t = reloadTasks.remove(id);
        if (t != null) t.cancel();
    }

    private void updateWeaponLore(ItemMeta meta, boolean hasMag, int ammo, int max) {
        List<String> lore = new ArrayList<>();
        lore.add(hasMag ? "§7Caricatore: §b" + ammo + "/" + max : "§7Caricatore: §cNessuno");
        meta.setLore(lore);
    }

    private void giveOrDrop(Player player, ItemStack item) {
        if (player.getInventory().firstEmpty() == -1) player.getWorld().dropItemNaturally(player.getLocation(), item);
        else player.getInventory().addItem(item);
    }
}

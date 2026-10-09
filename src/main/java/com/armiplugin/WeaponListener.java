package com.armiplugin;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WeaponListener implements Listener {

    private static final long FIRE_COOLDOWN_MS = 250L;

    private final JavaPlugin plugin;
    private final double bodyDamage;
    private final double headDamage;

    private final Map<UUID, Long> lastShot = new HashMap<>();

    public WeaponListener(JavaPlugin plugin, double bodyDamage, double headDamage) {
        this.plugin = plugin;
        this.bodyDamage = bodyDamage;
        this.headDamage = headDamage;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        if (event.getPlayer().isSneaking()) {
            tryShoot(event.getPlayer());
        }
    }

    private void tryShoot(Player player) {
        ItemStack weapon = getActiveWeapon(player);
        if (weapon == null || !player.isSneaking()) return;

        long now = System.currentTimeMillis();
        long last = lastShot.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < FIRE_COOLDOWN_MS) {
            return;
        }

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
        weapon.setItemMeta(meta);
        saveActiveWeapon(player, weapon);

        lastShot.put(player.getUniqueId(), now);
        performShot(player, weapon);
    }

    private ItemStack getActiveWeapon(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (isWeapon(mainHand)) {
            return mainHand;
        }

        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (isWeapon(offHand)) {
            return offHand;
        }

        return null;
    }

    private void saveActiveWeapon(Player player, ItemStack weapon) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null && mainHand.isSimilar(weapon)) {
            player.getInventory().setItemInMainHand(weapon);
            return;
        }

        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand != null && offHand.isSimilar(weapon)) {
            player.getInventory().setItemInOffHand(weapon);
        }
    }

    private boolean isWeapon(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        String itemId = meta.getPersistentDataContainer().get(Keys.ITEM_ID, PersistentDataType.STRING);
        return "glock_weapon".equals(itemId)
                || "beretta_weapon".equals(itemId)
                || "px4_weapon".equals(itemId);
    }

    private void performShot(Player player, ItemStack weapon) {
        Location origin = player.getEyeLocation();
        Vector direction = player.getLocation().getDirection().normalize();

        player.getWorld().playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.2F);

        for (Entity entity : player.getWorld().getNearbyEntities(
                origin,
                18,
                18,
                18,
                e -> e instanceof LivingEntity && e != player && player.hasLineOfSight(e)
        )) {
            LivingEntity target = (LivingEntity) entity;

            double distance = target.getLocation().distanceSquared(origin);
            if (distance > 25) {
                continue;
            }

            Location targetHead = target.getEyeLocation();
            boolean headshot = targetHead.distanceSquared(origin) < target.getLocation().distanceSquared(origin);

            target.damage(headshot ? headDamage : bodyDamage, player);

            player.getWorld().spawnParticle(
                    Particle.CRIT,
                    target.getLocation().add(0, 1, 0),
                    12,
                    0.2,
                    0.5,
                    0.2,
                    0.05
            );

            return;
        }

        player.getWorld().spawnParticle(
                Particle.SMOKE_NORMAL,
                origin.clone().add(direction.clone().multiply(1.5)),
                12,
                0.1,
                0.1,
                0.1,
                0.02
        );
    }

    private void playEmptySound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 1.0F);
    }
}

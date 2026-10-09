package com.armiplugin;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ItemFactory {

    public static final int GLOCK_MAG_CAPACITY = 17;
    public static final int BERETTA_MAG_CAPACITY = 15;
    public static final int PX4_MAG_CAPACITY = 17;

    // --- GLOCK ---
    public static ItemStack createGlock() {
        ItemStack item = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§fGlock");
        meta.setCustomModelData(11);
        meta.getPersistentDataContainer().set(Keys.WEAPON_TYPE, PersistentDataType.STRING, "glock");
        meta.getPersistentDataContainer().set(Keys.HAS_MAG, PersistentDataType.BYTE, (byte) 0);
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Caricatore: §cNessuno");
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createCaricatoreGlock(int ammo) {
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§fCaricatore Glock");
        meta.setCustomModelData(394);
        meta.getPersistentDataContainer().set(Keys.MAG_TYPE, PersistentDataType.STRING, "glock");
        meta.getPersistentDataContainer().set(Keys.MAG_AMMO, PersistentDataType.INTEGER, ammo);

        List<String> lore = new ArrayList<>();
        lore.add("§7Colpi: §f" + ammo + "/" + GLOCK_MAG_CAPACITY);
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    // --- BERETTA 92FS ---
    public static ItemStack createBeretta92FS() {
        ItemStack item = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§fBeretta 92FS");
        meta.setCustomModelData(8);
        meta.getPersistentDataContainer().set(Keys.WEAPON_TYPE, PersistentDataType.STRING, "beretta");
        meta.getPersistentDataContainer().set(Keys.HAS_MAG, PersistentDataType.BYTE, (byte) 0);

        List<String> lore = new ArrayList<>();
        lore.add("§7Caricatore: §cNessuno");
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createCaricatoreBeretta(int ammo) {
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§fCaricatore 92FS");
        meta.setCustomModelData(394);
        meta.getPersistentDataContainer().set(Keys.MAG_TYPE, PersistentDataType.STRING, "beretta");
        meta.getPersistentDataContainer().set(Keys.MAG_AMMO, PersistentDataType.INTEGER, ammo);

        List<String> lore = new ArrayList<>();
        lore.add("§7Colpi: §f" + ammo + "/" + BERETTA_MAG_CAPACITY);
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    // --- BERETTA PX4 ---
    public static ItemStack createBerettaPx4() {
        ItemStack item = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§fBeretta PX4");
        meta.setCustomModelData(6);
        meta.getPersistentDataContainer().set(Keys.WEAPON_TYPE, PersistentDataType.STRING, "px4");
        meta.getPersistentDataContainer().set(Keys.HAS_MAG, PersistentDataType.BYTE, (byte) 0);

        List<String> lore = new ArrayList<>();
        lore.add("§7Caricatore: §cNessuno");
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createCaricatorePx4(int ammo) {
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§fCaricatore PX4");
        meta.setCustomModelData(394);
        meta.getPersistentDataContainer().set(Keys.MAG_TYPE, PersistentDataType.STRING, "px4");
        meta.getPersistentDataContainer().set(Keys.MAG_AMMO, PersistentDataType.INTEGER, ammo);

        List<String> lore = new ArrayList<>();
        lore.add("§7Colpi: §f" + ammo + "/" + PX4_MAG_CAPACITY);
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    // --- MUNIZIONI ---
    public static ItemStack createMunizioni9mm(int amount) {
        ItemStack item = new ItemStack(Material.STICK, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§fMunizioni 9mm");
        meta.setCustomModelData(91);
        item.setItemMeta(meta);
        return item;
    }

    // --- MIRINI (BALESTRA CMD 100) ---
    public static ItemStack createSightItem() {
        ItemStack item = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§fMirino Glock");
        meta.setCustomModelData(100);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createBerettaSightItem() {
        ItemStack item = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§fMirino Beretta 92FS");
        meta.setCustomModelData(100);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createPx4SightItem() {
        ItemStack item = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§fMirino Beretta PX4");
        meta.setCustomModelData(100);
        item.setItemMeta(meta);
        return item;
    }

    // --- CONTROLLI ---
    public static boolean isGlock(ItemStack item) {
        return checkType(item, Keys.WEAPON_TYPE, "glock");
    }

    public static boolean isBeretta(ItemStack item) {
        return checkType(item, Keys.WEAPON_TYPE, "beretta");
    }

    public static boolean isPx4(ItemStack item) {
        return checkType(item, Keys.WEAPON_TYPE, "px4");
    }

    public static boolean isCaricatoreGlock(ItemStack item) {
        return checkType(item, Keys.MAG_TYPE, "glock");
    }

    public static boolean isCaricatoreBeretta(ItemStack item) {
        return checkType(item, Keys.MAG_TYPE, "beretta");
    }

    public static boolean isCaricatorePx4(ItemStack item) {
        return checkType(item, Keys.MAG_TYPE, "px4");
    }

    public static boolean isMunizioni9mm(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getType() == Material.STICK && item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 91;
    }

    public static boolean isSightItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getType() == Material.CROSSBOW && item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 100;
    }

    public static boolean isBerettaSightItem(ItemStack item) {
        return isSightItem(item);
    }

    public static boolean isPx4SightItem(ItemStack item) {
        return isSightItem(item);
    }

    private static boolean checkType(ItemStack item, org.bukkit.NamespacedKey key, String expected) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        String val = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return expected.equals(val);
    }
}

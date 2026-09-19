package com.armiplugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ItemFactory {

    public static final int GLOCK_MAG_CAPACITY = 17;
    public static final int BERETTA_MAG_CAPACITY = 15;

    // --- GLOCK ---
    public static ItemStack createGlock() {
        ItemStack item = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Glock 17");
            meta.setCustomModelData(7);
            meta.getPersistentDataContainer().set(Keys.ITEM_ID, PersistentDataType.STRING, "glock_weapon");
            meta.getPersistentDataContainer().set(Keys.HAS_MAG, PersistentDataType.BYTE, (byte) 0);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Caricatore: " + ChatColor.RED + "Nessuno");
            lore.add(ChatColor.DARK_GRAY + "Tasto sinistro: spara (solo accovacciato)");
            lore.add(ChatColor.DARK_GRAY + "Tasto destro: inserisci/espelli caricatore");
            meta.setLore(lore);

            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createCaricatoreGlock(int ammo) {
        ItemStack item = new ItemStack(Material.BRICK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "Caricatore Glock");
            meta.setCustomModelData(393);
            meta.getPersistentDataContainer().set(Keys.ITEM_ID, PersistentDataType.STRING, "glock_magazine");
            meta.getPersistentDataContainer().set(Keys.MAG_AMMO, PersistentDataType.INTEGER, ammo);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Colpi: " + ChatColor.WHITE + ammo + "/" + GLOCK_MAG_CAPACITY);
            lore.add(ChatColor.DARK_GRAY + "Tasto destro: ricarica dai 9mm nell'inventario");
            meta.setLore(lore);

            item.setItemMeta(meta);
        }
        return item;
    }

    // --- BERETTA 92FS ---
    public static ItemStack createBeretta92FS() {
        ItemStack item = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Beretta 92FS");
            meta.setCustomModelData(8);
            meta.getPersistentDataContainer().set(Keys.ITEM_ID, PersistentDataType.STRING, "beretta_weapon");
            meta.getPersistentDataContainer().set(Keys.HAS_MAG, PersistentDataType.BYTE, (byte) 0);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Caricatore: " + ChatColor.RED + "Nessuno");
            lore.add(ChatColor.DARK_GRAY + "Tasto sinistro: spara (solo accovacciato)");
            lore.add(ChatColor.DARK_GRAY + "Tasto destro: inserisci/espelli caricatore");
            meta.setLore(lore);

            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createCaricatoreBeretta(int ammo) {
        ItemStack item = new ItemStack(Material.BRICK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "Caricatore 92FS");
            meta.setCustomModelData(394); // Cambiati qui i CustomModelData a 394
            meta.getPersistentDataContainer().set(Keys.ITEM_ID, PersistentDataType.STRING, "beretta_magazine");
            meta.getPersistentDataContainer().set(Keys.MAG_AMMO, PersistentDataType.INTEGER, ammo);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Colpi: " + ChatColor.WHITE + ammo + "/" + BERETTA_MAG_CAPACITY);
            lore.add(ChatColor.DARK_GRAY + "Tasto destro: ricarica dai 9mm nell'inventario");
            meta.setLore(lore);

            item.setItemMeta(meta);
        }
        return item;
    }

    // --- MUNIZIONI ---
    public static ItemStack createMunizioni9mm(int amount) {
        ItemStack item = new ItemStack(Material.FLINT, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + "Munizioni 9mm");
            meta.setCustomModelData(101);
            meta.getPersistentDataContainer().set(Keys.ITEM_ID, PersistentDataType.STRING, "9mm_ammo");
            item.setItemMeta(meta);
        }
        return item;
    }

    // --- MIRINI (ADS) ---
    public static ItemStack createSightItem() {
        ItemStack item = new ItemStack(Material.WARPED_FUNGUS_ON_A_STICK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Glock 17 (Mirino)");
            meta.setCustomModelData(7);
            meta.getPersistentDataContainer().set(Keys.ITEM_ID, PersistentDataType.STRING, "glock_sight");
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createBerettaSightItem() {
        ItemStack item = new ItemStack(Material.WARPED_FUNGUS_ON_A_STICK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Beretta 92FS (Mirino)");
            meta.setCustomModelData(8);
            meta.getPersistentDataContainer().set(Keys.ITEM_ID, PersistentDataType.STRING, "beretta_sight");
            item.setItemMeta(meta);
        }
        return item;
    }

    // --- CONTROLLI E VERIFICHE ---
    public static boolean isGlock(ItemStack item) {
        return isMatch(item, "glock_weapon");
    }

    public static boolean isBeretta(ItemStack item) {
        return isMatch(item, "beretta_weapon");
    }

    public static boolean isCaricatoreGlock(ItemStack item) {
        return isMatch(item, "glock_magazine");
    }

    public static boolean isCaricatoreBeretta(ItemStack item) {
        return isMatch(item, "beretta_magazine");
    }

    public static boolean isMunizioni9mm(ItemStack item) {
        return isMatch(item, "9mm_ammo");
    }

    public static boolean isSightItem(ItemStack item) {
        return isMatch(item, "glock_sight");
    }

    public static boolean isBerettaSightItem(ItemStack item) {
        return isMatch(item, "beretta_sight");
    }

    private static boolean isMatch(ItemStack item, String id) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        String val = meta.getPersistentDataContainer().get(Keys.ITEM_ID, PersistentDataType.STRING);
        return id.equals(val);
    }
}

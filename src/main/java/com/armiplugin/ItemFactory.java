package com.armiplugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ItemFactory {

    // Capacità massima dei caricatori
    public static final int GLOCK_MAG_CAPACITY = 17;
    public static final int BERETTA_MAG_CAPACITY = 15;

    // ID Glock
    public static final String ID_GLOCK_WEAPON = "glock_weapon";
    public static final String ID_GLOCK_MAGAZINE = "glock_magazine";
    public static final String ID_9MM_AMMO = "9mm_ammo";
    public static final String ID_GLOCK_SIGHT = "glock_sight";

    // ID Beretta 92FS
    public static final String ID_BERETTA_WEAPON = "beretta_weapon";
    public static final String ID_BERETTA_MAGAZINE = "beretta_magazine";
    public static final String ID_BERETTA_SIGHT = "beretta_sight";

    // ==========================================
    // -------------- METODI GLOCK --------------
    // ==========================================

    public static ItemStack createGlock() {
        ItemStack item = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "Glock");
        meta.setCustomModelData(11);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Caricatore: " + ChatColor.RED + "Nessuno");
        lore.add(ChatColor.DARK_GRAY + "Tasto sinistro: spara (solo accovacciato)");
        lore.add(ChatColor.DARK_GRAY + "Tasto destro: inserisci/espelli caricatore");
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(Keys.ITEM_ID, PersistentDataType.STRING, ID_GLOCK_WEAPON);
        meta.getPersistentDataContainer().set(Keys.HAS_MAG, PersistentDataType.BYTE, (byte) 0);

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createCaricatoreGlock(int ammo) {
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.AQUA + "Caricatore Glock");
        meta.setCustomModelData(394);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Colpi: " + ChatColor.WHITE + ammo + "/" + GLOCK_MAG_CAPACITY);
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(Keys.ITEM_ID, PersistentDataType.STRING, ID_GLOCK_MAGAZINE);
        meta.getPersistentDataContainer().set(Keys.MAG_AMMO, PersistentDataType.INTEGER, ammo);

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createMunizioni9mm(int amount) {
        ItemStack item = new ItemStack(Material.STICK, amount);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.YELLOW + "9mm");
        meta.setCustomModelData(91);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Proiettili per Caricatore");
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(Keys.ITEM_ID, PersistentDataType.STRING, ID_9MM_AMMO);

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createSightItem() {
        ItemStack item = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.DARK_GRAY + "In mira...");
        meta.setCustomModelData(100);

        meta.getPersistentDataContainer().set(Keys.ITEM_ID, PersistentDataType.STRING, ID_GLOCK_SIGHT);

        item.setItemMeta(meta);
        return item;
    }

    // ==========================================
    // ----------- METODI BERETTA 92FS ----------
    // ==========================================

    /**
     * Crea la Beretta 92FS (balestra con CustomModelData 8).
     */
    public static ItemStack createBeretta92FS() {
        ItemStack item = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "Beretta 92FS");
        meta.setCustomModelData(8);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Caricatore: " + ChatColor.RED + "Nessuno");
        lore.add(ChatColor.DARK_GRAY + "Tasto sinistro: spara (solo accovacciato)");
        lore.add(ChatColor.DARK_GRAY + "Tasto destro: inserisci/espelli caricatore");
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(Keys.ITEM_ID, PersistentDataType.STRING, ID_BERETTA_WEAPON);
        meta.getPersistentDataContainer().set(Keys.HAS_MAG, PersistentDataType.BYTE, (byte) 0);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Crea il Caricatore 92FS (stick con CustomModelData 395).
     */
    public static ItemStack createCaricatoreBeretta(int ammo) {
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.AQUA + "Caricatore 92FS");
        meta.setCustomModelData(395);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Colpi: " + ChatColor.WHITE + ammo + "/" + BERETTA_MAG_CAPACITY);
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(Keys.ITEM_ID, PersistentDataType.STRING, ID_BERETTA_MAGAZINE);
        meta.getPersistentDataContainer().set(Keys.MAG_AMMO, PersistentDataType.INTEGER, ammo);

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createBerettaSightItem() {
        ItemStack item = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.DARK_GRAY + "In mira...");
        meta.setCustomModelData(102);

        meta.getPersistentDataContainer().set(Keys.ITEM_ID, PersistentDataType.STRING, ID_BERETTA_SIGHT);

        item.setItemMeta(meta);
        return item;
    }

    // ==========================================
    // ------- METODI UTILITÀ DI CONTROLLO ------
    // ==========================================

    public static boolean isGlock(ItemStack item) {
        return hasId(item, ID_GLOCK_WEAPON);
    }

    public static boolean isCaricatoreGlock(ItemStack item) {
        return hasId(item, ID_GLOCK_MAGAZINE);
    }

    public static boolean isMunizioni9mm(ItemStack item) {
        return hasId(item, ID_9MM_AMMO);
    }

    public static boolean isSightItem(ItemStack item) {
        return hasId(item, ID_GLOCK_SIGHT);
    }

    public static boolean isBeretta(ItemStack item) {
        return hasId(item, ID_BERETTA_WEAPON);
    }

    public static boolean isCaricatoreBeretta(ItemStack item) {
        return hasId(item, ID_BERETTA_MAGAZINE);
    }

    public static boolean isBerettaSightItem(ItemStack item) {
        return hasId(item, ID_BERETTA_SIGHT);
    }

    private static boolean hasId(ItemStack item, String id) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        String stored = meta.getPersistentDataContainer().get(Keys.ITEM_ID, PersistentDataType.STRING);
        return id.equals(stored);
    }
}

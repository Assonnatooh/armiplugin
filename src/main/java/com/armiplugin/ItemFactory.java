package com.armiplugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ItemFactory {

    // Capacità massima del caricatore della Glock.
    public static final int GLOCK_MAG_CAPACITY = 17;

    public static final String ID_GLOCK_WEAPON = "glock_weapon";
    public static final String ID_GLOCK_MAGAZINE = "glock_magazine";
    public static final String ID_9MM_AMMO = "9mm_ammo";
    public static final String ID_GLOCK_SIGHT = "glock_sight";

    /**
     * Crea la Glock (balestra con CustomModelData 11), senza caricatore inserito.
     */
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

    /**
     * Crea un Caricatore Glock (stick con CustomModelData 394) con una certa quantità di colpi.
     */
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

    /**
     * Crea i proiettili 9mm (stick con CustomModelData 91), usati per riempire i caricatori.
     */
    public static ItemStack createMunizioni9mm(int amount) {
        ItemStack item = new ItemStack(Material.STICK, amount);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.YELLOW + "9mm");
        meta.setCustomModelData(91);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Proiettili per Caricatore Glock");
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(Keys.ITEM_ID, PersistentDataType.STRING, ID_9MM_AMMO);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Item "mirino": balestra con CustomModelData 100, mostrato nella mano principale
     * al posto della Glock quando il giocatore è in mira (accovacciato).
     * Puramente visivo: non spara, non è ottenibile dalla GUI.
     */
    public static ItemStack createSightItem() {
        ItemStack item = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.DARK_GRAY + "In mira...");
        meta.setCustomModelData(100);

        meta.getPersistentDataContainer().set(Keys.ITEM_ID, PersistentDataType.STRING, ID_GLOCK_SIGHT);

        item.setItemMeta(meta);
        return item;
    }

    // ---- Metodi di utilità per riconoscere gli item ----

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

    private static boolean hasId(ItemStack item, String id) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        String stored = meta.getPersistentDataContainer().get(Keys.ITEM_ID, PersistentDataType.STRING);
        return id.equals(stored);
    }
}

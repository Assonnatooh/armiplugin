package com.armiplugin;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Contiene tutte le NamespacedKey usate per salvare dati custom
 * dentro il PersistentDataContainer degli ItemStack.
 */
public class Keys {

    // Identifica il TIPO di item custom: "glock_weapon", "glock_magazine", "9mm_ammo"
    public static NamespacedKey ITEM_ID;

    // Sull'arma: 1 = ha un caricatore inserito, 0 = no
    public static NamespacedKey HAS_MAG;

    // Sull'arma (quando ha un caricatore inserito) e sui caricatori: quanti colpi restano
    public static NamespacedKey MAG_AMMO;

    public static void init(JavaPlugin plugin) {
        ITEM_ID = new NamespacedKey(plugin, "item_id");
        HAS_MAG = new NamespacedKey(plugin, "has_mag");
        MAG_AMMO = new NamespacedKey(plugin, "mag_ammo");
    }
}

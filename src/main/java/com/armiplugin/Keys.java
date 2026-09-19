package com.armiplugin;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class Keys {

    public static NamespacedKey ITEM_ID;
    public static NamespacedKey HAS_MAG;
    public static NamespacedKey MAG_AMMO;

    public static void init(JavaPlugin plugin) {
        ITEM_ID = new NamespacedKey(plugin, "item_id");
        HAS_MAG = new NamespacedKey(plugin, "has_mag");
        MAG_AMMO = new NamespacedKey(plugin, "mag_ammo");
    }
}

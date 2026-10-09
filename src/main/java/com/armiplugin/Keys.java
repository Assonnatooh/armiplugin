package com.armiplugin;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class Keys {
    public static NamespacedKey WEAPON_TYPE;
    public static NamespacedKey MAG_TYPE;
    public static NamespacedKey HAS_MAG;
    public static NamespacedKey MAG_AMMO;
    public static NamespacedKey ITEM_ID;

    public static void init(JavaPlugin plugin) {
        WEAPON_TYPE = new NamespacedKey(plugin, "weapon_type");
        MAG_TYPE = new NamespacedKey(plugin, "mag_type");
        HAS_MAG = new NamespacedKey(plugin, "has_mag");
        MAG_AMMO = new NamespacedKey(plugin, "mag_ammo");
        ITEM_ID = new NamespacedKey(plugin, "item_id");
    }
}

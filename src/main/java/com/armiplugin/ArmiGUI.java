package com.armiplugin;

import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ArmiGUI {

    // Titolo usato anche per riconoscere l'inventario nel listener dei click
    public static final String TITLE = ChatColor.DARK_GRAY + "Armeria";

    // Slot fissi dentro la GUI
    public static final int SLOT_GLOCK = 10;
    public static final int SLOT_CARICATORE = 12;
    public static final int SLOT_MUNIZIONI = 14;

    public static void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, TITLE);

        gui.setItem(SLOT_GLOCK, ItemFactory.createGlock());
        gui.setItem(SLOT_CARICATORE, ItemFactory.createCaricatoreGlock(0));
        gui.setItem(SLOT_MUNIZIONI, ItemFactory.createMunizioni9mm(30));

        player.openInventory(gui);
    }
}

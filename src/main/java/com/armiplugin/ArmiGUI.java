package com.armiplugin;

import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class ArmiGUI {

    // Titolo usato anche per riconoscere l'inventario nel listener dei click
    public static final String TITLE = ChatColor.DARK_GRAY + "Armeria";

    // Slot Glock (Prima riga)
    public static final int SLOT_GLOCK = 10;
    public static final int SLOT_CARICATORE = 12;
    public static final int SLOT_MUNIZIONI = 14;

    // Slot Beretta 92FS (Seconda riga)
    public static final int SLOT_BERETTA = 19;
    public static final int SLOT_CARICATORE_BERETTA = 21;

    public static void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, TITLE);

        // Glock
        gui.setItem(SLOT_GLOCK, ItemFactory.createGlock());
        gui.setItem(SLOT_CARICATORE, ItemFactory.createCaricatoreGlock(0));
        gui.setItem(SLOT_MUNIZIONI, ItemFactory.createMunizioni9mm(64));

        // Beretta 92FS
        gui.setItem(SLOT_BERETTA, ItemFactory.createBeretta92FS());
        gui.setItem(SLOT_CARICATORE_BERETTA, ItemFactory.createCaricatoreBeretta(0));

        player.openInventory(gui);
    }
}

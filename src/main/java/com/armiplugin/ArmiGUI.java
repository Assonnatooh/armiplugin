package com.armiplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class ArmiGUI {

    public static final String TITLE = ChatColor.DARK_GRAY + "Armeria";

    public static final int SLOT_GLOCK = 10;
    public static final int SLOT_CARICATORE = 12;
    public static final int SLOT_MUNIZIONI = 14;

    public static final int SLOT_BERETTA = 19;
    public static final int SLOT_CARICATORE_BERETTA = 21;

    public static final int SLOT_PX4 = 28;
    public static final int SLOT_CARICATORE_PX4 = 30;

    public static void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 45, TITLE);

        gui.setItem(SLOT_GLOCK, ItemFactory.createGlock());
        gui.setItem(SLOT_CARICATORE, ItemFactory.createCaricatoreGlock(0));
        gui.setItem(SLOT_MUNIZIONI, ItemFactory.createMunizioni9mm(64));

        gui.setItem(SLOT_BERETTA, ItemFactory.createBeretta92FS());
        gui.setItem(SLOT_CARICATORE_BERETTA, ItemFactory.createCaricatoreBeretta(0));

        gui.setItem(SLOT_PX4, ItemFactory.createBerettaPx4());
        gui.setItem(SLOT_CARICATORE_PX4, ItemFactory.createCaricatorePx4(0));

        player.openInventory(gui);
    }
}

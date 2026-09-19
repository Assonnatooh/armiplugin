package com.armiplugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ArmiGUI.TITLE)) {
            event.setCancelled(true); // Impedisce di prendere l'item direttamente dalla GUI e scompaginarla

            if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            int slot = event.getSlot();

            switch (slot) {
                // GLOCK (Prima Riga)
                case ArmiGUI.SLOT_GLOCK:
                    giveOrDrop(player, ItemFactory.createGlock());
                    player.sendMessage("§aHai ricevuto una Glock 17!");
                    break;
                case ArmiGUI.SLOT_CARICATORE:
                    giveOrDrop(player, ItemFactory.createCaricatoreGlock(0));
                    player.sendMessage("§aHai ricevuto un Caricatore Glock!");
                    break;
                case ArmiGUI.SLOT_MUNIZIONI:
                    giveOrDrop(player, ItemFactory.createMunizioni9mm(64));
                    player.sendMessage("§aHai ricevuto 64 Munizioni 9mm!");
                    break;

                // BERETTA 92FS (Seconda Riga)
                case ArmiGUI.SLOT_BERETTA:
                    giveOrDrop(player, ItemFactory.createBeretta92FS());
                    player.sendMessage("§aHai ricevuto una Beretta 92FS!");
                    break;
                case ArmiGUI.SLOT_CARICATORE_BERETTA:
                    giveOrDrop(player, ItemFactory.createCaricatoreBeretta(0));
                    player.sendMessage("§aHai ricevuto un Caricatore 92FS!");
                    break;

                // BERETTA PX4 (Terza Riga)
                case ArmiGUI.SLOT_PX4:
                    giveOrDrop(player, ItemFactory.createBerettaPx4());
                    player.sendMessage("§aHai ricevuto una Beretta PX4!");
                    break;
                case ArmiGUI.SLOT_CARICATORE_PX4:
                    giveOrDrop(player, ItemFactory.createCaricatorePx4(0));
                    player.sendMessage("§aHai ricevuto un Caricatore PX4!");
                    break;

                default:
                    break;
            }
        }
    }

    private void giveOrDrop(Player player, ItemStack item) {
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        } else {
            player.getInventory().addItem(item);
        }
    }
}

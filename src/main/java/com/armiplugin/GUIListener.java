package com.armiplugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTitle() == null) return;
        if (!event.getView().getTitle().equals(ArmiGUI.TITLE)) return;

        // Blocca sempre il click per evitare che il giocatore prenda/sposti gli item della GUI
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        ItemStack toGive = null;

        if (slot == ArmiGUI.SLOT_GLOCK) {
            toGive = ItemFactory.createGlock();
        } else if (slot == ArmiGUI.SLOT_CARICATORE) {
            toGive = ItemFactory.createCaricatoreGlock(0);
        } else if (slot == ArmiGUI.SLOT_MUNIZIONI) {
            toGive = ItemFactory.createMunizioni9mm(30);
        } else {
            return; // click fuori dagli slot validi, ignora
        }

        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage("§cInventario pieno!");
            return;
        }

        player.getInventory().addItem(toGive);
        player.closeInventory();
    }
}

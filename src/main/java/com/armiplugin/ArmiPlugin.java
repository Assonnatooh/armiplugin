package com.armiplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ArmiPlugin extends JavaPlugin {

    // Danno al corpo e alla testa: come richiesto, 1.0 corpo / 1.5 testa
    private static final double BODY_DAMAGE = 1.0;
    private static final double HEAD_DAMAGE = 1.5;

    @Override
    public void onEnable() {
        Keys.init(this);

        getServer().getPluginManager().registerEvents(new GUIListener(), this);
        getServer().getPluginManager().registerEvents(new WeaponListener(this, BODY_DAMAGE, HEAD_DAMAGE), this);

        getLogger().info("ArmiPlugin abilitato! Danni: corpo=" + BODY_DAMAGE + " testa=" + HEAD_DAMAGE);
    }

    @Override
    public void onDisable() {
        getLogger().info("ArmiPlugin disabilitato.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("armi")) return false;

        if (!(sender instanceof Player)) {
            sender.sendMessage("Questo comando può essere usato solo in gioco.");
            return true;
        }

        Player player = (Player) sender;
        ArmiGUI.open(player);
        return true;
    }
}

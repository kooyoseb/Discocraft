package org.kooyoseb.discocraft;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.kooyoseb.discocraft.command.DiscocraftCommand;
import org.kooyoseb.discocraft.command.DiscordLinkCommand;
import org.kooyoseb.discocraft.config.DiscocraftConfig;
import org.kooyoseb.discocraft.discord.DiscordBridge;
import org.kooyoseb.discocraft.language.Messages;
import org.kooyoseb.discocraft.listener.MinecraftBridgeListener;
import org.kooyoseb.discocraft.link.LinkManager;

public final class Discocraft extends JavaPlugin {
    private DiscocraftConfig discocraftConfig;
    private DiscordBridge discordBridge;
    private LinkManager linkManager;
    private Messages messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        messages = new Messages(this);
        linkManager = new LinkManager(this);
        loadBridge();

        getServer().getPluginManager().registerEvents(new MinecraftBridgeListener(this), this);

        PluginCommand command = getCommand("discocraft");
        if (command != null) {
            DiscocraftCommand executor = new DiscocraftCommand(this, discordBridge);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        PluginCommand discordCommand = getCommand("discord");
        if (discordCommand != null) {
            DiscordLinkCommand executor = new DiscordLinkCommand(this);
            discordCommand.setExecutor(executor);
            discordCommand.setTabCompleter(executor);
        }
    }

    @Override
    public void onDisable() {
        if (discordBridge != null) {
            discordBridge.sendServerStop();
            discordBridge.shutdown();
        }
    }

    public void reloadBridge() {
        reloadConfig();
        messages.reload();
        if (discordBridge != null) {
            discordBridge.shutdown();
        }
        loadBridge();
    }

    public DiscocraftConfig discocraftConfig() {
        return discocraftConfig;
    }

    public DiscordBridge discordBridge() {
        return discordBridge;
    }

    public LinkManager linkManager() {
        return linkManager;
    }

    public Messages messages() {
        return messages;
    }

    private void loadBridge() {
        discocraftConfig = DiscocraftConfig.from(getConfig());
        discordBridge = new DiscordBridge(this, discocraftConfig);
        discordBridge.start();
    }
}

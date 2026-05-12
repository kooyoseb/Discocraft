package org.kooyoseb.discocraft.discord;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.kooyoseb.discocraft.Discocraft;

public final class DiscordMessageListener extends ListenerAdapter {
    private final Discocraft plugin;
    private final DiscordBridge bridge;

    public DiscordMessageListener(Discocraft plugin, DiscordBridge bridge) {
        this.plugin = plugin;
        this.bridge = bridge;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || !event.isFromGuild()) {
            return;
        }
        if (!bridge.shouldProcessIncoming(event.getJDA(), event.getChannel().getId())) {
            return;
        }

        String rawMessage = event.getMessage().getContentDisplay().trim();
        if (rawMessage.isBlank()) {
            return;
        }

        if (bridge.config().discordCommands() && rawMessage.startsWith("!")) {
            handleDiscordCommand(event, rawMessage);
            return;
        }

        bridge.broadcastDiscordMessage(event.getMember() != null
                ? event.getMember().getEffectiveName()
                : event.getAuthor().getName(), rawMessage);
    }

    private void handleDiscordCommand(MessageReceivedEvent event, String rawMessage) {
        String command = rawMessage.toLowerCase();
        if (command.equals("!status")) {
            event.getMessage().reply("Server is online. " + bridge.onlinePlayersLine()).queue();
            return;
        }
        if (command.equals("!players")) {
            event.getMessage().reply(bridge.onlinePlayersLine()).queue();
            return;
        }
        if (command.startsWith("!link ")) {
            String code = rawMessage.substring("!link ".length()).trim();
            if (code.isBlank()) {
                event.getMessage().reply("Usage: `!link <code>`").queue();
                return;
            }

            plugin.linkManager().completeLink(
                    code,
                    event.getAuthor().getId(),
                    event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getName()
            ).ifPresentOrElse(
                    playerName -> event.getMessage().reply("Linked Discord account to Minecraft player `" + playerName + "`.").queue(),
                    () -> event.getMessage().reply("Invalid or expired link code. Run `/discord link` in Minecraft again.").queue()
            );
            return;
        }
        if (command.equals("!unlink")) {
            plugin.linkManager().unlinkByDiscordId(event.getAuthor().getId()).ifPresentOrElse(
                    playerName -> event.getMessage().reply("Unlinked from Minecraft player `" + playerName + "`.").queue(),
                    () -> event.getMessage().reply("Your Discord account is not linked.").queue()
            );
            return;
        }
        if (command.startsWith("!say ")) {
            String message = rawMessage.substring("!say ".length()).trim();
            if (!message.isBlank()) {
                bridge.broadcastDiscordMessage(event.getAuthor().getName(), message);
                event.getMessage().addReaction(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("\u2705")).queue();
            }
            return;
        }
        if (command.startsWith(bridge.config().discordConsoleCommandPrefix().toLowerCase() + " ")) {
            handleConsoleCommand(event, rawMessage);
            return;
        }

        plugin.getLogger().fine("Ignored unknown Discord command: " + rawMessage);
    }

    private void handleConsoleCommand(MessageReceivedEvent event, String rawMessage) {
        if (!bridge.config().discordConsoleEnabled()) {
            event.getMessage().reply("Discord console commands are disabled.").queue();
            return;
        }
        if (!hasConsoleRole(event)) {
            event.getMessage().reply("You do not have permission to run console commands.").queue();
            return;
        }

        String prefix = bridge.config().discordConsoleCommandPrefix();
        String minecraftCommand = rawMessage.substring(prefix.length()).trim();
        if (minecraftCommand.startsWith("/")) {
            minecraftCommand = minecraftCommand.substring(1).trim();
        }

        if (!bridge.config().isConsoleCommandAllowed(minecraftCommand)) {
            event.getMessage().reply("That command is not allowed by `discord.console.allowed-prefixes`.").queue();
            return;
        }

        String commandToRun = minecraftCommand;
        Bukkit.getScheduler().runTask(plugin, () -> {
            boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandToRun);
            event.getMessage().reply(success
                    ? "Executed console command: `" + commandToRun + "`"
                    : "Command was not accepted by the server: `" + commandToRun + "`").queue();
        });
    }

    private boolean hasConsoleRole(MessageReceivedEvent event) {
        if (bridge.config().discordConsoleAllowedRoleIds().isEmpty()) {
            return true;
        }
        if (event.getMember() == null) {
            return false;
        }
        return event.getMember().getRoles().stream()
                .anyMatch(role -> bridge.config().discordConsoleAllowedRoleIds().contains(role.getId()));
    }
}

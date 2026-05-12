package org.kooyoseb.discocraft.link;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.kooyoseb.discocraft.Discocraft;

public final class LinkManager {
    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Discocraft plugin;
    private final File file;
    private final FileConfiguration data;
    private final Map<String, PendingLink> pendingLinks = new ConcurrentHashMap<>();

    public LinkManager(Discocraft plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "linked-accounts.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public String createCode(Player player) {
        removeExpiredCodes();
        pendingLinks.entrySet().removeIf(entry -> entry.getValue().playerId().equals(player.getUniqueId()));

        String code;
        do {
            code = randomCode();
        } while (pendingLinks.containsKey(code));

        pendingLinks.put(code, new PendingLink(player.getUniqueId(), player.getName(), Instant.now().plus(CODE_TTL)));
        return code;
    }

    public Optional<String> completeLink(String rawCode, String discordId, String discordName) {
        removeExpiredCodes();

        String code = rawCode.toUpperCase(Locale.ROOT);
        PendingLink pendingLink = pendingLinks.remove(code);
        if (pendingLink == null || pendingLink.expiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }

        unlinkByMinecraftId(pendingLink.playerId());
        unlinkByDiscordId(discordId);

        String uuid = pendingLink.playerId().toString();
        data.set("accounts." + uuid + ".player-name", pendingLink.playerName());
        data.set("accounts." + uuid + ".discord-id", discordId);
        data.set("accounts." + uuid + ".discord-name", discordName);
        data.set("discord-index." + discordId, uuid);
        save();

        return Optional.of(pendingLink.playerName());
    }

    public Optional<String> unlinkByMinecraftId(UUID playerId) {
        String uuid = playerId.toString();
        String playerName = data.getString("accounts." + uuid + ".player-name");
        String discordId = data.getString("accounts." + uuid + ".discord-id");
        if (discordId == null) {
            return Optional.empty();
        }

        data.set("accounts." + uuid, null);
        data.set("discord-index." + discordId, null);
        save();
        return Optional.ofNullable(playerName);
    }

    public Optional<String> unlinkByDiscordId(String discordId) {
        String uuid = data.getString("discord-index." + discordId);
        if (uuid == null) {
            return Optional.empty();
        }

        String playerName = data.getString("accounts." + uuid + ".player-name");
        data.set("accounts." + uuid, null);
        data.set("discord-index." + discordId, null);
        save();
        return Optional.ofNullable(playerName);
    }

    public boolean isLinked(UUID playerId) {
        return data.isString("accounts." + playerId + ".discord-id");
    }

    public Optional<String> linkedDiscordName(UUID playerId) {
        return Optional.ofNullable(data.getString("accounts." + playerId + ".discord-name"));
    }

    private void removeExpiredCodes() {
        Instant now = Instant.now();
        pendingLinks.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save linked-accounts.yml: " + exception.getMessage());
        }
    }

    private static String randomCode() {
        StringBuilder code = new StringBuilder(6);
        for (int index = 0; index < 6; index++) {
            code.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return code.toString();
    }

    private record PendingLink(UUID playerId, String playerName, Instant expiresAt) {
    }
}

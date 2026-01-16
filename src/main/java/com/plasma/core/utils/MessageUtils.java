package com.plasma.core.utils;

import com.plasma.core.PlasmaCore;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class MessageUtils {

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static void send(Player player, String message) {
        String prefix = PlasmaCore.getInstance().getConfig().getString("messages.prefix", "");
        player.sendMessage(color(prefix + message));
    }

    public static void sendRaw(Player player, String message) {
        player.sendMessage(color(message));
    }

    public static String getMessage(String key) {
        return PlasmaCore.getInstance().getConfig().getString("messages." + key, "&cСообщение не найдено: " + key);
    }

    public static void sendMessage(Player player, String key) {
        send(player, getMessage(key));
    }

    public static void sendMessage(Player player, String key, String... placeholders) {
        String message = getMessage(key);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
        }
        send(player, message);
    }
}

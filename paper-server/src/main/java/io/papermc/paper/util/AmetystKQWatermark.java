package io.papermc.paper.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

public final class AmetystKQWatermark {

    private static final TextColor COLOR_PRIMARY = TextColor.color(0xc77dff);
    private static final TextColor COLOR_SECONDARY = TextColor.color(0x9d4edd);
    private static final TextColor COLOR_ACCENT = TextColor.color(0xe0aaff);
    private static final TextColor COLOR_GRAY = NamedTextColor.GRAY;
    private static final TextColor COLOR_DARK = NamedTextColor.DARK_GRAY;

    private static final String[] BANNER = {
        "▄▄                                                                 ▄▄   ▄▄▄    ▄▄▄▄   ",
        "   ████                          ██                            ██      ██  ██▀    ██▀▀██  ",
        "   ████    ████▄██▄   ▄████▄   ███████   ▀██  ███  ▄▄█████▄  ███████   ██▄██     ██    ██ ",
        "  ██  ██   ██ ██ ██  ██▄▄▄▄██    ██       ██▄ ██   ██▄▄▄▄ ▀    ██      █████     ██    ██ ",
        "  ██████   ██ ██ ██  ██▀▀▀▀▀▀    ██        ████▀    ▀▀▀▀██▄    ██      ██  ██▄   ██    ██ ",
        " ▄██  ██▄  ██ ██ ██  ▀██▄▄▄▄█    ██▄▄▄      ███    █▄▄▄▄▄██    ██▄▄▄   ██   ██▄   ██▄▄██▀ ",
        " ▀▀    ▀▀  ▀▀ ▀▀ ▀▀    ▀▀▀▀▀      ▀▀▀▀      ██      ▀▀▀▀▀▀      ▀▀▀▀   ▀▀    ▀▀    ▀▀▀██  ",
        "                                          ███                                          ▀  ",
    };

    private static final String VERSION_STRING = "AmetystKQ 1.21.1";

    public static void print() {
        String ansiPrimary = "\u001B[38;2;199;125;255m";
        String ansiSecondary = "\u001B[38;2;157;78;221m";
        String ansiAccent = "\u001B[38;2;224;170;255m";
        String ansiReset = "\u001B[0m";

        for (int i = 0; i < BANNER.length; i++) {
            String color = switch (i) {
                case 0 -> ansiPrimary;
                case 1 -> ansiSecondary;
                case 2 -> ansiPrimary;
                case 3 -> ansiSecondary;
                case 4 -> ansiPrimary;
                case 5 -> ansiSecondary;
                case 6 -> ansiPrimary;
                case 7 -> ansiAccent;
                default -> ansiReset;
            };
            System.out.println(color + BANNER[i] + ansiReset);
        }
        System.out.println(ansiAccent + VERSION_STRING + ansiReset);
        System.out.println(ansiAccent + "\u2726 High-Performance Paper Fork \u26A1" + ansiReset);
        System.out.println(ansiPrimary + "  FAQ: kq.ametystmc.net/faq  |  Discord: kq.ametystmc.net/discord" + ansiReset);
        System.out.println();
    }

    public static Component getBannerComponent() {
        Component banner = Component.empty();
        for (String line : BANNER) {
            TextColor color;
            if (banner.equals(Component.empty())) {
                color = COLOR_PRIMARY;
            } else {
                color = COLOR_SECONDARY;
            }
            banner = banner.append(Component.text(line + "\n").color(color));
        }
        banner = banner.append(Component.text(VERSION_STRING + "\n").color(COLOR_ACCENT));
        return banner;
    }

    private AmetystKQWatermark() {}
}

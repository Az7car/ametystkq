package io.papermc.paper.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public final class AmetystKQWatermark {

    private static final TextColor COLOR_PRIMARY = TextColor.color(0xc77dff);
    private static final TextColor COLOR_SECONDARY = TextColor.color(0x9d4edd);
    private static final TextColor COLOR_ACCENT = TextColor.color(0xe0aaff);
    private static final TextColor COLOR_GRAY = NamedTextColor.GRAY;
    private static final TextColor COLOR_DARK = NamedTextColor.DARK_GRAY;

    private static final String[] BANNER = {
        "\u2584\u2584                                                                 \u2584\u2584   \u2584\u2584\u2584    \u2584\u2584\u2584\u2584   ",
        "   \u2588\u2588\u2588\u2588                          \u2588\u2588                            \u2588\u2588      \u2588\u2588  \u2588\u2588\u2580    \u2588\u2588\u2580\u2580\u2588\u2588  ",
        "   \u2588\u2588\u2588\u2588    \u2588\u2588\u2588\u2584\u2588\u2588\u2584   \u2584\u2588\u2588\u2588\u2588\u2584   \u2588\u2588\u2588\u2588\u2588\u2588\u2588   \u2580\u2588\u2588  \u2588\u2588\u2588  \u2584\u2584\u2588\u2588\u2588\u2588\u2588\u2584  \u2588\u2588\u2588\u2588\u2588\u2588\u2588   \u2588\u2588\u2584\u2588\u2588     \u2588\u2588    \u2588\u2588 ",
        "  \u2588\u2588  \u2588\u2588   \u2588\u2588 \u2588\u2588 \u2588\u2588  \u2588\u2588\u2584\u2584\u2584\u2584\u2588\u2588    \u2588\u2588       \u2588\u2588\u2584 \u2588\u2588   \u2588\u2588\u2584\u2584\u2584\u2584 \u2580    \u2588\u2588      \u2588\u2588\u2588\u2588\u2588     \u2588\u2588    \u2588\u2588 ",
        "  \u2588\u2588\u2588\u2588\u2588\u2588   \u2588\u2588 \u2588\u2588 \u2588\u2588  \u2588\u2588\u2580\u2580\u2580\u2580\u2580\u2580    \u2588\u2588        \u2588\u2588\u2588\u2588\u2580    \u2580\u2580\u2580\u2580\u2588\u2588\u2584    \u2588\u2588      \u2588\u2588  \u2588\u2588\u2584   \u2588\u2588    \u2588\u2588 ",
        " \u2584\u2588\u2588  \u2588\u2588\u2584  \u2588\u2588 \u2588\u2588 \u2588\u2588  \u2580\u2588\u2588\u2584\u2584\u2584\u2584\u2588    \u2588\u2588\u2584\u2584\u2584      \u2588\u2588\u2588    \u2588\u2584\u2584\u2584\u2584\u2584\u2588\u2588    \u2588\u2588\u2584\u2584\u2584   \u2588\u2588   \u2588\u2588\u2584   \u2588\u2588\u2584\u2584\u2588\u2588\u2580 ",
        " \u2580\u2580    \u2580\u2580  \u2580\u2580 \u2580\u2580 \u2580\u2580    \u2580\u2580\u2580\u2580\u2580      \u2580\u2580\u2580\u2580      \u2588\u2588      \u2580\u2580\u2580\u2580\u2580\u2580      \u2580\u2580\u2580\u2580   \u2580\u2580    \u2580\u2580    \u2580\u2580\u2580\u2588\u2588  ",
        "                                          \u2588\u2588\u2588                                          \u2580 ",
    };

    private static final String VERSION_STRING = "AmetystKQ 1.21.11";
    private static final String TAGLINE = "\u2726 High-Performance Paper Fork \u26A1";
    private static final String LINKS = "  FAQ: kq.ametystmc.net/faq  |  Discord: kq.ametystmc.net/discord";

    private static int getTerminalWidth() {
        try {
            String envCols = System.getenv("COLUMNS");
            if (envCols != null) {
                int w = Integer.parseInt(envCols);
                if (w > 0) return w;
            }
        } catch (Exception ignored) {}
        try {
            Terminal terminal = TerminalBuilder.terminal();
            int w = terminal.getWidth();
            if (w > 0) return w;
        } catch (Exception ignored) {}
        return 80;
    }

    private static String[] getScaledBanner(int width) {
        if (width >= 80) {
            return BANNER;
        }
        int artWidth = 80;
        if (width < 40) {
            return new String[]{"AmetystKQ"};
        }
        String[] scaled = new String[BANNER.length];
        int crop = Math.max(0, artWidth - width);
        for (int i = 0; i < BANNER.length; i++) {
            String line = BANNER[i];
            if (crop > 0 && line.length() > crop) {
                scaled[i] = line.substring(crop / 2, Math.min(line.length(), line.length() - crop / 2));
            } else {
                scaled[i] = line;
            }
        }
        return scaled;
    }

    private static String padLine(String line, int width) {
        int visibleLen = line.length();
        if (visibleLen >= width) return line;
        int pad = (width - visibleLen) / 2;
        return " ".repeat(Math.max(0, pad)) + line;
    }

    private static void printAnsi(String line, String color, int termWidth) {
        String padded = padLine(line, termWidth);
        System.out.println(color + padded + "\u001B[0m");
    }

    public static void print() {
        int termWidth = getTerminalWidth();
        String ansiPrimary = "\u001B[38;2;199;125;255m";
        String ansiSecondary = "\u001B[38;2;157;78;221m";
        String ansiAccent = "\u001B[38;2;224;170;255m";

        String[] banner = getScaledBanner(termWidth);

        for (int i = 0; i < banner.length; i++) {
            String color = switch (i) {
                case 0, 2, 4, 6 -> ansiPrimary;
                case 1, 3, 5 -> ansiSecondary;
                case 7 -> ansiAccent;
                default -> ansiPrimary;
            };
            printAnsi(banner[i], color, termWidth);
        }
        printAnsi(VERSION_STRING, ansiAccent, termWidth);
        printAnsi(TAGLINE, ansiAccent, termWidth);
        printAnsi(LINKS, ansiPrimary, termWidth);
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

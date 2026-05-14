package com.example.shellmod;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class AnsiColorParser {
    private final AnsiStyle style = new AnsiStyle();

    public static Text parse(String line) {
        return new AnsiColorParser().parseLine(line);
    }

    public Text parseLine(String line) {
        MutableText result = Text.literal("");
        StringBuilder plain = new StringBuilder();
        int column = 0;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '\u001B') {
                if (i + 1 >= line.length()) {
                    break;
                }

                char next = line.charAt(i + 1);
                if (next == '[') {
                    int sequenceStart = i + 2;
                    int sequenceEnd = sequenceStart;
                    while (sequenceEnd < line.length() && !isCsiFinal(line.charAt(sequenceEnd))) {
                        sequenceEnd++;
                    }

                    if (sequenceEnd >= line.length()) {
                        break;
                    }

                    appendPlain(result, plain, style);
                    String params = line.substring(sequenceStart, sequenceEnd);
                    char command = line.charAt(sequenceEnd);
                    column = handleCsi(result, plain, style, column, params, command);
                    i = sequenceEnd;
                    continue;
                }

                if (next == ']') {
                    i = skipOsc(line, i + 2);
                    continue;
                }

                i++;
                continue;
            }

            if (c == '\r') {
                continue;
            }

            plain.append(c);
            column++;
        }

        appendPlain(result, plain, style);
        return result;
    }

    private static int handleCsi(MutableText result, StringBuilder plain, AnsiStyle style, int column, String params, char command) {
        return switch (command) {
            case 'm' -> {
                applySgr(style, params);
                yield column;
            }
            case 'C' -> {
                int count = firstParam(params, 1);
                appendSpaces(plain, count);
                yield column + count;
            }
            case 'G' -> {
                int target = Math.max(firstParam(params, 1) - 1, 0);
                if (target > column) {
                    int count = target - column;
                    appendSpaces(plain, count);
                    yield target;
                }
                yield column;
            }
            case 'K' -> column;
            default -> column;
        };
    }

    private static void applySgr(AnsiStyle style, String params) {
        int[] codes = parseParams(params);
        if (codes.length == 0) {
            style.reset();
            return;
        }

        for (int i = 0; i < codes.length; i++) {
            int code = codes[i];

            switch (code) {
                case 0 -> style.reset();
                case 1 -> style.bold = true;
                case 3 -> style.italic = true;
                case 4 -> style.underline = true;
                case 9 -> style.strikethrough = true;
                case 22 -> style.bold = false;
                case 23 -> style.italic = false;
                case 24 -> style.underline = false;
                case 29 -> style.strikethrough = false;
                case 30, 31, 32, 33, 34, 35, 36, 37, 90, 91, 92, 93, 94, 95, 96, 97 -> style.color = ansiBasicColor(code);
                case 38 -> {
                    if (i + 1 < codes.length && codes[i + 1] == 5 && i + 2 < codes.length) {
                        style.color = xterm256Color(codes[i + 2]);
                        i += 2;
                    } else if (i + 1 < codes.length && codes[i + 1] == 2 && i + 4 < codes.length) {
                        int r = clampColor(codes[i + 2]);
                        int g = clampColor(codes[i + 3]);
                        int b = clampColor(codes[i + 4]);
                        style.color = (r << 16) | (g << 8) | b;
                        i += 4;
                    }
                }
                case 39 -> style.color = null;
                default -> {
                }
            }
        }
    }

    private static void appendPlain(MutableText result, StringBuilder plain, AnsiStyle style) {
        if (plain.isEmpty()) {
            return;
        }

        result.append(Text.literal(plain.toString()).setStyle(style.toTextStyle()));
        plain.setLength(0);
    }

    private static boolean isCsiFinal(char c) {
        return c >= '@' && c <= '~';
    }

    private static int skipOsc(String line, int start) {
        for (int i = start; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\u0007') {
                return i;
            }
            if (c == '\u001B' && i + 1 < line.length() && line.charAt(i + 1) == '\\') {
                return i + 1;
            }
        }
        return line.length() - 1;
    }

    private static int[] parseParams(String params) {
        if (params == null || params.isEmpty()) {
            return new int[0];
        }

        String cleaned = params.replace("?", "").replace(":", ";");
        String[] parts = cleaned.split(";");
        int[] codes = new int[parts.length];
        int count = 0;

        for (String part : parts) {
            if (part.isEmpty()) {
                codes[count++] = 0;
                continue;
            }

            try {
                codes[count++] = Integer.parseInt(part);
            } catch (NumberFormatException ignored) {
            }
        }

        int[] result = new int[count];
        System.arraycopy(codes, 0, result, 0, count);
        return result;
    }

    private static int firstParam(String params, int fallback) {
        int[] parsed = parseParams(params);
        return parsed.length == 0 || parsed[0] == 0 ? fallback : parsed[0];
    }

    private static void appendSpaces(StringBuilder builder, int count) {
        for (int i = 0; i < count; i++) {
            builder.append(' ');
        }
    }

    private static Integer ansiBasicColor(int code) {
        return switch (code) {
            case 30 -> 0x000000;
            case 31 -> 0xAA0000;
            case 32 -> 0x00AA00;
            case 33 -> 0xFFAA00;
            case 34 -> 0x5555FF;
            case 35 -> 0xFF55FF;
            case 36 -> 0x55FFFF;
            case 37 -> 0xFFFFFF;
            case 90 -> 0x555555;
            case 91 -> 0xFF5555;
            case 92 -> 0x55FF55;
            case 93 -> 0xFFFF55;
            case 94 -> 0x5555FF;
            case 95 -> 0xFF55FF;
            case 96 -> 0x55FFFF;
            case 97 -> 0xFFFFFF;
            default -> null;
        };
    }

    private static int xterm256Color(int code) {
        code = Math.max(0, Math.min(255, code));

        int[] base = {
                0x000000, 0x800000, 0x008000, 0x808000, 0x000080, 0x800080, 0x008080, 0xc0c0c0,
                0x808080, 0xff0000, 0x00ff00, 0xffff00, 0x0000ff, 0xff00ff, 0x00ffff, 0xffffff
        };
        if (code < 16) {
            return base[code];
        }

        if (code < 232) {
            int value = code - 16;
            int r = value / 36;
            int g = (value / 6) % 6;
            int b = value % 6;
            return (xtermComponent(r) << 16) | (xtermComponent(g) << 8) | xtermComponent(b);
        }

        int gray = 8 + (code - 232) * 10;
        return (gray << 16) | (gray << 8) | gray;
    }

    private static int xtermComponent(int value) {
        return value == 0 ? 0 : 55 + value * 40;
    }

    private static int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static class AnsiStyle {
        private Integer color;
        private boolean bold;
        private boolean italic;
        private boolean underline;
        private boolean strikethrough;

        private void reset() {
            color = null;
            bold = false;
            italic = false;
            underline = false;
            strikethrough = false;
        }

        private Style toTextStyle() {
            Style style = Style.EMPTY;
            if (color != null) {
                style = style.withColor(color);
            }
            if (bold) {
                style = style.withBold(true);
            }
            if (italic) {
                style = style.withItalic(true);
            }
            if (underline) {
                style = style.withUnderline(true);
            }
            if (strikethrough) {
                style = style.withStrikethrough(true);
            }
            return style;
        }
    }
}

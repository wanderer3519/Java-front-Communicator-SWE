package com.swe.canvas.datamodel.serialization;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.swe.canvas.datamodel.shape.Point;

/**
 * A utility class containing low-level static helper methods for manually
 * parsing and constructing JSON strings, without any external libraries.
 * Updated to handle Integers and Whitespace/Newlines from .NET JSON.
 */
public final class JsonUtils {

    private JsonUtils() {
    }

    // =========================================================================
    // Construction Helpers
    // =========================================================================

    public static String colorToHex(final Color color) {
        if (color == null) {
            return "#00000000";
        }
        return String.format("#%08X", color.getRGB());
    }

    public static String jsonEscape(final String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\"", "\\\"") + "\"";
    }

    // =========================================================================
    // Parsing (Extraction) Helpers
    // =========================================================================

    public static Color hexToColor(final String hexString) {
        if (hexString == null || hexString.length() != 9 || !hexString.startsWith("#")) {
            return Color.BLACK;
        }
        try {
            return new Color((int) Long.parseLong(hexString.substring(1), 16), true);
        } catch (NumberFormatException e) {
            return Color.BLACK;
        }
    }

    /**
     * Extracts a string value. Handles whitespace around colon.
     */
    public static String extractString(final String content, final String key) {
        // Pattern: "key" \s* : \s* "value"
        final Pattern pattern = Pattern.compile(Pattern.quote("\"" + key + "\"") + "\\s*:\\s*\"(.*?)\"");
        final Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * Extracts a double value (handles integers in JSON too).
     */
    public static double extractDouble(final String content, final String key) {
        // Pattern: "key" \s* : \s* number
        final Pattern pattern = Pattern.compile(Pattern.quote("\"" + key + "\"") + "\\s*:\\s*([\\-0-9\\.]+)");
        final Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    public static long extractLong(final String content, final String key) {
        final Pattern pattern = Pattern.compile(Pattern.quote("\"" + key + "\"") + "\\s*:\\s*(\\d+)");
        final Matcher matcher = pattern.matcher(content);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : 0L;
    }

    public static boolean extractBoolean(final String content, final String key) {
        final Pattern pattern = Pattern.compile(Pattern.quote("\"" + key + "\"") + "\\s*:\\s*(true|false)");
        final Matcher matcher = pattern.matcher(content);
        return matcher.find() && Boolean.parseBoolean(matcher.group(1));
    }

    /**
     * Extracts the Points array content, handling newlines.
     */
    public static List<Point> extractPoints(final String content) {
        final List<Point> points = new ArrayList<>();

        // (?s) enables DOTALL mode so . matches newlines
        // Pattern: "Points" \s* : \s* [ ... ]
        final Pattern arrayPattern = Pattern.compile("(?s)" + Pattern.quote("\"Points\":") + "\\s*\\[(.*?)\\]");
        final Matcher arrayMatcher = arrayPattern.matcher(content);

        if (arrayMatcher.find()) {
            final String pointsArrayContent = arrayMatcher.group(1);

            // Pattern for individual points: "X" \s* : \s* num , \s* "Y" \s* : \s* num
            // We use a broader regex to catch { "X": 1, "Y": 2 } with spaces/newlines
            final Pattern pointPattern = Pattern.compile(
                    "(?s)\\{\\s*\"X\"\\s*:\\s*([\\-0-9\\.]+)\\s*,\\s*\"Y\"\\s*:\\s*([\\-0-9\\.]+)\\s*\\}"
            );
            final Matcher pointMatcher = pointPattern.matcher(pointsArrayContent);

            while (pointMatcher.find()) {
                final double x = Double.parseDouble(pointMatcher.group(1));
                final double y = Double.parseDouble(pointMatcher.group(2));
                points.add(new Point(x, y));
            }
        }
        return points;
    }

    /**
     * Extracts a nested JSON object.
     * Uses strict brace counting, robust against whitespace.
     */
    public static String extractNestedJson(final String content, final String key) {
        // Find "key"
        String searchKey = "\"" + key + "\"";
        int keyIndex = content.indexOf(searchKey);
        if (keyIndex == -1) return null;

        // Find colon after key
        int colonIndex = content.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1) return null;

        // Scan for start of value (skip whitespace)
        int valueStart = -1;
        for (int i = colonIndex + 1; i < content.length(); i++) {
            char c = content.charAt(i);
            if (!Character.isWhitespace(c)) {
                valueStart = i;
                break;
            }
        }
        if (valueStart == -1) return null;

        // Check for 'null'
        if (content.startsWith("null", valueStart)) return "null";

        // Must be '{'
        if (content.charAt(valueStart) != '{') return null;

        // Brace counting
        int balance = 0;
        int objectEnd = -1;
        for (int i = valueStart; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') {
                balance++;
            } else if (c == '}') {
                balance--;
                if (balance == 0) {
                    objectEnd = i + 1; // Include closing brace
                    break;
                }
            }
        }

        if (objectEnd != -1) {
            return content.substring(valueStart, objectEnd);
        }
        return null;
    }
}
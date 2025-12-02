/*
 * -----------------------------------------------------------------------------
 * File: JsonUtils.java
 * Owner: Gajjala Bhavani Shankar
 * Roll Number : 112201026
 * Module : Canvas
 *
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.serialization;

import com.swe.canvas.datamodel.shape.Point;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility class containing low-level static helper methods for manually
 * parsing and constructing JSON strings, without any external libraries.
 */
public final class JsonUtils {

    /** Expected length of a hex color string (#AARRGGBB). */
    private static final int HEX_STR_LENGTH = 9;

    /** Radix for hex parsing. */
    private static final int HEX_RADIX = 16;

    /** Group index for the first capture group in regex. */
    private static final int GROUP_ONE = 1;

    /** Group index for the second capture group in regex. */
    private static final int GROUP_TWO = 2;

    private JsonUtils() {
    }

    // =========================================================================
    // Construction Helpers
    // =========================================================================

    /**
     * Converts a Color object to a hex string format (#AARRGGBB).
     *
     * @param color The color to convert.
     * @return The hex string representation, or a default transparent black if null.
     */
    public static String colorToHex(final Color color) {
        if (color == null) {
            return "#00000000";
        }
        return String.format("#%08X", color.getRGB());
    }

    /**
     * Escapes a string value for safe inclusion in a JSON string.
     * Replaces double quotes with escaped quotes.
     *
     * @param value The raw string value.
     * @return The escaped JSON string wrapped in quotes, or "null".
     */
    public static String jsonEscape(final String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\"", "\\\"") + "\"";
    }

    // =========================================================================
    // Parsing (Extraction) Helpers
    // =========================================================================

    /**
     * Parses a hex string (#AARRGGBB) into a Color object.
     *
     * @param hexString The hex string to parse.
     * @return The Color object, or Black if invalid.
     */
    public static Color hexToColor(final String hexString) {
        if (hexString == null || hexString.length() != HEX_STR_LENGTH
                || !hexString.startsWith("#")) {
            return Color.BLACK;
        }
        try {
            return new Color((int) Long.parseLong(hexString.substring(1), HEX_RADIX), true);
        } catch (final NumberFormatException e) {
            return Color.BLACK;
        }
    }

    /**
     * Extracts a string value associated with a key from a JSON string.
     * Handles whitespace around the colon.
     *
     * @param content The JSON content string.
     * @param key     The key to search for.
     * @return The extracted string value, or null if not found.
     */
    public static String extractString(final String content, final String key) {
        // Pattern: "key" \s* : \s* "value"
        final Pattern pattern = Pattern.compile(Pattern.quote("\"" + key + "\"")
                + "\\s*:\\s*\"(.*?)\"");
        final Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(GROUP_ONE);
        }
        return null;
    }

    /**
     * Extracts a double value associated with a key.
     * Handles integers appearing as values as well.
     *
     * @param content The JSON content string.
     * @param key     The key to search for.
     * @return The double value, or 0.0 if not found or invalid.
     */
    public static double extractDouble(final String content, final String key) {
        // Pattern: "key" \s* : \s* number
        final Pattern pattern = Pattern.compile(Pattern.quote("\"" + key + "\"")
                + "\\s*:\\s*([\\-0-9\\.]+)");
        final Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(GROUP_ONE));
            } catch (final NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    /**
     * Extracts a long value associated with a key.
     *
     * @param content The JSON content string.
     * @param key     The key to search for.
     * @return The long value, or 0L if not found.
     */
    public static long extractLong(final String content, final String key) {
        final Pattern pattern = Pattern.compile(Pattern.quote("\"" + key + "\"")
                + "\\s*:\\s*(\\d+)");
        final Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(GROUP_ONE));
        }
        return 0L;
    }

    /**
     * Extracts a boolean value associated with a key.
     *
     * @param content The JSON content string.
     * @param key     The key to search for.
     * @return The boolean value, or false if not found.
     */
    public static boolean extractBoolean(final String content, final String key) {
        final Pattern pattern = Pattern.compile(Pattern.quote("\"" + key + "\"")
                + "\\s*:\\s*(true|false)");
        final Matcher matcher = pattern.matcher(content);
        return matcher.find() && Boolean.parseBoolean(matcher.group(GROUP_ONE));
    }

    /**
     * Extracts a list of Points from the "Points" JSON array.
     * Handles newlines via DOTALL regex mode.
     *
     * @param content The JSON content string.
     * @return A list of Point objects.
     */
    public static List<Point> extractPoints(final String content) {
        final List<Point> points = new ArrayList<>();

        // (?s) enables DOTALL mode so . matches newlines
        // Pattern: "Points" \s* : \s* [ ... ]
        final Pattern arrayPattern = Pattern.compile("(?s)" + Pattern.quote("\"Points\":")
                + "\\s*\\[(.*?)\\]");
        final Matcher arrayMatcher = arrayPattern.matcher(content);

        if (arrayMatcher.find()) {
            final String pointsArrayContent = arrayMatcher.group(GROUP_ONE);

            // Pattern for individual points: "X" \s* : \s* num , \s* "Y" \s* : \s* num
            final Pattern pointPattern = Pattern.compile(
                    "(?s)\\{\\s*\"X\"\\s*:\\s*([\\-0-9\\.]+)\\s*,\\s*\"Y\"\\s*:\\s*([\\-0-9\\.]+)\\s*\\}");
            final Matcher pointMatcher = pointPattern.matcher(pointsArrayContent);

            while (pointMatcher.find()) {
                final double x = Double.parseDouble(pointMatcher.group(GROUP_ONE));
                final double y = Double.parseDouble(pointMatcher.group(GROUP_TWO));
                points.add(new Point(x, y));
            }
        }
        return points;
    }

    /**
     * Extracts a nested JSON object for a specific key.
     * Uses strict brace counting, robust against whitespace.
     *
     * @param content The JSON content string.
     * @param key     The key to search for.
     * @return The nested JSON string (including braces), or null/ "null" string.
     */
    public static String extractNestedJson(final String content, final String key) {
        final int valueStart = findValueStart(content, key);
        if (valueStart == -1) {
            return null;
        }

        // Check for 'null' literal
        if (content.startsWith("null", valueStart)) {
            return "null";
        }

        // Must be '{'
        if (content.charAt(valueStart) != '{') {
            return null;
        }

        return extractObjectString(content, valueStart);
    }

    /**
     * Finds the start index of the value associated with the key.
     *
     * @param content The JSON content.
     * @param key     The key to search for.
     * @return The index where the value starts, or -1.
     */
    private static int findValueStart(final String content, final String key) {
        final String searchKey = "\"" + key + "\"";
        final int keyIndex = content.indexOf(searchKey);
        if (keyIndex == -1) {
            return -1;
        }

        final int colonIndex = content.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1) {
            return -1;
        }

        for (int i = colonIndex + 1; i < content.length(); i++) {
            if (!Character.isWhitespace(content.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Extracts a full JSON object string starting from the opening brace.
     * Considers nested braces.
     *
     * @param content The JSON content.
     * @param start   The index of the opening brace '{'.
     * @return The full JSON object string, or null if unbalanced.
     */
    private static String extractObjectString(final String content, final int start) {
        int balance = 0;
        for (int i = start; i < content.length(); i++) {
            final char c = content.charAt(i);
            if (c == '{') {
                balance++;
            } else if (c == '}') {
                balance--;
                if (balance == 0) {
                    return content.substring(start, i + 1);
                }
            }
        }
        return null;
    }
}
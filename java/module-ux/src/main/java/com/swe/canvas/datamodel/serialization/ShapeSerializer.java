/*
 * -----------------------------------------------------------------------------
 * File: ShapeSerializer.java
 * Owner: Gajjala Bhavani Shankar
 * Roll Number : 112201026
 * Module : Canvas
 *
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.serialization;

import com.swe.canvas.datamodel.canvas.ShapeState;
import com.swe.canvas.datamodel.shape.EllipseShape;
import com.swe.canvas.datamodel.shape.FreehandShape;
import com.swe.canvas.datamodel.shape.LineShape;
import com.swe.canvas.datamodel.shape.Point;
import com.swe.canvas.datamodel.shape.RectangleShape;
import com.swe.canvas.datamodel.shape.Shape;
import com.swe.canvas.datamodel.shape.ShapeId;
import com.swe.canvas.datamodel.shape.ShapeType;
import com.swe.canvas.datamodel.shape.TriangleShape;
import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for serializing and deserializing ShapeState objects to/from JSON.
 */
public final class ShapeSerializer {

    /** Opening brace character. */
    private static final char OPEN_BRACE = '{';

    /** Closing brace character. */
    private static final char CLOSE_BRACE = '}';

    /** Double quote character. */
    private static final char QUOTE = '"';

    /** Colon character. */
    private static final char COLON = ':';

    /** Comma character. */
    private static final char COMMA = ',';

    private ShapeSerializer() {
        // Utility class
    }

    /**
     * Serializes a ShapeState to a JSON string.
     *
     * @param shapeState The state to serialize.
     * @return The JSON string.
     */
    public static String serializeShape(final ShapeState shapeState) {
        if (shapeState == null || shapeState.getShape() == null) {
            return null;
        }

        final Shape shape = shapeState.getShape();
        final StringBuilder sb = new StringBuilder();

        sb.append(OPEN_BRACE);

        sb.append(JsonUtils.jsonEscape("ShapeId")).append(COLON)
                .append(JsonUtils.jsonEscape(shape.getShapeId().getValue())).append(COMMA);
        sb.append(JsonUtils.jsonEscape("Type")).append(COLON)
                .append(JsonUtils.jsonEscape(shape.getShapeType().toString())).append(COMMA);

        appendPoints(sb, shape.getPoints());
        sb.append(COMMA);

        sb.append(JsonUtils.jsonEscape("Color")).append(COLON)
                .append(JsonUtils.jsonEscape(JsonUtils.colorToHex(shape.getColor()))).append(COMMA);
        sb.append(JsonUtils.jsonEscape("Thickness")).append(COLON)
                .append((int) shape.getThickness()).append(COMMA);

        sb.append(JsonUtils.jsonEscape("CreatedBy")).append(COLON)
                .append(JsonUtils.jsonEscape(shape.getCreatedBy())).append(COMMA);
        sb.append(JsonUtils.jsonEscape("LastModifiedBy")).append(COLON)
                .append(JsonUtils.jsonEscape(shape.getLastUpdatedBy())).append(COMMA);

        sb.append(JsonUtils.jsonEscape("LastModified")).append(COLON)
                .append(shapeState.getLastModified()).append(COMMA);

        sb.append(JsonUtils.jsonEscape("IsDeleted")).append(COLON)
                .append(shapeState.isDeleted());

        sb.append(CLOSE_BRACE);
        return sb.toString();
    }

    private static void appendPoints(final StringBuilder sb, final List<Point> points) {
        sb.append(JsonUtils.jsonEscape("Points")).append(":[");
        for (int i = 0; i < points.size(); i++) {
            final Point p = points.get(i);
            sb.append(OPEN_BRACE);
            sb.append(JsonUtils.jsonEscape("X")).append(COLON).append((int) p.getX()).append(COMMA);
            sb.append(JsonUtils.jsonEscape("Y")).append(COLON).append((int) p.getY());
            sb.append(CLOSE_BRACE);
            if (i < points.size() - 1) {
                sb.append(COMMA);
            }
        }
        sb.append("]");
    }

    /**
     * Deserializes a JSON string into a ShapeState object.
     *
     * @param json The JSON string.
     * @return The ShapeState object.
     */
    public static ShapeState deserializeShape(final String json) {
        if (json == null || json.isEmpty() || "null".equals(json)) {
            return null;
        }

        try {
            String content = json.trim();
            if (content.startsWith(String.valueOf(OPEN_BRACE))) {
                content = content.substring(1, content.length() - 1);
            }

            final String shapeId = JsonUtils.extractString(content, "ShapeId");
            final String typeName = JsonUtils.extractString(content, "Type");
            final String colorHex = JsonUtils.extractString(content, "Color");
            final double thickness = JsonUtils.extractDouble(content, "Thickness");
            final String createdBy = JsonUtils.extractString(content, "CreatedBy");
            final String lastModBy = JsonUtils.extractString(content, "LastModifiedBy");
            final boolean isDeleted = JsonUtils.extractBoolean(content, "IsDeleted");
            final long lastModified = JsonUtils.extractLong(content, "LastModified");
            final List<Point> points = JsonUtils.extractPoints(content);

            if (shapeId == null || typeName == null || createdBy == null
                    || lastModBy == null || points == null) {
                throw new SerializationException("Missing crucial shape field.");
            }

            final ShapeType shapeType = ShapeType.valueOf(typeName);
            final Color color = JsonUtils.hexToColor(colorHex);
            final ShapeId id = new ShapeId(shapeId);

            final Shape newShape = buildShape(shapeType, id, points, thickness,
                    color, createdBy, lastModBy);

            return new ShapeState(newShape, isDeleted, lastModified);

        } catch (final Exception e) {
            throw new SerializationException("Failed to deserialize ShapeState: "
                    + e.getMessage(), e);
        }
    }

    private static Shape buildShape(final ShapeType type, final ShapeId id,
                                    final List<Point> points, final double thickness,
                                    final Color color, final String createdBy,
                                    final String lastModBy) {
        switch (type) {
            case FREEHAND:
                return new FreehandShape(id, points, thickness, color, createdBy, lastModBy);
            case RECTANGLE:
                return new RectangleShape(id, points, thickness, color, createdBy, lastModBy);
            case TRIANGLE:
                return new TriangleShape(id, points, thickness, color, createdBy, lastModBy);
            case LINE:
                return new LineShape(id, points, thickness, color, createdBy, lastModBy);
            case ELLIPSE:
                return new EllipseShape(id, points, thickness, color, createdBy, lastModBy);
            default:
                throw new SerializationException("Unknown ShapeType: " + type);
        }
    }

    /**
     * Serializes a map of Shape IDs to ShapeStates.
     *
     * @param shapes The map to serialize.
     * @return The JSON string.
     */
    public static String serializeShapesMap(final Map<ShapeId, ShapeState> shapes) {
        if (shapes == null || shapes.isEmpty()) {
            return "{}";
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(OPEN_BRACE).append("\n");
        int i = 0;
        for (final Map.Entry<ShapeId, ShapeState> entry : shapes.entrySet()) {
            sb.append("  ").append(JsonUtils.jsonEscape(entry.getKey().getValue()));
            sb.append(COLON).append(" ");
            final String shapeJson = serializeShape(entry.getValue());

            if (shapeJson != null) {
                sb.append(shapeJson);
            } else {
                sb.append("null");
            }

            if (i < shapes.size() - 1) {
                sb.append(COMMA);
            }
            sb.append("\n");
            i++;
        }
        sb.append(CLOSE_BRACE);
        return sb.toString();
    }

    /**
     * Deserializes a JSON string into a map of ShapeStates.
     *
     * @param json The JSON string.
     * @return The map of ShapeIds to ShapeStates.
     */
    public static Map<ShapeId, ShapeState> deserializeShapesMap(final String json) {
        final Map<ShapeId, ShapeState> map = new HashMap<>();

        final String content = cleanJson(json);
        if (content.isEmpty()) {
            return map;
        }

        int index = 0;
        final int length = content.length();

        while (index < length) {
            final int newIndex = processNextEntry(content, index, map);
            if (newIndex == -1) {
                break;
            }
            index = newIndex;
        }
        return map;
    }

    private static String cleanJson(final String json) {
        if (json == null || json.trim().length() < 2) {
            return "";
        }
        String content = json.trim();
        if (content.startsWith(String.valueOf(OPEN_BRACE))) {
            content = content.substring(1);
        }
        if (content.endsWith(String.valueOf(CLOSE_BRACE))) {
            content = content.substring(0, content.length() - 1);
        }
        return content.trim();
    }

    /**
     * Processes the next key-value entry in the JSON map string.
     *
     * @param content The JSON content.
     * @param index   The current index.
     * @param map     The map to populate.
     * @return The new index after processing, or -1 if no entry found.
     */
    private static int processNextEntry(final String content, final int index,
                                        final Map<ShapeId, ShapeState> map) {
        // 1. Skip whitespace and find start of key
        final int keyStart = findNextKeyStart(content, index);
        if (keyStart == -1) {
            return -1;
        }

        // 2. Extract Key end
        final int keyEnd = content.indexOf(QUOTE, keyStart);
        if (keyEnd == -1) {
            return -1;
        }

        // 3. Find start of value (json object)
        final int valueStart = content.indexOf(OPEN_BRACE, keyEnd);
        if (valueStart == -1) {
            return -1;
        }

        // 4. Extract Value (nested JSON object)
        final String shapeJson = extractJsonValue(content, valueStart);
        if (shapeJson == null) {
            return -1;
        }

        // 5. Deserialize and add to map
        final ShapeState state = deserializeShape(shapeJson);
        if (state != null && state.getShape() != null) {
            map.put(state.getShape().getShapeId(), state);
        }

        // 6. Calculate next index
        return calculateNextIndex(content, valueStart + shapeJson.length());
    }

    private static int calculateNextIndex(final String content, final int processedUpTo) {
        int index = processedUpTo;
        final int length = content.length();
        while (index < length && (Character.isWhitespace(content.charAt(index))
                || content.charAt(index) == COMMA)) {
            index++;
        }
        return index;
    }

    private static int findNextKeyStart(final String content, final int startIndex) {
        int i = startIndex;
        final int length = content.length();
        while (i < length && (Character.isWhitespace(content.charAt(i))
                || content.charAt(i) == COMMA)) {
            i++;
        }
        if (i >= length || content.charAt(i) != QUOTE) {
            return -1;
        }
        return i + 1; // Return index after opening quote
    }

    private static String extractJsonValue(final String content, final int start) {
        int braceCount = 0;
        for (int i = start; i < content.length(); i++) {
            final char c = content.charAt(i);
            if (c == OPEN_BRACE) {
                braceCount++;
            } else if (c == CLOSE_BRACE) {
                braceCount--;
            }

            if (braceCount == 0) {
                return content.substring(start, i + 1);
            }
        }
        return null;
    }
}
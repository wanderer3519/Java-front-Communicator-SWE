package com.swe.canvas.datamodel.serialization;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.swe.canvas.datamodel.action.Action;
import com.swe.canvas.datamodel.action.ActionType;
import com.swe.canvas.datamodel.action.CreateShapeAction;
import com.swe.canvas.datamodel.action.DeleteShapeAction;
import com.swe.canvas.datamodel.action.ModifyShapeAction;
import com.swe.canvas.datamodel.action.ResurrectShapeAction;
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

/**
 * Provides manual JSON serialization and deserialization for the Canvas data model.
 */
public final class ShapeSerializer {

    private ShapeSerializer() {}

    // ... [Existing methods: serializeShape, deserializeShape, serializeAction, deserializeAction] ...
    // (Include previous logic here to ensure the file is complete, or append the new methods below)

    // --- RE-INSERT EXISTING SINGLE SHAPE/ACTION LOGIC FOR CONTEXT ---

    public static String serializeShape(final ShapeState shapeState) {
        if (shapeState == null || shapeState.getShape() == null) return null;
        final Shape shape = shapeState.getShape();
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append(JsonUtils.jsonEscape("ShapeId")).append(":").append(JsonUtils.jsonEscape(shape.getShapeId().getValue())).append(",");
        sb.append(JsonUtils.jsonEscape("Type")).append(":").append(JsonUtils.jsonEscape(shape.getShapeType().toString())).append(",");
        sb.append(JsonUtils.jsonEscape("Points")).append(":[");
        final List<Point> points = shape.getPoints();
        for (int i = 0; i < points.size(); i++) {
            final Point p = points.get(i);
            sb.append("{").append(JsonUtils.jsonEscape("X")).append(":").append(p.getX()).append(",")
                    .append(JsonUtils.jsonEscape("Y")).append(":").append(p.getY()).append("}");
            if (i < points.size() - 1) sb.append(",");
        }
        sb.append("],");
        sb.append(JsonUtils.jsonEscape("Color")).append(":").append(JsonUtils.jsonEscape(JsonUtils.colorToHex(shape.getColor()))).append(",");
        sb.append(JsonUtils.jsonEscape("Thickness")).append(":").append(shape.getThickness()).append(",");
        sb.append(JsonUtils.jsonEscape("CreatedBy")).append(":").append(JsonUtils.jsonEscape(shape.getCreatedBy())).append(",");
        sb.append(JsonUtils.jsonEscape("LastModifiedBy")).append(":").append(JsonUtils.jsonEscape(shape.getLastUpdatedBy())).append(",");
        sb.append(JsonUtils.jsonEscape("IsDeleted")).append(":").append(shapeState.isDeleted());
        sb.append("}");
        return sb.toString();
    }

    public static ShapeState deserializeShape(final String json) {
        if (json == null || json.isEmpty() || "null".equals(json)) return null;
        try {
            String content = json.trim();
            if (content.startsWith("{")) content = content.substring(1, content.length() - 1);

            final String shapeId = JsonUtils.extractString(content, "ShapeId");
            final String typeName = JsonUtils.extractString(content, "Type");
            final String colorHex = JsonUtils.extractString(content, "Color");
            final double thickness = JsonUtils.extractDouble(content, "Thickness");
            final String createdBy = JsonUtils.extractString(content, "CreatedBy");
            final String lastModifiedBy = JsonUtils.extractString(content, "LastModifiedBy");
            final boolean isDeleted = JsonUtils.extractBoolean(content, "IsDeleted");
            final List<Point> points = JsonUtils.extractPoints(content);

            if (shapeId == null || typeName == null) throw new SerializationException("Missing shape fields.");

            final ShapeType shapeType = ShapeType.valueOf(typeName);
            final Color color = JsonUtils.hexToColor(colorHex);
            final ShapeId id = new ShapeId(shapeId);
            final Shape newShape;

            switch (shapeType) {
                case FREEHAND: newShape = new FreehandShape(id, points, thickness, color, createdBy, lastModifiedBy); break;
                case RECTANGLE: newShape = new RectangleShape(id, points, thickness, color, createdBy, lastModifiedBy); break;
                case TRIANGLE: newShape = new TriangleShape(id, points, thickness, color, createdBy, lastModifiedBy); break;
                case LINE: newShape = new LineShape(id, points, thickness, color, createdBy, lastModifiedBy); break;
                case ELLIPSE: newShape = new EllipseShape(id, points, thickness, color, createdBy, lastModifiedBy); break;
                default: throw new SerializationException("Unknown ShapeType: " + typeName);
            }
            return new ShapeState(newShape, isDeleted, 0L);
        } catch (Exception e) {
            throw new SerializationException("Deserialization failed: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // NEW: Dictionary Serialization for Save/Restore
    // =========================================================================

    /**
     * Serializes a Map of ShapeId -> ShapeState into a JSON object.
     * Format: {"shape-id-1": { ...shape... }, "shape-id-2": { ...shape... }}
     */
    public static String serializeShapesMap(final Map<ShapeId, ShapeState> shapes) {
        if (shapes == null || shapes.isEmpty()) {
            return "{}";
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\n"); // Pretty print newline

        int i = 0;
        for (Map.Entry<ShapeId, ShapeState> entry : shapes.entrySet()) {
            // Key: ShapeId string
            sb.append("  ").append(JsonUtils.jsonEscape(entry.getKey().getValue()));
            sb.append(": ");

            // Value: ShapeState object
            final String shapeJson = serializeShape(entry.getValue());
            sb.append(shapeJson != null ? shapeJson : "null");

            if (i < shapes.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
            i++;
        }

        sb.append("}");
        return sb.toString();
    }

    /**
     * Deserializes a JSON object back into a Map of ShapeId -> ShapeState.
     * Uses brace counting to safely extract nested shape objects.
     */
    public static Map<ShapeId, ShapeState> deserializeShapesMap(final String json) {
        final Map<ShapeId, ShapeState> map = new HashMap<>();
        if (json == null || json.trim().length() < 2) {
            return map;
        }

        String content = json.trim();
        if (content.startsWith("{")) content = content.substring(1);
        if (content.endsWith("}")) content = content.substring(0, content.length() - 1);
        content = content.trim();

        if (content.isEmpty()) {
            return map;
        }

        int index = 0;
        final int length = content.length();

        while (index < length) {
            // 1. Find Key
            // Skip whitespace/comma
            while (index < length && (Character.isWhitespace(content.charAt(index)) || content.charAt(index) == ',')) {
                index++;
            }
            if (index >= length) break;

            if (content.charAt(index) != '"') break; // Should be quote

            int keyStart = index + 1;
            int keyEnd = content.indexOf('"', keyStart);
            if (keyEnd == -1) break;

            String keyString = content.substring(keyStart, keyEnd);

            // 2. Find Value (skip to colon, then to value start)
            index = keyEnd + 1;
            int valueStart = -1;
            while (index < length) {
                char c = content.charAt(index);
                if (c == ':') {
                    // Colon found, find next non-whitespace
                    index++;
                    while (index < length && Character.isWhitespace(content.charAt(index))) index++;
                    valueStart = index;
                    break;
                }
                index++;
            }

            if (valueStart == -1 || valueStart >= length) break;

            // 3. Extract JSON Object (Brace counting)
            if (content.charAt(valueStart) == '{') {
                int braceCount = 0;
                int valueEnd = -1;
                for (int i = valueStart; i < length; i++) {
                    char c = content.charAt(i);
                    if (c == '{') braceCount++;
                    else if (c == '}') {
                        braceCount--;
                        if (braceCount == 0) {
                            valueEnd = i + 1;
                            break;
                        }
                    }
                }

                if (valueEnd != -1) {
                    String shapeJson = content.substring(valueStart, valueEnd);
                    ShapeState state = deserializeShape(shapeJson);
                    if (state != null && state.getShape() != null) {
                        map.put(state.getShape().getShapeId(), state);
                    }
                    index = valueEnd;
                } else {
                    break; // Malformed
                }
            } else {
                // Handle null or other primitives if necessary (unlikely for this schema)
                index++;
            }
        }
        return map;
    }
}
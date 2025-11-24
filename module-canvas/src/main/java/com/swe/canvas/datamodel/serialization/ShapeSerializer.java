/*
 * -----------------------------------------------------------------------------
 * File: ShapeSerializer.java
 * Module: Canvas
 * -----------------------------------------------------------------------------
 */

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

public final class ShapeSerializer {

    private ShapeSerializer() { }

    /**
     * Serializes a ShapeState to JSON.
     */
    public static String serializeShape(final ShapeState shapeState) {
        if (shapeState == null || shapeState.getShape() == null) {
            return null;
        }

        final Shape shape = shapeState.getShape();
        final StringBuilder sb = new StringBuilder();

        sb.append("{");

        sb.append(JsonUtils.jsonEscape("ShapeId")).append(":").append(JsonUtils.jsonEscape(shape.getShapeId().getValue())).append(",");
        sb.append(JsonUtils.jsonEscape("Type")).append(":").append(JsonUtils.jsonEscape(shape.getShapeType().toString())).append(",");

        sb.append(JsonUtils.jsonEscape("Points")).append(":[");
        final List<Point> points = shape.getPoints();
        for (int i = 0; i < points.size(); i++) {
            final Point p = points.get(i);
            sb.append("{");
            sb.append(JsonUtils.jsonEscape("X")).append(":").append((int) p.getX()).append(",");
            sb.append(JsonUtils.jsonEscape("Y")).append(":").append((int) p.getY());
            sb.append("}");
            if (i < points.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("],");

        sb.append(JsonUtils.jsonEscape("Color")).append(":").append(JsonUtils.jsonEscape(JsonUtils.colorToHex(shape.getColor()))).append(",");
        sb.append(JsonUtils.jsonEscape("Thickness")).append(":").append((int) shape.getThickness()).append(",");

        sb.append(JsonUtils.jsonEscape("CreatedBy")).append(":").append(JsonUtils.jsonEscape(shape.getCreatedBy())).append(",");
        sb.append(JsonUtils.jsonEscape("LastModifiedBy")).append(":").append(JsonUtils.jsonEscape(shape.getLastUpdatedBy())).append(",");
        
        // --- ADDED: LastModified ---
        sb.append(JsonUtils.jsonEscape("LastModified")).append(":").append(shapeState.getLastModified()).append(",");
        
        sb.append(JsonUtils.jsonEscape("IsDeleted")).append(":").append(shapeState.isDeleted());

        sb.append("}");
        return sb.toString();
    }

    public static ShapeState deserializeShape(final String json) {
        if (json == null || json.isEmpty() || "null".equals(json)) {
            return null;
        }

        try {
            String content = json.trim();
            if (content.startsWith("{")) {
                content = content.substring(1, content.length() - 1);
            }

            final String shapeId = JsonUtils.extractString(content, "ShapeId");
            final String typeName = JsonUtils.extractString(content, "Type");
            final String colorHex = JsonUtils.extractString(content, "Color");
            final double thickness = JsonUtils.extractDouble(content, "Thickness");
            final String createdBy = JsonUtils.extractString(content, "CreatedBy");
            final String lastModifiedBy = JsonUtils.extractString(content, "LastModifiedBy");
            final boolean isDeleted = JsonUtils.extractBoolean(content, "IsDeleted");
            
            // --- ADDED: LastModified ---
            final long lastModified = JsonUtils.extractLong(content, "LastModified");

            final List<Point> points = JsonUtils.extractPoints(content);

            if (shapeId == null || typeName == null || createdBy == null || lastModifiedBy == null || points == null) {
                throw new SerializationException("Missing crucial shape field.");
            }

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

            // Pass the deserialized timestamp instead of 0L
            return new ShapeState(newShape, isDeleted, lastModified);

        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize ShapeState: " + e.getMessage(), e);
        }
    }
    
    // ... [Rest of the file: serializeShapesMap, deserializeShapesMap - keep exactly as they were] ...
    
    public static String serializeShapesMap(final Map<ShapeId, ShapeState> shapes) {
        if (shapes == null || shapes.isEmpty()) {
            return "{}";
        }
        final StringBuilder sb = new StringBuilder();
        sb.append("{").append("\n");
        int i = 0;
        for (Map.Entry<ShapeId, ShapeState> entry : shapes.entrySet()) {
            sb.append("  ").append(JsonUtils.jsonEscape(entry.getKey().getValue()));
            sb.append(": ");
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

    public static Map<ShapeId, ShapeState> deserializeShapesMap(final String json) {
        final Map<ShapeId, ShapeState> map = new HashMap<>();
        if (json == null || json.trim().length() < 2) {
            return map;
        }

        String content = json.trim();
        if (content.startsWith("{")) {
            content = content.substring(1);
        }
        if (content.endsWith("}")) {
            content = content.substring(0, content.length() - 1);
        }
        content = content.trim();

        if (content.isEmpty()) {
            return map;
        }

        int index = 0;
        final int length = content.length();

        while (index < length) {
            while (index < length && (Character.isWhitespace(content.charAt(index)) || content.charAt(index) == ',')) {
                index++;
            }
            if (index >= length) {
                break;
            }
            if (content.charAt(index) != '"') {
                break;
            }

            int keyStart = index + 1;
            int keyEnd = content.indexOf('"', keyStart);
            if (keyEnd == -1) {
                break;
            }

            index = keyEnd + 1;
            while (index < length && (Character.isWhitespace(content.charAt(index)) || content.charAt(index) == ':')) {
                index++;
            }

            if (index < length && content.charAt(index - 1) != '{') {
                 while (index < length && content.charAt(index) != '{') {
                     index++;
                 }
            }

            if (index < length && content.charAt(index) == '{') {
                int braceCount = 1;
                int end = index + 1;
                while (end < length && braceCount > 0) {
                    if (content.charAt(end) == '{') {
                        braceCount++;
                    } else if (content.charAt(end) == '}') {
                        braceCount--;
                    }
                    end++;
                }
                String shapeJson = content.substring(index, end);
                ShapeState state = deserializeShape(shapeJson);
                if (state != null && state.getShape() != null) {
                    map.put(state.getShape().getShapeId(), state);
                }
                index = end;
            } else {
                break;
            }
        }
        return map;
    }
}
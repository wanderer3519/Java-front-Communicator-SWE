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

    private ShapeSerializer() {}

    /**
     * Serializes a ShapeState to JSON.
     * UPDATED: Serializes Coordinates and Thickness as Integers to match .NET.
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

        // Points: Serialize X and Y as INTEGERS
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

        // Thickness: Serialize as INTEGER
        sb.append(JsonUtils.jsonEscape("Thickness")).append(":").append((int) shape.getThickness()).append(",");

        sb.append(JsonUtils.jsonEscape("CreatedBy")).append(":").append(JsonUtils.jsonEscape(shape.getCreatedBy())).append(",");
        sb.append(JsonUtils.jsonEscape("LastModifiedBy")).append(":").append(JsonUtils.jsonEscape(shape.getLastUpdatedBy())).append(",");
        sb.append(JsonUtils.jsonEscape("IsDeleted")).append(":").append(shapeState.isDeleted());

        sb.append("}");
        return sb.toString();
    }

    public static ShapeState deserializeShape(final String json) {
        if (json == null || json.isEmpty() || "null".equals(json)) {
            return null;
        }

        try {
            // Trim whitespace/newlines
            String content = json.trim();
            if (content.startsWith("{")) {
                content = content.substring(1, content.length() - 1);
            }

            // JsonUtils is now robust enough to handle the spaces in the .NET JSON
            final String shapeId = JsonUtils.extractString(content, "ShapeId");
            final String typeName = JsonUtils.extractString(content, "Type");
            final String colorHex = JsonUtils.extractString(content, "Color");
            final double thickness = JsonUtils.extractDouble(content, "Thickness");
            final String createdBy = JsonUtils.extractString(content, "CreatedBy");
            final String lastModifiedBy = JsonUtils.extractString(content, "LastModifiedBy");
            final boolean isDeleted = JsonUtils.extractBoolean(content, "IsDeleted");

            final List<Point> points = JsonUtils.extractPoints(content);

            if (shapeId == null || typeName == null || createdBy == null || lastModifiedBy == null || points == null) {
                // In a partial update scenario or if .NET omits certain nulls, we might check here.
                // But for a full shape restore, these are required.
                throw new SerializationException("Missing crucial shape field.");
            }

            final ShapeType shapeType = ShapeType.valueOf(typeName); // .NET sends "FREEHAND" (uppercase), which matches Java Enum.
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
            throw new SerializationException("Failed to deserialize ShapeState: " + e.getMessage(), e);
        }
    }

    // [Rest of class: deserializeAction, serializeAction, serializeShapesMap, etc. remain the same]
    // The serializeShapesMap method in the previous step is already correct,
    // it will use the updated serializeShape logic above.

    // ... [Insert Reference to existing serializeAction/deserializeAction/Map methods] ...

    // --- RE-INSERTING MAP LOGIC FOR COMPLETENESS ---
    public static String serializeShapesMap(final Map<ShapeId, ShapeState> shapes) {
        if (shapes == null || shapes.isEmpty()) return "{}";
        final StringBuilder sb = new StringBuilder();
        sb.append("{").append("\n");
        int i = 0;
        for (Map.Entry<ShapeId, ShapeState> entry : shapes.entrySet()) {
            sb.append("  ").append(JsonUtils.jsonEscape(entry.getKey().getValue()));
            sb.append(": ");
            final String shapeJson = serializeShape(entry.getValue());
            sb.append(shapeJson != null ? shapeJson : "null");
            if (i < shapes.size() - 1) sb.append(",");
            sb.append("\n");
            i++;
        }
        sb.append("}");
        return sb.toString();
    }

    public static Map<ShapeId, ShapeState> deserializeShapesMap(final String json) {
        final Map<ShapeId, ShapeState> map = new HashMap<>();
        if (json == null || json.trim().length() < 2) return map;

        String content = json.trim();
        if (content.startsWith("{")) content = content.substring(1);
        if (content.endsWith("}")) content = content.substring(0, content.length() - 1);
        content = content.trim();

        if (content.isEmpty()) return map;

        int index = 0;
        final int length = content.length();

        while (index < length) {
            // Robust key finding (skipping whitespace/newlines)
            while (index < length && (Character.isWhitespace(content.charAt(index)) || content.charAt(index) == ',')) {
                index++;
            }
            if (index >= length) break;
            if (content.charAt(index) != '"') break;

            int keyStart = index + 1;
            int keyEnd = content.indexOf('"', keyStart);
            if (keyEnd == -1) break;
            // String key = content.substring(keyStart, keyEnd); // We don't actually need the key string since it's inside the shape object too.

            // Find colon
            index = keyEnd + 1;
            while (index < length && (Character.isWhitespace(content.charAt(index)) || content.charAt(index) == ':')) {
                // Advance past colon and whitespace
                if (content.charAt(index) == ':') {
                    // Ensure we move past it
                }
                index++;
            }

            // Now at value start. Check for brace.
            int valueStart = index - 1; // Adjust index logic slightly or reuse extractNestedJson logic

            // Safer approach: Use extractNestedJson logic to find the balanced object from current position
            if (index < length && content.charAt(index-1) != '{') {
                // We consumed whitespace, step back to check if we are at '{'
                // The loop above is a bit aggressive. Let's rely on brace counting from current index.
                // Reset to find the first '{'
                while(index < length && content.charAt(index) != '{') index++;
            }

            if (index < length && content.charAt(index) == '{') {
                int braceCount = 1;
                int end = index + 1;
                while (end < length && braceCount > 0) {
                    if (content.charAt(end) == '{') braceCount++;
                    else if (content.charAt(end) == '}') braceCount--;
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

    public static String serializeAction(final Action action) {
        if (action == null) return "null";
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append(JsonUtils.jsonEscape("ActionType")).append(":").append(JsonUtils.jsonEscape(action.getActionType().toString())).append(",");
        sb.append(JsonUtils.jsonEscape("ActionId")).append(":").append(JsonUtils.jsonEscape(action.getActionId())).append(",");
        sb.append(JsonUtils.jsonEscape("ShapeId")).append(":").append(JsonUtils.jsonEscape(action.getShapeId().getValue())).append(",");
        sb.append(JsonUtils.jsonEscape("UserId")).append(":").append(JsonUtils.jsonEscape(action.getUserId())).append(",");
        sb.append(JsonUtils.jsonEscape("Timestamp")).append(":").append(action.getTimestamp()).append(",");

        final String prevStateJson = serializeShape(action.getPrevState());
        sb.append(JsonUtils.jsonEscape("PrevState")).append(":").append(prevStateJson != null ? prevStateJson : "null").append(",");

        final String newStateJson = serializeShape(action.getNewState());
        sb.append(JsonUtils.jsonEscape("NewState")).append(":").append(newStateJson != null ? newStateJson : "null");

        sb.append("}");
        return sb.toString();
    }

    public static Action deserializeAction(final String json) {
        if (json == null || json.isEmpty() || "null".equals(json)) return null;
        try {
            final String content = json.trim().substring(1, json.trim().length() - 1);
            final String actionId = JsonUtils.extractString(content, "ActionId");
            final String shapeId = JsonUtils.extractString(content, "ShapeId");
            final String userId = JsonUtils.extractString(content, "UserId");
            final ActionType actionType = ActionType.valueOf(JsonUtils.extractString(content, "ActionType"));
            final long timestamp = JsonUtils.extractLong(content, "Timestamp");
            final String prevStateJson = JsonUtils.extractNestedJson(content, "PrevState");
            final String newStateJson = JsonUtils.extractNestedJson(content, "NewState");

            final ShapeState prevState = deserializeShape(prevStateJson);
            final ShapeState newState = deserializeShape(newStateJson);

            // ... (Factory logic same as before) ...
            if (actionId == null || shapeId == null || userId == null || actionType == null || newState == null) {
                throw new SerializationException("Missing action fields");
            }
            final ShapeId targetId = new ShapeId(shapeId);
            switch (actionType) {
                case CREATE: return new CreateShapeAction(actionId, userId, timestamp, targetId, newState);
                case MODIFY: return new ModifyShapeAction(actionId, userId, timestamp, targetId, prevState, newState);
                case DELETE: return new DeleteShapeAction(actionId, userId, timestamp, targetId, prevState, newState);
                case RESURRECT: return new ResurrectShapeAction(actionId, userId, timestamp, targetId, prevState, newState);
                default: throw new SerializationException("Unknown action type");
            }
        } catch (Exception e) {
            throw new SerializationException("Action deserialization failed", e);
        }
    }
}
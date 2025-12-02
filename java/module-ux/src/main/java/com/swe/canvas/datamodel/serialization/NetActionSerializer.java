/*
 * -----------------------------------------------------------------------------
 * File: NetActionSerializer.java
 * Owner: Gajjala Bhavani Shankar
 * Roll Number : 112201026
 * Module : Canvas
 *
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.serialization;

import com.swe.canvas.datamodel.action.Action;
import com.swe.canvas.datamodel.action.ActionType;
import com.swe.canvas.datamodel.action.CreateShapeAction;
import com.swe.canvas.datamodel.action.DeleteShapeAction;
import com.swe.canvas.datamodel.action.ModifyShapeAction;
import com.swe.canvas.datamodel.action.ResurrectShapeAction;
import com.swe.canvas.datamodel.canvas.ShapeState;
import com.swe.canvas.datamodel.shape.ShapeId;

/**
 * Serializes and deserializes Action objects for network transmission.
 */
public final class NetActionSerializer {

    private NetActionSerializer() {
        // Utility class
    }

    // =========================================================================
    // Action Serialization/Deserialization
    // =========================================================================

    /**
     * Manually serializes an Action object, including nested ShapeState objects.
     * Fields included: actionId, actionType, prevState, newState (and others from
     * the base class).
     *
     * @param action The action to serialize.
     * @return A JSON string.
     */
    public static String serializeAction(final Action action) {
        if (action == null) {
            return "null";
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("{");

        // 1. Core Metadata Fields
        sb.append(JsonUtils.jsonEscape("ActionId")).append(":")
                .append(JsonUtils.jsonEscape(action.getActionId())).append(",");
        sb.append(JsonUtils.jsonEscape("ActionType")).append(":")
                .append(JsonUtils.jsonEscape(action.getActionType().toString())).append(",");

        // 2. PrevState (nested ShapeState JSON)
        appendState(sb, "Prev", action.getPrevState());
        sb.append(",");

        // 3. NewState (nested ShapeState JSON)
        appendState(sb, "Next", action.getNewState());

        sb.append("}");
        return sb.toString();
    }

    /**
     * Helper to append a state object to the string builder.
     *
     * @param sb    The StringBuilder to append to.
     * @param key   The JSON key.
     * @param state The ShapeState object.
     */
    private static void appendState(final StringBuilder sb, final String key,
                                    final ShapeState state) {
        final String json = ShapeSerializer.serializeShape(state);
        sb.append(JsonUtils.jsonEscape(key)).append(":");
        if (json != null) {
            sb.append(json);
        } else {
            sb.append("null");
        }
    }

    /**
     * Manually deserializes a JSON string back into a concrete Action object.
     *
     * <p>IMPORTANT: This method uses Action subclass constructors for correct object creation.</p>
     *
     * @param json The JSON string to deserialize.
     * @return The concrete Action object.
     */
    public static Action deserializeAction(final String json) {
        if (json == null || json.isEmpty() || "null".equals(json)) {
            return null;
        }

        try {
            final String content = json.substring(1, json.length() - 1); // Remove outer { }

            // 1. Extract Core Metadata Fields
            final String actionId = JsonUtils.extractString(content, "ActionId");
            final String typeStr = JsonUtils.extractString(content, "ActionType");

            validateMetadata(actionId, typeStr);

            final ActionType actionType = ActionType.valueOf(typeStr);

            // 2. Extract nested ShapeState objects
            final ShapeState prevState = extractState(content, "Prev");
            final ShapeState newState = extractState(content, "Next");

            if (newState == null) {
                throw new SerializationException("Missing crucial action field: Next state.");
            }

            // 3. Factory logic
            return buildAction(actionType, actionId, prevState, newState);

        } catch (final Exception e) {
            throw new SerializationException("Failed to manually deserialize Action: "
                    + e.getMessage(), e);
        }
    }

    private static void validateMetadata(final String id, final String type) {
        if (id == null || type == null) {
            throw new SerializationException("Missing ActionId or ActionType during deserialization.");
        }
    }

    private static ShapeState extractState(final String content, final String key) {
        final String nestedJson = JsonUtils.extractNestedJson(content, key);
        return ShapeSerializer.deserializeShape(nestedJson);
    }

    private static Action buildAction(final ActionType type, final String id,
                                      final ShapeState prev, final ShapeState next) {
        final String userId = next.getShape().getLastUpdatedBy();
        final long timestamp = next.getLastModified();
        final ShapeId targetId = next.getShape().getShapeId();

        switch (type) {
            case CREATE:
                // CreateAction ignores prevState (should be null)
                return new CreateShapeAction(id, userId, timestamp, targetId, next);
            case MODIFY:
                return new ModifyShapeAction(id, userId, timestamp, targetId, prev, next);
            case DELETE:
                return new DeleteShapeAction(id, userId, timestamp, targetId, prev, next);
            case RESURRECT:
                return new ResurrectShapeAction(id, userId, timestamp, targetId, prev, next);
            default:
                throw new SerializationException("Unknown action type: " + type);
        }
    }
}
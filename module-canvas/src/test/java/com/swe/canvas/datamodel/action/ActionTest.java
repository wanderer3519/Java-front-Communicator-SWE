/*
 * -----------------------------------------------------------------------------
 * File: ActionTest.java
 * Location: module-canvas/src/test/java/com/swe/canvas/datamodel/action/ActionTest.java
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.action;

import com.swe.canvas.datamodel.canvas.ShapeState;
import com.swe.canvas.datamodel.shape.Shape;
import com.swe.canvas.datamodel.shape.ShapeId;
import com.swe.canvas.datamodel.shape.ShapeType;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ActionTest {

    /**
     * Concrete Stub for abstract Shape class.
     */
    static class TestShape extends Shape {
        private static final long serialVersionUID = 1L;

        public TestShape(ShapeId shapeId, String userId) {
            super(shapeId, ShapeType.FREEHAND, new ArrayList<>(), 1.0, Color.BLACK, userId, userId);
        }

        @Override
        public Shape copy() {
            return new TestShape(getShapeId(), getCreatedBy());
        }
    }

    /**
     * Concrete Stub for abstract Action class.
     */
    static class TestAction extends Action {
        private static final long serialVersionUID = 1L;

        public TestAction(String actionId, String userId, long timestamp, ActionType actionType,
                          ShapeId shapeId, ShapeState prevState, ShapeState newState) {
            super(actionId, userId, timestamp, actionType, shapeId, prevState, newState);
        }
    }

    private ShapeState createDummyState(String id) {
        // ID must be >= 8 chars to avoid StringIndexOutOfBounds in toString()
        TestShape shape = new TestShape(new ShapeId(id), "user-001");
        return new ShapeState(shape, false, System.currentTimeMillis());
    }

    @Test
    void testConstructorAndGetters_ValidInput() {
        String actionId = "action-001";
        String userId = "user-001";
        long timestamp = 123456789L;
        ActionType type = ActionType.MODIFY;
        ShapeId shapeId = new ShapeId("shape-001");
        ShapeState prevState = createDummyState("shape-001");
        ShapeState newState = createDummyState("shape-001");

        Action action = new TestAction(actionId, userId, timestamp, type, shapeId, prevState, newState);

        assertEquals(actionId, action.getActionId());
        assertEquals(userId, action.getUserId());
        assertEquals(timestamp, action.getTimestamp());
        assertEquals(type, action.getActionType());
        assertEquals(shapeId, action.getShapeId());
        assertEquals(prevState, action.getPrevState());
        assertEquals(newState, action.getNewState());
    }

    @Test
    void testConstructor_PrevStateCanBeNull() {
        ShapeState newState = createDummyState("shape-002");
        Action action = new TestAction("action-001", "user-001", 100L, ActionType.CREATE, new ShapeId("shape-002"), null, newState);

        assertNull(action.getPrevState());
        assertEquals(newState, action.getNewState());
    }

    @Test
    void testConstructor_Nulls_ThrowException() {
        ShapeState state = createDummyState("shape-001");
        ShapeId id = new ShapeId("shape-001");

        assertThrows(NullPointerException.class, () ->
                new TestAction(null, "user", 1L, ActionType.MODIFY, id, state, state));
        assertThrows(NullPointerException.class, () ->
                new TestAction("action", null, 1L, ActionType.MODIFY, id, state, state));
        assertThrows(NullPointerException.class, () ->
                new TestAction("action", "user", 1L, null, id, state, state));
        assertThrows(NullPointerException.class, () ->
                new TestAction("action", "user", 1L, ActionType.MODIFY, null, state, state));
        assertThrows(NullPointerException.class, () ->
                new TestAction("action", "user", 1L, ActionType.MODIFY, id, state, null));
    }

    @Test
    void testEqualsAndHashCode() {
        ShapeState state = createDummyState("shape-001");
        ShapeId id = new ShapeId("shape-001");

        Action a1 = new TestAction("action-id-1", "user-1", 1L, ActionType.MODIFY, id, state, state);
        Action a2 = new TestAction("action-id-1", "user-2", 2L, ActionType.DELETE, id, state, state); // Same Action ID
        Action a3 = new TestAction("action-id-2", "user-1", 1L, ActionType.MODIFY, id, state, state); // Different Action ID

        // Reflexive
        assertEquals(a1, a1);

        // Symmetric
        assertEquals(a1, a2);
        assertEquals(a2, a1);

        // Not Equal (Different ID)
        assertNotEquals(a1, a3);

        // Not Equal (Null) - Covers "o == null"
        assertNotEquals(a1, null);

        // Not Equal (Different Class) - Covers "getClass() != o.getClass()"
        assertNotEquals(a1, "I am a String, not an Action");

        // HashCode consistency
        assertEquals(a1.hashCode(), a2.hashCode());
        assertNotEquals(a1.hashCode(), a3.hashCode());
    }

    @Test
    void testToString() {
        ShapeState state = createDummyState("shape-001");
        Action action = new TestAction("123456789", "user-test", 1L, ActionType.MODIFY, new ShapeId("shape-12345"), state, state);

        String str = action.toString();
        assertTrue(str.contains("MODIFY"));
        assertTrue(str.contains("user-test"));
        assertTrue(str.contains("12345678")); // Checks substring logic
    }
}
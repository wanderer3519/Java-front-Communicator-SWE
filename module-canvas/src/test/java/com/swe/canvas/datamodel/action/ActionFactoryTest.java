package com.swe.canvas.datamodel.action;

import com.swe.canvas.datamodel.canvas.CanvasState;
import com.swe.canvas.datamodel.canvas.ShapeState;
import com.swe.canvas.datamodel.shape.Point;
import com.swe.canvas.datamodel.shape.Shape;
import com.swe.canvas.datamodel.shape.ShapeId;
import com.swe.canvas.datamodel.shape.ShapeType;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ActionFactoryTest {

    // Concrete shape stub with valid ID length (>= 8 chars)
    static class StubShape extends Shape {
        private static final long serialVersionUID = 1L;
        public StubShape(ShapeId id, Color c, double t, String user) {
            super(id, ShapeType.LINE, new ArrayList<>(Arrays.asList(new Point(0,0))), t, c, user, user);
        }
        @Override public Shape copy() {
            return new StubShape(getShapeId(), getColor(), getThickness(), getCreatedBy());
        }
    }

    // Concrete Action stub to utilize ActionType.UNKNOWN
    static class UnknownAction extends Action {
        private static final long serialVersionUID = 1L;
        public UnknownAction(String actionId, String userId, long timestamp, ShapeId shapeId, ShapeState prevState, ShapeState newState) {
            super(actionId, userId, timestamp, ActionType.UNKNOWN, shapeId, prevState, newState);
        }
    }

    // --- Standard Action Creation Tests ---

    @Test
    void testCreateCreateAction() {
        ActionFactory factory = new ActionFactory();
        StubShape shape = new StubShape(new ShapeId("shape-001"), Color.RED, 1.0, "user-001");

        Action action = factory.createCreateAction(shape, "user-001");

        assertTrue(action instanceof CreateShapeAction);
        assertEquals(ActionType.CREATE, action.getActionType());
        assertEquals("shape-001", action.getShapeId().getValue());
        assertNull(action.getPrevState());
        assertNotNull(action.getNewState());
        assertFalse(action.getNewState().isDeleted());
    }

    @Test
    void testCreateModifyAction() {
        ActionFactory factory = new ActionFactory();
        CanvasState canvas = new CanvasState();
        ShapeId id = new ShapeId("shape-001");

        // Prep canvas with existing shape
        StubShape original = new StubShape(id, Color.RED, 1.0, "user-001");
        canvas.applyState(id, new ShapeState(original, false, 100L));

        // Request modification
        StubShape modified = new StubShape(id, Color.BLUE, 5.0, "user-001");
        Action action = factory.createModifyAction(canvas, id, modified, "user-002");

        assertTrue(action instanceof ModifyShapeAction);
        assertEquals(ActionType.MODIFY, action.getActionType());

        // Verify changes
        assertEquals(Color.RED, action.getPrevState().getShape().getColor());
        assertEquals(Color.BLUE, action.getNewState().getShape().getColor());
        assertEquals("user-002", action.getNewState().getShape().getLastUpdatedBy());
    }

    @Test
    void testCreateModifyAction_ThrowsIfDeleted() {
        ActionFactory factory = new ActionFactory();
        CanvasState canvas = new CanvasState();
        ShapeId id = new ShapeId("shape-001");

        // Prep canvas with DELETED shape
        StubShape original = new StubShape(id, Color.RED, 1.0, "user-001");
        canvas.applyState(id, new ShapeState(original, true, 100L)); // isDeleted = true

        StubShape modified = new StubShape(id, Color.BLUE, 5.0, "user-001");

        Exception e = assertThrows(IllegalStateException.class, () ->
                factory.createModifyAction(canvas, id, modified, "user-002")
        );
        // This covers the right side of the OR: || prevState.isDeleted()
        assertTrue(e.getMessage().contains("deleted shape"));
    }

    @Test
    void testCreateModifyAction_ThrowsIfNonExistent() {
        ActionFactory factory = new ActionFactory();
        CanvasState canvas = new CanvasState(); // Empty canvas
        ShapeId id = new ShapeId("non-existent");
        StubShape modified = new StubShape(id, Color.BLUE, 5.0, "user-001");

        // This covers the left side of the OR: if (prevState == null ...
        Exception e = assertThrows(IllegalStateException.class, () ->
                factory.createModifyAction(canvas, id, modified, "user-002")
        );
        assertTrue(e.getMessage().contains("non-existent"));
    }

    @Test
    void testCreateDeleteAction() {
        ActionFactory factory = new ActionFactory();
        CanvasState canvas = new CanvasState();
        ShapeId id = new ShapeId("shape-001");

        // Prep canvas
        StubShape original = new StubShape(id, Color.RED, 1.0, "user-001");
        canvas.applyState(id, new ShapeState(original, false, 100L));

        Action action = factory.createDeleteAction(canvas, id, "user-002");

        assertTrue(action instanceof DeleteShapeAction);
        assertEquals(ActionType.DELETE, action.getActionType());
        assertFalse(action.getPrevState().isDeleted());
        assertTrue(action.getNewState().isDeleted());
    }

    @Test
    void testCreateDeleteAction_ThrowsIfAlreadyDeleted() {
        ActionFactory factory = new ActionFactory();
        CanvasState canvas = new CanvasState();
        ShapeId id = new ShapeId("shape-001");

        // Prep canvas with DELETED shape
        StubShape original = new StubShape(id, Color.RED, 1.0, "user-001");
        canvas.applyState(id, new ShapeState(original, true, 100L)); // Already deleted

        Exception e = assertThrows(IllegalStateException.class, () ->
                factory.createDeleteAction(canvas, id, "user-002")
        );
        // This covers the right side of the OR: || prevState.isDeleted()
        assertTrue(e.getMessage().contains("deleted shape"));
    }

    @Test
    void testCreateDeleteAction_ThrowsIfNonExistent() {
        ActionFactory factory = new ActionFactory();
        CanvasState canvas = new CanvasState(); // Empty canvas
        ShapeId id = new ShapeId("non-existent");

        // This covers the left side of the OR: if (prevState == null ...
        Exception e = assertThrows(IllegalStateException.class, () ->
                factory.createDeleteAction(canvas, id, "user-002")
        );
        assertTrue(e.getMessage().contains("non-existent"));
    }

    // --- Inverse Action Tests (Undo Logic) ---

    @Test
    void testCreateInverseAction_UndoCreate() {
        // Inverse of Create -> Delete
        ActionFactory factory = new ActionFactory();
        ShapeId id = new ShapeId("shape-001");
        ShapeState createdState = new ShapeState(new StubShape(id, Color.RED, 1.0, "u1"), false, 100L);

        // Original Action: Create
        Action original = new CreateShapeAction("act-001", "u1", 100L, id, createdState);

        // Create Inverse
        Action inverse = factory.createInverseAction(original, "u2");

        assertTrue(inverse instanceof DeleteShapeAction);
        assertEquals(ActionType.DELETE, inverse.getActionType());
        // The item that was created (original.newState) becomes the prevState of the delete
        assertEquals(createdState.getShape(), inverse.getPrevState().getShape());
        // The inverse's new state should be deleted
        assertTrue(inverse.getNewState().isDeleted());
    }

    @Test
    void testCreateInverseAction_UndoModify() {
        // Inverse of Modify -> Modify (Swap states)
        ActionFactory factory = new ActionFactory();
        ShapeId id = new ShapeId("shape-001");

        ShapeState stateA = new ShapeState(new StubShape(id, Color.RED, 1.0, "u1"), false, 100L);
        ShapeState stateB = new ShapeState(new StubShape(id, Color.BLUE, 2.0, "u1"), false, 200L);

        // Original Action: Modify A -> B
        Action original = new ModifyShapeAction("act-001", "u1", 200L, id, stateA, stateB);

        // Create Inverse
        Action inverse = factory.createInverseAction(original, "u2");

        assertTrue(inverse instanceof ModifyShapeAction);
        assertEquals(ActionType.MODIFY, inverse.getActionType());

        // Verify swapping: Inverse Prev should be State B, Inverse New should be State A
        assertEquals(Color.BLUE, inverse.getPrevState().getShape().getColor()); // Was B
        assertEquals(Color.RED, inverse.getNewState().getShape().getColor());   // Was A
    }

    @Test
    void testCreateInverseAction_UndoDelete() {
        // Inverse of Delete -> Resurrect
        ActionFactory factory = new ActionFactory();
        ShapeId id = new ShapeId("shape-001");

        ShapeState prev = new ShapeState(new StubShape(id, Color.RED, 1.0, "u1"), false, 100L);
        ShapeState next = new ShapeState(new StubShape(id, Color.RED, 1.0, "u1"), true, 200L);

        // Original Action: Delete
        Action original = new DeleteShapeAction("act-001", "u1", 200L, id, prev, next);

        // Create Inverse
        Action inverse = factory.createInverseAction(original, "u2");

        assertTrue(inverse instanceof ResurrectShapeAction);
        assertEquals(ActionType.RESURRECT, inverse.getActionType());
        assertTrue(inverse.getPrevState().isDeleted());
        assertFalse(inverse.getNewState().isDeleted());
        assertEquals("u2", inverse.getNewState().getShape().getLastUpdatedBy());
    }

    @Test
    void testCreateInverseAction_UndoResurrect() {
        // Inverse of Resurrect -> Delete
        ActionFactory factory = new ActionFactory();
        ShapeId id = new ShapeId("shape-001");

        ShapeState prev = new ShapeState(new StubShape(id, Color.RED, 1.0, "u1"), true, 100L); // Deleted
        ShapeState next = new ShapeState(new StubShape(id, Color.RED, 1.0, "u1"), false, 200L); // Resurrected

        // Original Action: Resurrect
        Action original = new ResurrectShapeAction("act-001", "u1", 200L, id, prev, next);

        // Create Inverse
        Action inverse = factory.createInverseAction(original, "u2");

        assertTrue(inverse instanceof DeleteShapeAction);
        assertEquals(ActionType.DELETE, inverse.getActionType());
        assertFalse(inverse.getPrevState().isDeleted()); // Was active (resurrected state)
        assertTrue(inverse.getNewState().isDeleted());   // Now deleted again
    }

    @Test
    void testCreateInverseAction_UnknownType() {
        // Tests the "Red Line" default case in the switch statement
        ActionFactory factory = new ActionFactory();
        ShapeId id = new ShapeId("shape-001");
        ShapeState state = new ShapeState(new StubShape(id, Color.RED, 1.0, "u1"), false, 100L);

        // Create an action with UNKNOWN type
        Action unknown = new UnknownAction("act-xxx", "u1", 100L, id, state, state);

        Exception e = assertThrows(IllegalArgumentException.class, () ->
                factory.createInverseAction(unknown, "u2")
        );
        assertTrue(e.getMessage().contains("Unknown action type"));
    }
}
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

    // Concrete shape stub
    static class StubShape extends Shape {
        public StubShape(ShapeId id, Color c, double t, String user) {
            super(id, ShapeType.LINE, new ArrayList<>(Arrays.asList(new Point(0,0))), t, c, user, user);
        }
        @Override public Shape copy() {
            return new StubShape(shapeId, color, thickness, createdBy);
        }
    }

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
    void testCreateInverseAction_UndoDelete() {
        ActionFactory factory = new ActionFactory();
        ShapeId id = new ShapeId("shape-001");

        ShapeState prev = new ShapeState(new StubShape(id, Color.RED, 1.0, "user-001"), false, 100L);
        ShapeState next = new ShapeState(new StubShape(id, Color.RED, 1.0, "user-001"), true, 200L);

        // Original Action: Delete
        Action original = new DeleteShapeAction("action-001", "user-001", 200L, id, prev, next);

        // Inverse: Resurrect
        Action inverse = factory.createInverseAction(original, "user-002");

        assertTrue(inverse instanceof ResurrectShapeAction);
        assertTrue(inverse.getPrevState().isDeleted()); // Inverse prev = Original next
        assertFalse(inverse.getNewState().isDeleted()); // Inverse next = Original prev
        assertEquals("user-002", inverse.getNewState().getShape().getLastUpdatedBy());
    }
}
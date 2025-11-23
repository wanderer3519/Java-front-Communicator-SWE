package com.swe.canvas.datamodel.action;

import com.swe.canvas.datamodel.canvas.ShapeState;
import com.swe.canvas.datamodel.shape.Shape;
import com.swe.canvas.datamodel.shape.ShapeId;
import com.swe.canvas.datamodel.shape.ShapeType;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class CreateShapeActionTest {

    static class StubShape extends Shape {
        public StubShape() {
            // ID must be >= 8 chars for toString() safety
            super(new ShapeId("shape-001"), ShapeType.LINE, new ArrayList<>(), 1, Color.RED, "user-001", "user-001");
        }
        @Override public Shape copy() { return this; }
    }

    @Test
    void testConstruction_Success() {
        ShapeState newState = new ShapeState(new StubShape(), false, 100L);
        CreateShapeAction action = new CreateShapeAction("action-001", "user-001", 100L, new ShapeId("shape-001"), newState);

        assertEquals(ActionType.CREATE, action.getActionType());
        assertNull(action.getPrevState());
        assertEquals(newState, action.getNewState());
    }

    @Test
    void testConstruction_ThrowsIfNewStateIsDeleted() {
        ShapeState deletedState = new ShapeState(new StubShape(), true, 100L); // isDeleted = true

        Exception e = assertThrows(IllegalArgumentException.class, () ->
                new CreateShapeAction("action-001", "user-001", 100L, new ShapeId("shape-001"), deletedState)
        );
        // Updated to match "cannot be 'deleted'" from source code
        assertTrue(e.getMessage().contains("cannot be 'deleted'"),
                "Expected message containing 'cannot be 'deleted'', but got: " + e.getMessage());
    }
}
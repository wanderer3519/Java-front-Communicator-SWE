package com.swe.canvas.datamodel.action;

import com.swe.canvas.datamodel.canvas.ShapeState;
import com.swe.canvas.datamodel.shape.Shape;
import com.swe.canvas.datamodel.shape.ShapeId;
import com.swe.canvas.datamodel.shape.ShapeType;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class DeleteShapeActionTest {

    static class StubShape extends Shape {
        public StubShape() {
            super(new ShapeId("shape-001"), ShapeType.TRIANGLE, new ArrayList<>(), 1, Color.GREEN, "user-001", "user-001");
        }
        @Override public Shape copy() { return this; }
    }

    @Test
    void testConstruction_Success() {
        ShapeState prev = new ShapeState(new StubShape(), false, 100L);
        ShapeState next = new ShapeState(new StubShape(), true, 200L); // Deleted

        DeleteShapeAction action = new DeleteShapeAction("action-001", "user-001", 200L, new ShapeId("shape-001"), prev, next);

        assertEquals(ActionType.DELETE, action.getActionType());
    }

    @Test
    void testConstruction_ThrowsIfPrevIsAlreadyDeleted() {
        ShapeState prev = new ShapeState(new StubShape(), true, 100L); // Already deleted
        ShapeState next = new ShapeState(new StubShape(), true, 200L);

        Exception e = assertThrows(IllegalArgumentException.class, () ->
                new DeleteShapeAction("action-001", "user-001", 200L, new ShapeId("shape-001"), prev, next)
        );
        assertTrue(e.getMessage().contains("must not be 'deleted'"));
    }

    @Test
    void testConstruction_ThrowsIfNewIsNotDeleted() {
        ShapeState prev = new ShapeState(new StubShape(), false, 100L);
        ShapeState next = new ShapeState(new StubShape(), false, 200L); // Not deleted

        Exception e = assertThrows(IllegalArgumentException.class, () ->
                new DeleteShapeAction("action-001", "user-001", 200L, new ShapeId("shape-001"), prev, next)
        );
        assertTrue(e.getMessage().contains("must be 'deleted'"));
    }
}
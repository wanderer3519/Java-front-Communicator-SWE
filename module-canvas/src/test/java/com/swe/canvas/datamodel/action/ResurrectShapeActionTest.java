package com.swe.canvas.datamodel.action;

import com.swe.canvas.datamodel.canvas.ShapeState;
import com.swe.canvas.datamodel.shape.Shape;
import com.swe.canvas.datamodel.shape.ShapeId;
import com.swe.canvas.datamodel.shape.ShapeType;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ResurrectShapeActionTest {

    static class StubShape extends Shape {
        public StubShape() {
            super(new ShapeId("shape-001"), ShapeType.ELLIPSE, new ArrayList<>(), 1, Color.YELLOW, "user-001", "user-001");
        }
        @Override public Shape copy() { return this; }
    }

    @Test
    void testConstruction_Success() {
        ShapeState prev = new ShapeState(new StubShape(), true, 100L); // Was deleted
        ShapeState next = new ShapeState(new StubShape(), false, 200L); // Now active

        ResurrectShapeAction action = new ResurrectShapeAction("action-001", "user-001", 200L, new ShapeId("shape-001"), prev, next);

        assertEquals(ActionType.RESURRECT, action.getActionType());
    }

    @Test
    void testConstruction_ThrowsIfPrevWasNotDeleted() {
        ShapeState prev = new ShapeState(new StubShape(), false, 100L);
        ShapeState next = new ShapeState(new StubShape(), false, 200L);

        Exception e = assertThrows(IllegalArgumentException.class, () ->
                new ResurrectShapeAction("action-001", "user-001", 200L, new ShapeId("shape-001"), prev, next)
        );
        assertTrue(e.getMessage().contains("must be 'deleted'"));
    }

    @Test
    void testConstruction_ThrowsIfNewIsStillDeleted() {
        ShapeState prev = new ShapeState(new StubShape(), true, 100L);
        ShapeState next = new ShapeState(new StubShape(), true, 200L);

        Exception e = assertThrows(IllegalArgumentException.class, () ->
                new ResurrectShapeAction("action-001", "user-001", 200L, new ShapeId("shape-001"), prev, next)
        );
        assertTrue(e.getMessage().contains("must not be 'deleted'"));
    }
}
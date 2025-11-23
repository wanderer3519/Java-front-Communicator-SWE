package com.swe.canvas.datamodel.action;

import com.swe.canvas.datamodel.canvas.ShapeState;
import com.swe.canvas.datamodel.shape.Shape;
import com.swe.canvas.datamodel.shape.ShapeId;
import com.swe.canvas.datamodel.shape.ShapeType;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ModifyShapeActionTest {

    static class StubShape extends Shape {
        public StubShape() {
            super(new ShapeId("shape-001"), ShapeType.RECTANGLE, new ArrayList<>(), 1, Color.BLUE, "user-001", "user-001");
        }
        @Override public Shape copy() { return this; }
    }

    @Test
    void testConstruction_Success() {
        ShapeState prev = new ShapeState(new StubShape(), false, 100L);
        ShapeState next = new ShapeState(new StubShape(), false, 200L);

        ModifyShapeAction action = new ModifyShapeAction("action-001", "user-001", 200L, new ShapeId("shape-001"), prev, next);

        assertEquals(ActionType.MODIFY, action.getActionType());
        assertEquals(prev, action.getPrevState());
        assertEquals(next, action.getNewState());
    }

    @Test
    void testConstruction_ThrowsIfPrevStateDeleted() {
        ShapeState prev = new ShapeState(new StubShape(), true, 100L); // Deleted
        ShapeState next = new ShapeState(new StubShape(), false, 200L);

        Exception e = assertThrows(IllegalArgumentException.class, () ->
                new ModifyShapeAction("action-001", "user-001", 200L, new ShapeId("shape-001"), prev, next)
        );
        assertTrue(e.getMessage().contains("cannot be performed on a deleted shape"));
    }

    @Test
    void testConstruction_ThrowsIfNewStateDeleted() {
        ShapeState prev = new ShapeState(new StubShape(), false, 100L);
        ShapeState next = new ShapeState(new StubShape(), true, 200L); // Deleted

        Exception e = assertThrows(IllegalArgumentException.class, () ->
                new ModifyShapeAction("action-001", "user-001", 200L, new ShapeId("shape-001"), prev, next)
        );
        assertTrue(e.getMessage().contains("cannot be performed on a deleted shape"));
    }
}
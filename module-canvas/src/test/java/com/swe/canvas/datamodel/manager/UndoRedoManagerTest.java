/*
 * -----------------------------------------------------------------------------
 * File: UndoRedoManagerTest.java
 * Owner: Gajjala Bhavani Shankar
 * Roll Number : 112201026
 * Module: Canvas
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.manager;

import com.swe.canvas.datamodel.action.Action;
import com.swe.canvas.datamodel.action.ActionType;
import com.swe.canvas.datamodel.canvas.ShapeState;
import com.swe.canvas.datamodel.shape.Point;
import com.swe.canvas.datamodel.shape.Shape;
import com.swe.canvas.datamodel.shape.ShapeId;
import com.swe.canvas.datamodel.shape.ShapeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class UndoRedoManagerTest {

    private UndoRedoManager manager;
    private Action stubAction1;
    private Action stubAction2;
    private Action stubAction3;

    static class TestShape extends Shape {
        private static final long serialVersionUID = 1L;

        public TestShape(ShapeId id) {
            super(id, ShapeType.FREEHAND, new ArrayList<>(Arrays.asList(new Point(0, 0))), 1, Color.BLACK, "u", "u");
        }

        @Override
        public Shape copy() {
            return new TestShape(getShapeId());
        }
    }

    static class TestAction extends Action {
        private static final long serialVersionUID = 1L;

        public TestAction(String actionId) {
            super(actionId, "user", 1L, ActionType.MODIFY, new ShapeId("s1"),
                    new ShapeState(new TestShape(new ShapeId("s1")), false, 1L),
                    new ShapeState(new TestShape(new ShapeId("s1")), false, 2L));
        }
    }

    @BeforeEach
    void setUp() {
        manager = new UndoRedoManager();
        stubAction1 = new TestAction("act-1");
        stubAction2 = new TestAction("act-2");
        stubAction3 = new TestAction("act-3");
    }

    @Test
    void testInitialState() {
        assertFalse(manager.canUndo());
        assertFalse(manager.canRedo());
        assertNull(manager.getActionToUndo());
        assertNull(manager.getActionToRedo());
    }

    @Test
    void testStandardPushUndoRedo() {
        manager.push(stubAction1);
        assertTrue(manager.canUndo());
        assertEquals(stubAction1, manager.getActionToUndo());

        manager.applyHostUndo();
        assertTrue(manager.canRedo());
        assertEquals(stubAction1, manager.getActionToRedo());

        manager.applyHostRedo();
        assertFalse(manager.canRedo());
    }

    @Test
    void testPushClearsRedoHistory() {
        manager.push(stubAction1);
        manager.applyHostUndo();
        manager.push(stubAction2);

        assertEquals(stubAction2, manager.getActionToUndo());
        assertFalse(manager.canRedo());
    }

    @Test
    void testClear() {
        manager.push(stubAction1);
        manager.clear();
        assertFalse(manager.canUndo());
    }

    @Test
    void testBoundarySafeGuards() {
        manager.applyHostUndo(); // Empty
        manager.push(stubAction1);
        manager.applyHostRedo(); // Full
        assertTrue(manager.canUndo());
    }
}
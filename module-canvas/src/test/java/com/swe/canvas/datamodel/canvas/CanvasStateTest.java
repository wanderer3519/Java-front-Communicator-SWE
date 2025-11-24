/*
 * -----------------------------------------------------------------------------
 * File: CanvasStateTest.java
 * Location: module-canvas/src/test/java/com/swe/canvas/datamodel/canvas/CanvasStateTest.java
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.canvas;

import com.swe.canvas.datamodel.shape.Point;
import com.swe.canvas.datamodel.shape.Shape;
import com.swe.canvas.datamodel.shape.ShapeId;
import com.swe.canvas.datamodel.shape.ShapeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class CanvasStateTest {

    private CanvasState canvasState;

    // Stub for Shape
    static class TestShape extends Shape {
        private static final long serialVersionUID = 1L;
        public TestShape(ShapeId id) {
            super(id, ShapeType.LINE, new ArrayList<>(Arrays.asList(new Point(0,0))), 1.0, Color.BLACK, "u1", "u1");
        }
        @Override public Shape copy() { return this; }
    }

    @BeforeEach
    void setUp() {
        canvasState = new CanvasState();
    }

    @Test
    void testApplyAndGetState() {
        ShapeId id = new ShapeId("shape-1");
        ShapeState state = new ShapeState(new TestShape(id), false, 100L);

        canvasState.applyState(id, state);

        ShapeState retrieved = canvasState.getShapeState(id);
        assertEquals(state, retrieved);
    }

    @Test
    void testApplyState_ThrowsOnNulls() {
        ShapeId id = new ShapeId("s1");
        ShapeState state = new ShapeState(new TestShape(id), false, 100L);

        assertThrows(NullPointerException.class, () -> canvasState.applyState(null, state));
        assertThrows(NullPointerException.class, () -> canvasState.applyState(id, null));
    }

    @Test
    void testGetVisibleShapes_FiltersDeleted() {
        ShapeId id1 = new ShapeId("s1");
        ShapeId id2 = new ShapeId("s2");

        ShapeState active = new ShapeState(new TestShape(id1), false, 100L);
        ShapeState deleted = new ShapeState(new TestShape(id2), true, 100L);

        canvasState.applyState(id1, active);
        canvasState.applyState(id2, deleted);

        Collection<Shape> visible = canvasState.getVisibleShapes();

        assertEquals(1, visible.size());
        assertTrue(visible.contains(active.getShape()));
        assertFalse(visible.contains(deleted.getShape()));
    }

    @Test
    void testOnUpdateCallback() {
        AtomicBoolean callbackFired = new AtomicBoolean(false);

        // Set callback
        canvasState.setOnUpdate(() -> callbackFired.set(true));

        // Trigger manual notification (this is often called by ActionManager)
        canvasState.notifyUpdate();

        assertTrue(callbackFired.get());
    }

    @Test
    void testSetOnUpdate_HandlesNullSafe() {
        // Should not throw exception if we set null, defaults to no-op
        canvasState.setOnUpdate(null);
        assertDoesNotThrow(() -> canvasState.notifyUpdate());
    }

    @Test
    void testGetAllStates_ReturnsReadOnlyMap() {
        ShapeId id = new ShapeId("s1");
        canvasState.applyState(id, new ShapeState(new TestShape(id), false, 100L));

        Map<ShapeId, ShapeState> allStates = canvasState.getAllStates();

        assertEquals(1, allStates.size());
        // Verify unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> allStates.clear());
    }

    @Test
    void testSetAllStates() {
        // Prepare a new map state
        Map<ShapeId, ShapeState> newMap = new HashMap<>();
        ShapeId id = new ShapeId("restored-1");
        newMap.put(id, new ShapeState(new TestShape(id), false, 500L));

        // Trigger restore
        canvasState.setAllStates(newMap);

        assertEquals(1, canvasState.getAllStates().size());
        assertNotNull(canvasState.getShapeState(id));
        assertEquals(500L, canvasState.getShapeState(id).getLastModified());
    }

    @Test
    void testSetAllStates_NullClearsCanvas() {
        canvasState.applyState(new ShapeId("s1"), new ShapeState(new TestShape(new ShapeId("s1")), false, 1L));
        assertEquals(1, canvasState.getAllStates().size());

        canvasState.setAllStates(null);

        assertEquals(0, canvasState.getAllStates().size());
    }

    @Test
    void testClear() {
        canvasState.applyState(new ShapeId("s1"), new ShapeState(new TestShape(new ShapeId("s1")), false, 1L));

        canvasState.clear();

        assertEquals(0, canvasState.getAllStates().size());
    }
}
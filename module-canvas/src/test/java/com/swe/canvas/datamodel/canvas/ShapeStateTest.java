/*
 * -----------------------------------------------------------------------------
 * File: ShapeStateTest.java
 * Location: module-canvas/src/test/java/com/swe/canvas/datamodel/canvas/ShapeStateTest.java
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.canvas;

import com.swe.canvas.datamodel.shape.Point;
import com.swe.canvas.datamodel.shape.Shape;
import com.swe.canvas.datamodel.shape.ShapeId;
import com.swe.canvas.datamodel.shape.ShapeType;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ShapeStateTest {

    // Stub for Shape to allow testing without concrete implementations
    static class TestShape extends Shape {
        private static final long serialVersionUID = 1L;

        public TestShape(ShapeId shapeId, String userId) {
            super(shapeId, ShapeType.FREEHAND, new ArrayList<>(Arrays.asList(new Point(0, 0))), 1.0, Color.BLACK, userId, userId);
        }

        public TestShape(ShapeId shapeId, Color color, String userId) {
            super(shapeId, ShapeType.FREEHAND, new ArrayList<>(Arrays.asList(new Point(0, 0))), 1.0, color, userId, userId);
        }

        @Override
        public Shape copy() {
            // FIX: Use public getters instead of private fields
            return new TestShape(getShapeId(), getColor(), getCreatedBy());
        }
    }

    @Test
    void testConstructorAndGetters() {
        ShapeId id = new ShapeId("shape-1");
        TestShape shape = new TestShape(id, "user-1");
        long timestamp = 123456789L;

        ShapeState state = new ShapeState(shape, true, timestamp);

        assertEquals(shape, state.getShape());
        assertEquals(id, state.getShapeId());
        assertTrue(state.isDeleted());
        assertEquals(timestamp, state.getLastModified());
    }

    @Test
    void testGetShapeId_ReturnsNullIfShapeIsNull() {
        // Creating a state with null shape is technically possible via constructor if not null-checked there
        // But ShapeState implementation doesn't strict-check constructor, so we can test getShapeId null safety
        ShapeState state = new ShapeState(null, false, 100L);
        assertNull(state.getShapeId());
    }

    @Test
    void testCopy_DeepCopyVerification() {
        TestShape originalShape = new TestShape(new ShapeId("s1"), Color.RED, "u1");
        ShapeState originalState = new ShapeState(originalShape, false, 100L);

        // Act: Create a copy
        ShapeState copyState = originalState.copy();

        // Assert: Not same instance, but content equal
        assertNotSame(originalState, copyState);
        assertEquals(originalState, copyState);

        // Assert: Modifying copy's shape doesn't affect original
        copyState.getShape().setColor(Color.BLUE);
        assertEquals(Color.RED, originalState.getShape().getColor());
    }

    @Test
    void testToString() {
        TestShape shape = new TestShape(new ShapeId("s1"), "u1");
        ShapeState state = new ShapeState(shape, true, 999L);

        String result = state.toString();

        assertTrue(result.contains("ShapeState"));
        assertTrue(result.contains("deleted=true"));
        assertTrue(result.contains("modified=999"));
    }

    @Test
    void testEqualsAndHashCode() {
        TestShape s1 = new TestShape(new ShapeId("id1"), "u1");
        ShapeState state1 = new ShapeState(s1, false, 100L);
        ShapeState state2 = new ShapeState(s1, false, 100L); // Identical to state1
        ShapeState state3 = new ShapeState(s1, true, 100L);  // Different deleted status
        ShapeState state4 = new ShapeState(s1, false, 200L); // Different timestamp

        TestShape s2 = new TestShape(new ShapeId("id2"), "u1");
        ShapeState state5 = new ShapeState(s2, false, 100L); // Different shape

        // Reflexive
        assertEquals(state1, state1);

        // Symmetric
        assertEquals(state1, state2);
        assertEquals(state2, state1);

        // Not Equal fields
        assertNotEquals(state1, state3);
        assertNotEquals(state1, state4);
        assertNotEquals(state1, state5);

        // Not Equal types/null
        assertNotEquals(state1, null);
        assertNotEquals(state1, "Some String");

        // HashCode consistency
        assertEquals(state1.hashCode(), state2.hashCode());
        assertNotEquals(state1.hashCode(), state3.hashCode());
    }
}
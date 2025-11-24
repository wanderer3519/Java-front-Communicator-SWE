/*
 * -----------------------------------------------------------------------------
 * File: TriangleShapeTest.java
 * Owner: Gajjala Bhavani Shankar
 * Roll Number : 112201026
 * Module : Canvas
 *
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.shape;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link TriangleShape} class.
 *
 * <p>Verifies that the triangle enforces the bounding-box constraint (2 points)
 * and that the copy operation preserves data integrity.</p>
 *
 * @author Gajjala Bhavani Shankar
 */
class TriangleShapeTest {

    private static final double THICKNESS = 4.0;
    private static final double BOX_MIN = 10.0;
    private static final double BOX_MAX = 50.0;

    /**
     * Tests the successful creation of a TriangleShape with a valid bounding box.
     */
    @Test
    void testValidConstruction() {
        final ShapeId id = new ShapeId("tri-1");
        final List<Point> bounds = Arrays.asList(new Point(0, 0), new Point(BOX_MAX, BOX_MAX));
        
        final TriangleShape triangle = new TriangleShape(id, bounds, THICKNESS, Color.CYAN, "user1", "user1");

        Assertions.assertEquals(ShapeType.TRIANGLE, triangle.getShapeType(),
            "Shape type should be TRIANGLE");
        Assertions.assertEquals(2, triangle.getPoints().size(),
            "Should store exactly 2 points for the bounding box");
        Assertions.assertEquals(THICKNESS, triangle.getThickness());
    }

    /**
     * Tests that the constructor throws an exception for invalid point counts.
     */
    @Test
    void testInvalidPointCount() {
        final ShapeId id = new ShapeId("bad-tri");
        final Color col = Color.RED;
        final String user = "user";

        // Case 1: One point
        final List<Point> onePoint = Collections.singletonList(new Point(BOX_MIN, BOX_MIN));
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            new TriangleShape(id, onePoint, THICKNESS, col, user, user),
            "Should throw exception for 1 point"
        );

        // Case 2: Three points
        final List<Point> threePoints = Arrays.asList(
            new Point(0, 0), new Point(BOX_MIN, BOX_MIN), new Point(BOX_MAX, BOX_MAX)
        );
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            new TriangleShape(id, threePoints, THICKNESS, col, user, user),
            "Should throw exception for 3 points"
        );

        // Case 3: Empty list
        final List<Point> noPoints = new ArrayList<>();
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            new TriangleShape(id, noPoints, THICKNESS, col, user, user),
            "Should throw exception for 0 points"
        );
    }

    /**
     * Tests the deep copy functionality.
     */
    @Test
    void testCopy() {
        final ShapeId id = new ShapeId("tri-original");
        final List<Point> bounds = new ArrayList<>();
        bounds.add(new Point(BOX_MIN, BOX_MIN));
        bounds.add(new Point(BOX_MAX, BOX_MAX));

        final TriangleShape original = new TriangleShape(id, bounds, THICKNESS, Color.YELLOW, "creator", "editor");
        final Shape copy = original.copy();

        // 1. Verify different memory references
        Assertions.assertNotSame(original, copy, "Copy must create a new object instance");

        // 2. Verify logical equality
        Assertions.assertEquals(original, copy, "Copy must be logically equal to original");

        // 3. Verify deep copy of the points list
        Assertions.assertNotSame(original.getPoints(), copy.getPoints(),
            "The points list must be a new ArrayList instance");
            
        // 4. Verify correct type
        Assertions.assertInstanceOf(TriangleShape.class, copy, "Copy must return a TriangleShape");
    }
}
/*
 * -----------------------------------------------------------------------------
 * File: RectangleShapeTest.java
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
 * Unit tests for the {@link RectangleShape} class.
 *
 * <p>Verifies that the rectangle adheres to its geometric constraints (2 points)
 * and that the copy mechanism produces a correct independent clone.</p>
 *
 * @author Gajjala Bhavani Shankar
 */
class RectangleShapeTest {

    private static final double THICKNESS = 5.0;
    private static final double X1 = 10.0;
    private static final double Y1 = 20.0;
    private static final double X2 = 50.0;
    private static final double Y2 = 60.0;

    /**
     * Tests the successful creation of a RectangleShape.
     */
    @Test
    void testValidConstruction() {
        final ShapeId id = new ShapeId("rect-1");
        final List<Point> corners = Arrays.asList(new Point(X1, Y1), new Point(X2, Y2));
        final RectangleShape rect = new RectangleShape(id, corners, THICKNESS, Color.BLUE, "user1", "user1");

        Assertions.assertEquals(ShapeType.RECTANGLE, rect.getShapeType(),
            "Shape type should be RECTANGLE");
        Assertions.assertEquals(2, rect.getPoints().size(),
            "Should store exactly 2 points");
    }

    /**
     * Tests that the constructor throws an exception if the number of points is incorrect.
     */
    @Test
    void testInvalidPointCount() {
        final ShapeId id = new ShapeId("bad-rect");
        final Color col = Color.RED;
        final String user = "user";

        // Case 1: Only 1 point
        final List<Point> onePoint = Collections.singletonList(new Point(0, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            new RectangleShape(id, onePoint, THICKNESS, col, user, user),
            "Should throw exception for 1 point"
        );

        // Case 2: 3 points
        final List<Point> threePoints = Arrays.asList(
            new Point(0, 0), new Point(1, 1), new Point(2, 2)
        );
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            new RectangleShape(id, threePoints, THICKNESS, col, user, user),
            "Should throw exception for 3 points"
        );
        
        // Case 3: Empty list
        final List<Point> noPoints = new ArrayList<>();
        Assertions.assertThrows(IllegalArgumentException.class, () -> 
            new RectangleShape(id, noPoints, THICKNESS, col, user, user),
            "Should throw exception for 0 points"
        );
    }

    /**
     * Tests the copy functionality.
     */
    @Test
    void testCopy() {
        final ShapeId id = new ShapeId("rect-original");
        final List<Point> corners = new ArrayList<>();
        corners.add(new Point(X1, Y1));
        corners.add(new Point(X2, Y2));

        final RectangleShape original = new RectangleShape(id, corners, THICKNESS, Color.GREEN, "creator", "editor");
        final Shape copy = original.copy();

        // 1. Verify it is a new instance
        Assertions.assertNotSame(original, copy, "Copy should create a new object reference");

        // 2. Verify content equality
        Assertions.assertEquals(original, copy, "Copy should be equal in value to original");

        // 3. Verify deep copy of list
        Assertions.assertNotSame(original.getPoints(), copy.getPoints(),
            "The points list should be a new ArrayList instance");
            
        // 4. Verify type preservation
        Assertions.assertInstanceOf(RectangleShape.class, copy, "Copy must return a RectangleShape");
    }
}
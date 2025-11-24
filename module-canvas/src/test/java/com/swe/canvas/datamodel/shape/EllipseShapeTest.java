/*
 * -----------------------------------------------------------------------------
 * File: EllipseShapeTest.java
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
 * Unit tests for the {@link EllipseShape} class.
 *
 * <p>These tests verify that the Ellipse correctly enforces the bounding box
 * concept (requires 2 points) and that cloning operations produce valid,
 * independent objects.</p>
 *
 * @author Gajjala Bhavani Shankar
 */
class EllipseShapeTest {

    private static final double THICKNESS = 3.5;
    private static final double WIDTH = 60.0;
    private static final double HEIGHT = 40.0;

    /**
     * Tests the successful creation of an EllipseShape with valid data.
     */
    @Test
    void testValidConstruction() {
        final ShapeId id = new ShapeId("ellipse-1");
        final List<Point> bounds = Arrays.asList(new Point(0, 0), new Point(WIDTH, HEIGHT));
        
        final EllipseShape ellipse = new EllipseShape(id, bounds, THICKNESS, Color.ORANGE, "user1", "user1");

        Assertions.assertEquals(ShapeType.ELLIPSE, ellipse.getShapeType(),
            "Shape type should be ELLIPSE");
        Assertions.assertEquals(2, ellipse.getPoints().size(),
            "Should store exactly 2 points for bounding box");
        Assertions.assertEquals(THICKNESS, ellipse.getThickness());
    }

    /**
     * Tests that the constructor throws an exception if the point count is invalid.
     */
    @Test
    void testInvalidPointCount() {
        final ShapeId id = new ShapeId("bad-ellipse");
        final Color col = Color.RED;
        final String user = "user";

        // Case 1: One point
        final List<Point> onePoint = Collections.singletonList(new Point(10, 10));
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            new EllipseShape(id, onePoint, THICKNESS, col, user, user),
            "Should throw exception for 1 point"
        );

        // Case 2: Three points
        final List<Point> threePoints = Arrays.asList(
            new Point(0, 0), new Point(10, 10), new Point(20, 20)
        );
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            new EllipseShape(id, threePoints, THICKNESS, col, user, user),
            "Should throw exception for 3 points"
        );

        // Case 3: Empty list
        final List<Point> noPoints = new ArrayList<>();
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            new EllipseShape(id, noPoints, THICKNESS, col, user, user),
            "Should throw exception for 0 points"
        );
    }

    /**
     * Tests the deep copy functionality.
     */
    @Test
    void testCopy() {
        final ShapeId id = new ShapeId("ellipse-original");
        final List<Point> bounds = new ArrayList<>();
        bounds.add(new Point(10, 10));
        bounds.add(new Point(WIDTH, HEIGHT));

        final EllipseShape original = new EllipseShape(id, bounds, THICKNESS, Color.PINK, "creator", "editor");
        final Shape copy = original.copy();

        // 1. Check reference inequality
        Assertions.assertNotSame(original, copy, "Copy should create a new object instance");

        // 2. Check content equality
        Assertions.assertEquals(original, copy, "Copy content should match original");

        // 3. Check list deep copy
        Assertions.assertNotSame(original.getPoints(), copy.getPoints(),
            "Points list should be a new ArrayList instance");
            
        // 4. Verify correct class type
        Assertions.assertInstanceOf(EllipseShape.class, copy, "Copy must return an EllipseShape");
    }
}
/*
 * -----------------------------------------------------------------------------
 * File: LineShapeTest.java
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
 * Unit tests for the {@link LineShape} class.
 *
 * <p>These tests verify that the LineShape correctly enforces its geometric
 * constraints (requiring exactly 2 points) and that object cloning works
 * as expected.</p>
 *
 * @author Gajjala Bhavani Shankar
 */
class LineShapeTest {

    /**
     * Tests the successful creation of a LineShape with valid data.
     */
    @Test
    void testValidConstruction() {
        final ShapeId id = new ShapeId("line-1");
        final List<Point> points = Arrays.asList(
            new Point(0, 0),
            new Point(100, 100)
        );
        
        final LineShape line = new LineShape(id, points, 5.0, Color.BLACK, "user1", "user1");

        Assertions.assertEquals(ShapeType.LINE, line.getShapeType(),
            "Shape type should be LINE");
        Assertions.assertEquals(2, line.getPoints().size(),
            "Should store exactly 2 points");
        Assertions.assertEquals(5.0, line.getThickness());
    }

    /**
     * Tests that the constructor throws an exception if the point count is invalid.
     * This ensures we catch logic errors where a line might be initialized incorrectly.
     */
    @Test
    void testInvalidPointCount() {
        final ShapeId id = new ShapeId("bad-line");
        final double thk = 1.0;
        final Color col = Color.RED;
        final String user = "user";

        // Case 1: Single point (invalid for a line)
        final List<Point> onePoint = Collections.singletonList(new Point(10, 10));
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            new LineShape(id, onePoint, thk, col, user, user),
            "Should throw exception for 1 point"
        );

        // Case 2: Three points (invalid for a simple line segment)
        final List<Point> threePoints = Arrays.asList(
            new Point(0, 0), new Point(10, 10), new Point(20, 20)
        );
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            new LineShape(id, threePoints, thk, col, user, user),
            "Should throw exception for 3 points"
        );

        // Case 3: Empty list
        final List<Point> noPoints = new ArrayList<>();
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            new LineShape(id, noPoints, thk, col, user, user),
            "Should throw exception for 0 points"
        );
    }

    /**
     * Tests the deep copy functionality.
     */
    @Test
    void testCopy() {
        final ShapeId id = new ShapeId("line-original");
        final List<Point> points = new ArrayList<>();
        points.add(new Point(10, 10));
        points.add(new Point(20, 20));

        final LineShape original = new LineShape(id, points, 3.0, Color.BLUE, "creator", "editor");
        final Shape copy = original.copy();

        // 1. Check reference inequality (new object)
        Assertions.assertNotSame(original, copy, "Copy should create a new object instance");

        // 2. Check content equality
        Assertions.assertEquals(original, copy, "Copy content should match original");

        // 3. Check list deep copy
        Assertions.assertNotSame(original.getPoints(), copy.getPoints(),
            "Points list should be a new ArrayList instance");
            
        // 4. Verify correct class type
        Assertions.assertInstanceOf(LineShape.class, copy, "Copy must return a LineShape");
    }
}
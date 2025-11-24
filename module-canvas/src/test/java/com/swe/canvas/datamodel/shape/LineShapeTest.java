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

    private static final double THICKNESS = 3.0;
    private static final double START_COORD = 10.0;
    private static final double END_COORD = 100.0;

    /**
     * Tests the successful creation of a LineShape with valid data.
     */
    @Test
    void testValidConstruction() {
        final ShapeId id = new ShapeId("line-1");
        final List<Point> points = Arrays.asList(
            new Point(0, 0),
            new Point(END_COORD, END_COORD)
        );
        
        final LineShape line = new LineShape(id, points, THICKNESS, Color.BLACK, "user1", "user1");

        Assertions.assertEquals(ShapeType.LINE, line.getShapeType(),
            "Shape type should be LINE");
        Assertions.assertEquals(2, line.getPoints().size(),
            "Should store exactly 2 points");
        Assertions.assertEquals(THICKNESS, line.getThickness());
    }

    /**
     * Tests that the constructor throws an exception if the point count is invalid.
     */
    @Test
    void testInvalidPointCount() {
        final ShapeId id = new ShapeId("bad-line");
        final Color col = Color.RED;
        final String user = "user";

        // Case 1: Single point
        final List<Point> onePoint = Collections.singletonList(new Point(START_COORD, START_COORD));
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            new LineShape(id, onePoint, THICKNESS, col, user, user),
            "Should throw exception for 1 point"
        );

        // Case 2: Three points
        final List<Point> threePoints = Arrays.asList(
            new Point(0, 0), new Point(START_COORD, START_COORD), new Point(END_COORD, END_COORD)
        );
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            new LineShape(id, threePoints, THICKNESS, col, user, user),
            "Should throw exception for 3 points"
        );

        // Case 3: Empty list
        final List<Point> noPoints = new ArrayList<>();
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            new LineShape(id, noPoints, THICKNESS, col, user, user),
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
        points.add(new Point(START_COORD, START_COORD));
        points.add(new Point(END_COORD, END_COORD));

        final LineShape original = new LineShape(id, points, THICKNESS, Color.BLUE, "creator", "editor");
        final Shape copy = original.copy();

        // 1. Check reference inequality
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
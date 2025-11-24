/*
 * -----------------------------------------------------------------------------
 * File: FreehandShapeTest.java
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
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link FreehandShape} class.
 *
 * <p>Verifies that freehand shapes correctly handle variable numbers of points
 * and that the copy mechanism isolates the data of the new instance.</p>
 *
 * @author Gajjala Bhavani Shankar
 */
class FreehandShapeTest {

    private static final double TEST_THICKNESS = 2.0;
    private static final double TEST_COORD_1 = 10.0;
    private static final double TEST_COORD_2 = 20.0;
    private static final double TEST_COORD_3 = 30.0;

    /**
     * Tests the successful creation of a FreehandShape with multiple points.
     */
    @Test
    void testValidConstruction() {
        final ShapeId id = new ShapeId("free-1");
        final List<Point> stroke = Arrays.asList(
            new Point(0, 0),
            new Point(TEST_COORD_1, TEST_COORD_1),
            new Point(TEST_COORD_2, TEST_COORD_2)
        );
        
        final FreehandShape shape = new FreehandShape(id, stroke, TEST_THICKNESS, Color.BLACK, "user1", "user1");

        Assertions.assertEquals(ShapeType.FREEHAND, shape.getShapeType(),
            "Shape type should be FREEHAND");
        Assertions.assertEquals(3, shape.getPoints().size(),
            "Should store all points provided in the list");
        Assertions.assertEquals(stroke, shape.getPoints(),
            "Points list should match the input");
    }

    /**
     * Tests that a single point (a dot) is valid.
     */
    @Test
    void testSinglePointConstruction() {
        final ShapeId id = new ShapeId("dot-1");
        final List<Point> dot = Arrays.asList(new Point(TEST_COORD_1, TEST_COORD_1));
        
        final FreehandShape shape = new FreehandShape(id, dot, TEST_THICKNESS, Color.RED, "user", "user");
        
        Assertions.assertEquals(1, shape.getPoints().size(), "Single point freehand shape should be valid");
    }

    /**
     * Tests that creating a shape with an empty list throws an exception.
     */
    @Test
    void testEmptyListThrowsException() {
        final ShapeId id = new ShapeId("bad-free");
        final List<Point> emptyList = new ArrayList<>();
        final Color col = Color.BLUE;
        final String user = "user";

        Assertions.assertThrows(IllegalArgumentException.class, () ->
            new FreehandShape(id, emptyList, TEST_THICKNESS, col, user, user),
            "Constructor should throw exception if point list is empty"
        );
    }

    /**
     * Tests the deep copy functionality.
     */
    @Test
    void testCopy() {
        final ShapeId id = new ShapeId("free-original");
        final List<Point> stroke = new ArrayList<>();
        stroke.add(new Point(TEST_COORD_1, TEST_COORD_1));
        stroke.add(new Point(TEST_COORD_2, TEST_COORD_2));

        final FreehandShape original = new FreehandShape(id, stroke, TEST_THICKNESS, Color.MAGENTA, "creator", "updater");
        final Shape copy = original.copy();

        // 1. Verify distinct objects
        Assertions.assertNotSame(original, copy, "Copy should create a new object instance");

        // 2. Verify content equality
        Assertions.assertEquals(original, copy, "Copy content should match original");

        // 3. Verify deep copy of the list
        Assertions.assertNotSame(original.getPoints(), copy.getPoints(),
            "Points list should be a new ArrayList instance");
            
        // 4. Verify type correctness
        Assertions.assertInstanceOf(FreehandShape.class, copy, "Copy must return a FreehandShape");
    }
}
/*
 * -----------------------------------------------------------------------------
 * File: ShapeFactoryTest.java
 * Owner: Gajjala Bhavani Shankar
 * Roll Number : 112201026
 * Module : Canvas
 *
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.shape;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link ShapeFactory} class.
 *
 * <p>Verifies that the factory correctly instantiates the appropriate
 * concrete Shape classes and handles invalid or unknown inputs robustly.</p>
 *
 * @author Gajjala Bhavani Shankar
 */
class ShapeFactoryTest {

    private ShapeFactory factory;
    private ShapeId shapeId;
    private double thickness;
    private Color color;
    private String userId;

    @BeforeEach
    void setUp() {
        factory = new ShapeFactory();
        shapeId = new ShapeId("test-factory-id");
        thickness = 2.5;
        color = Color.RED;
        userId = "factory-tester";
    }

    /**
     * Helper method to generate valid points for testing.
     *
     * @param count Number of points needed.
     * @return List of points.
     */
    private List<Point> getPoints(final int count) {
        final Point[] pts = new Point[count];
        for (int i = 0; i < count; i++) {
            pts[i] = new Point(i * 10, i * 10);
        }
        return Arrays.asList(pts);
    }

    @Test
    void testCreateFreehand() {
        final List<Point> points = getPoints(3);
        final Shape shape = factory.createShape(
            ShapeType.FREEHAND, shapeId, points, thickness, color, userId
        );

        Assertions.assertInstanceOf(FreehandShape.class, shape,
            "Should return FreehandShape instance");
        Assertions.assertEquals(ShapeType.FREEHAND, shape.getShapeType());
        Assertions.assertEquals(shapeId, shape.getShapeId());
    }

    @Test
    void testCreateRectangle() {
        final List<Point> points = getPoints(2);
        final Shape shape = factory.createShape(
            ShapeType.RECTANGLE, shapeId, points, thickness, color, userId
        );

        Assertions.assertInstanceOf(RectangleShape.class, shape,
            "Should return RectangleShape instance");
        Assertions.assertEquals(ShapeType.RECTANGLE, shape.getShapeType());
    }

    @Test
    void testCreateEllipse() {
        final List<Point> points = getPoints(2);
        final Shape shape = factory.createShape(
            ShapeType.ELLIPSE, shapeId, points, thickness, color, userId
        );

        Assertions.assertInstanceOf(EllipseShape.class, shape,
            "Should return EllipseShape instance");
        Assertions.assertEquals(ShapeType.ELLIPSE, shape.getShapeType());
    }

    @Test
    void testCreateTriangle() {
        final List<Point> points = getPoints(2);
        final Shape shape = factory.createShape(
            ShapeType.TRIANGLE, shapeId, points, thickness, color, userId
        );

        Assertions.assertInstanceOf(TriangleShape.class, shape,
            "Should return TriangleShape instance");
        Assertions.assertEquals(ShapeType.TRIANGLE, shape.getShapeType());
    }

    @Test
    void testCreateLine() {
        final List<Point> points = getPoints(2);
        final Shape shape = factory.createShape(
            ShapeType.LINE, shapeId, points, thickness, color, userId
        );

        Assertions.assertInstanceOf(LineShape.class, shape,
            "Should return LineShape instance");
        Assertions.assertEquals(ShapeType.LINE, shape.getShapeType());
    }

    /**
     * Tests that passing a ShapeType that is not handled in the switch
     * (e.g., UNKNOWN) throws an IllegalArgumentException.
     *
     * <p>This test is crucial for 100% coverage as it hits the 'default' case
     * of the switch statement. It requires ShapeType.UNKNOWN to exist.</p>
     */
    @Test
    void testUnknownShapeType() {
        final List<Point> points = getPoints(2);
        
        // Verify that the factory throws exception for the UNKNOWN type
        // This ensures the 'default' branch of the switch is executed.
        final IllegalArgumentException exception = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> factory.createShape(
                ShapeType.UNKNOWN, shapeId, points, thickness, color, userId
            ),
            "Should throw IllegalArgumentException for UNKNOWN shape type"
        );

        Assertions.assertTrue(exception.getMessage().contains("Unknown or unsupported shape type"),
            "Exception message should contain descriptive text");
    }

    /**
     * Tests that passing null as the ShapeType throws a NullPointerException.
     *
     * <p>The switch statement implicitly throws NPE if the variable is null.
     * Testing this ensures robust handling behavior is verified.</p>
     */
    @Test
    void testNullShapeType() {
        final List<Point> points = getPoints(2);

        Assertions.assertThrows(NullPointerException.class, () -> {
            factory.createShape(null, shapeId, points, thickness, color, userId);
        }, "Switching on null should throw NullPointerException");
    }

    /**
     * Tests that invalid arguments (like null points) propagate exceptions
     * from the specific shape constructors.
     */
    @Test
    void testConstructorArgumentsPropagation() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            factory.createShape(ShapeType.LINE, shapeId, null, thickness, color, userId);
        }, "Should throw NPE because LineShape constructor checks for null points");
        
        final List<Point> emptyPoints = Collections.emptyList();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            factory.createShape(ShapeType.LINE, shapeId, emptyPoints, thickness, color, userId);
        }, "Should throw IllegalArgumentException because LineShape requires 2 points");
    }
}
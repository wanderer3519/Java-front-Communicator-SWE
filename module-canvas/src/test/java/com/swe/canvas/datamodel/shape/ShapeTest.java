/*
 * -----------------------------------------------------------------------------
 * File: ShapeTest.java
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the abstract {@link Shape} class.
 *
 * <p>This test class uses a concrete internal implementation to verify
 * the shared logic of all shapes. It is specifically designed to achieve
 * 100% branch coverage on the equals() method.</p>
 *
 * @author Gajjala Bhavani Shankar
 */
class ShapeTest {

    private ShapeId id1;
    private ShapeId id2;
    private ShapeType type1;
    private ShapeType type2;
    private List<Point> points1;
    private List<Point> points2;
    private Color color1;
    private Color color2;
    private double thickness1;
    private double thickness2;
    private String user1;
    private String user2;

    private Shape baseShape;

    /**
     * A concrete implementation of Shape used solely for testing.
     */
    private static class ConcreteShape extends Shape {
        ConcreteShape(final ShapeId id, final ShapeType type, final List<Point> points,
                      final double thickness, final Color color,
                      final String createdBy, final String updatedBy) {
            super(id, type, points, thickness, color, createdBy, updatedBy);
        }

        @Override
        public Shape copy() {
            return new ConcreteShape(shapeId, shapeType, new ArrayList<>(points),
                thickness, color, createdBy, lastUpdatedBy);
        }
    }

    @BeforeEach
    void setUp() {
        // Initialize two distinct values for every field to test inequality
        id1 = new ShapeId("id-111");
        id2 = new ShapeId("id-222");

        type1 = ShapeType.RECTANGLE;
        // Assuming ShapeType has at least two values. 
        // If RECTANGLE is the only one, use it, but coverage requires distinct checks.
        type2 = ShapeType.ELLIPSE;

        points1 = Arrays.asList(new Point(0, 0));
        points2 = Arrays.asList(new Point(10, 10));

        color1 = Color.RED;
        color2 = Color.BLUE;

        thickness1 = 5.0;
        thickness2 = 10.0;

        user1 = "User-A";
        user2 = "User-B";

        // Create the "Control" shape against which we compare everything
        baseShape = new ConcreteShape(id1, type1, points1, thickness1, color1, user1, user1);
    }

    /**
     * Tests the constructor and standard getters.
     */
    @Test
    void testConstructorAndGetters() {
        Assertions.assertEquals(id1, baseShape.getShapeId());
        Assertions.assertEquals(type1, baseShape.getShapeType());
        Assertions.assertEquals(points1, baseShape.getPoints());
        Assertions.assertEquals(thickness1, baseShape.getThickness());
        Assertions.assertEquals(color1, baseShape.getColor());
        Assertions.assertEquals(user1, baseShape.getCreatedBy());
        Assertions.assertEquals(user1, baseShape.getLastUpdatedBy());
    }

    /**
     * Tests translate logic (moving points).
     */
    @Test
    void testTranslate() {
        final double dx = 5.0;
        final double dy = 5.0;
        baseShape.translate(dx, dy);

        final Point p = baseShape.getPoints().get(0);
        Assertions.assertEquals(5.0, p.getX());
        Assertions.assertEquals(5.0, p.getY());
    }

    /**
     * Tests basic setters.
     */
    @Test
    void testSetters() {
        baseShape.setThickness(thickness2);
        Assertions.assertEquals(thickness2, baseShape.getThickness());

        baseShape.setColor(color2);
        Assertions.assertEquals(color2, baseShape.getColor());

        baseShape.setPoints(points2);
        Assertions.assertEquals(points2, baseShape.getPoints());

        baseShape.setLastUpdatedBy(user2);
        Assertions.assertEquals(user2, baseShape.getLastUpdatedBy());
    }

    /**
     * Tests copy functionality.
     */
    @Test
    void testCopy() {
        final Shape copy = baseShape.copy();
        Assertions.assertNotSame(baseShape, copy, "Copy should return a new instance");
        Assertions.assertEquals(baseShape, copy, "Copy should have identical values");
    }

    /**
     * <b>CRITICAL TEST FOR 100% COVERAGE</b>
     * <p>This method tests the equals() chain step-by-step.
     * Since equals() uses short-circuit AND (&&), we must fail
     * each condition individually to reach the subsequent lines.</p>
     */
    @Test
    void testEqualityBranchCoverage() {
        // 1. Reflexive Check (this == o)
        Assertions.assertEquals(baseShape, baseShape, "Should be equal to itself");

        // 2. Null Check
        Assertions.assertNotEquals(baseShape, null, "Should not be equal to null");

        // 3. Class/Type Check
        Assertions.assertNotEquals(baseShape, "A String", "Should not be equal to different class");

        // 4. Thickness Check (First in the && chain)
        final Shape diffThickness = new ConcreteShape(id1, type1, points1, thickness2, color1, user1, user1);
        Assertions.assertNotEquals(baseShape, diffThickness, "Should fail on different thickness");

        // 5. ShapeId Check (Second in chain)
        final Shape diffId = new ConcreteShape(id2, type1, points1, thickness1, color1, user1, user1);
        Assertions.assertNotEquals(baseShape, diffId, "Should fail on different ShapeId");

        // 6. Points Check (Third in chain)
        final Shape diffPoints = new ConcreteShape(id1, type1, points2, thickness1, color1, user1, user1);
        Assertions.assertNotEquals(baseShape, diffPoints, "Should fail on different Points");

        // 7. Color Check (Fourth in chain)
        final Shape diffColor = new ConcreteShape(id1, type1, points1, thickness1, color2, user1, user1);
        Assertions.assertNotEquals(baseShape, diffColor, "Should fail on different Color");

        // 8. CreatedBy Check (Fifth in chain)
        final Shape diffCreatedBy = new ConcreteShape(id1, type1, points1, thickness1, color1, user2, user1);
        Assertions.assertNotEquals(baseShape, diffCreatedBy, "Should fail on different CreatedBy");

        // 9. LastUpdatedBy Check (Sixth in chain)
        final Shape diffUpdatedBy = new ConcreteShape(id1, type1, points1, thickness1, color1, user1, user2);
        Assertions.assertNotEquals(baseShape, diffUpdatedBy, "Should fail on different LastUpdatedBy");

        // 10. ShapeType Check (Last in chain)
        final Shape diffType = new ConcreteShape(id1, type2, points1, thickness1, color1, user1, user1);
        Assertions.assertNotEquals(baseShape, diffType, "Should fail on different ShapeType");

        // 11. Exact Match (All pass)
        final Shape exactMatch = new ConcreteShape(id1, type1, points1, thickness1, color1, user1, user1);
        Assertions.assertEquals(baseShape, exactMatch, "Should pass when all fields match");
    }

    /**
     * Tests hashCode consistency.
     */
    @Test
    void testHashCode() {
        final Shape match = new ConcreteShape(id1, type1, points1, thickness1, color1, user1, user1);
        Assertions.assertEquals(baseShape.hashCode(), match.hashCode());
    }

    /**
     * Tests toString does not crash and contains key info.
     */
    @Test
    void testToString() {
        final String str = baseShape.toString();
        Assertions.assertNotNull(str);
        // Check that substring logic works safely even for short IDs
        final Shape shortIdShape = new ConcreteShape(new ShapeId("12"), type1, points1, thickness1, color1, user1, user1);
        Assertions.assertNotNull(shortIdShape.toString());
    }
}
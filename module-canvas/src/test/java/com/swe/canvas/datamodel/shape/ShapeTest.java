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
 * 100% branch and line coverage.</p>
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
            // NOTE: Must use getters here because fields in Shape are private
            return new ConcreteShape(
                getShapeId(),
                getShapeType(),
                new ArrayList<>(getPoints()),
                getThickness(),
                getColor(),
                getCreatedBy(),
                getLastUpdatedBy()
            );
        }
    }

    @BeforeEach
    void setUp() {
        // Initialize distinct values for coverage
        id1 = new ShapeId("id-111");
        id2 = new ShapeId("id-222");

        type1 = ShapeType.RECTANGLE;
        type2 = ShapeType.LINE;

        points1 = Arrays.asList(new Point(0, 0));
        points2 = Arrays.asList(new Point(10, 10));

        color1 = Color.RED;
        color2 = Color.BLUE;

        thickness1 = 5.0;
        thickness2 = 10.0;

        user1 = "User-A";
        user2 = "User-B";

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
     * Tests strict equality logic.
     */
    @Test
    void testEqualityBranchCoverage() {
        // 1. Reflexive Check
        Assertions.assertEquals(baseShape, baseShape, "Should be equal to itself");

        // 2. Null Check
        Assertions.assertNotEquals(baseShape, null, "Should not be equal to null");

        // 3. Class/Type Check
        Assertions.assertNotEquals(baseShape, "A String", "Should not be equal to different class");

        // 4. Thickness Check
        final Shape diffThickness = new ConcreteShape(id1, type1, points1, thickness2, color1, user1, user1);
        Assertions.assertNotEquals(baseShape, diffThickness, "Should fail on different thickness");

        // 5. ShapeId Check
        final Shape diffId = new ConcreteShape(id2, type1, points1, thickness1, color1, user1, user1);
        Assertions.assertNotEquals(baseShape, diffId, "Should fail on different ShapeId");

        // 6. Points Check
        final Shape diffPoints = new ConcreteShape(id1, type1, points2, thickness1, color1, user1, user1);
        Assertions.assertNotEquals(baseShape, diffPoints, "Should fail on different Points");

        // 7. Color Check
        final Shape diffColor = new ConcreteShape(id1, type1, points1, thickness1, color2, user1, user1);
        Assertions.assertNotEquals(baseShape, diffColor, "Should fail on different Color");

        // 8. CreatedBy Check
        final Shape diffCreatedBy = new ConcreteShape(id1, type1, points1, thickness1, color1, user2, user1);
        Assertions.assertNotEquals(baseShape, diffCreatedBy, "Should fail on different CreatedBy");

        // 9. LastUpdatedBy Check
        final Shape diffUpdatedBy = new ConcreteShape(id1, type1, points1, thickness1, color1, user1, user2);
        Assertions.assertNotEquals(baseShape, diffUpdatedBy, "Should fail on different LastUpdatedBy");

        // 10. ShapeType Check
        final Shape diffType = new ConcreteShape(id1, type2, points1, thickness1, color1, user1, user1);
        Assertions.assertNotEquals(baseShape, diffType, "Should fail on different ShapeType");

        // 11. Exact Match
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
     * Tests toString truncation logic.
     * <p>This ensures we hit the line: idVal = idVal.substring(0, ID_DISPLAY_LENGTH);</p>
     */
    @Test
    void testToString() {
        // Case 1: Short ID (length < 8)
        final Shape shortIdShape = new ConcreteShape(
            new ShapeId("12"), type1, points1, thickness1, color1, user1, user1
        );
        Assertions.assertTrue(shortIdShape.toString().contains("id=12,"),
            "Short IDs should be displayed in full");

        // Case 2: Long ID (length > 8)
        final String longString = "1234567890";
        final Shape longIdShape = new ConcreteShape(
            new ShapeId(longString), type1, points1, thickness1, color1, user1, user1
        );
        final String result = longIdShape.toString();
        
        Assertions.assertTrue(result.contains("id=12345678,"),
            "IDs longer than 8 chars should be truncated");
        Assertions.assertFalse(result.contains("90"),
            "Truncated ID should not contain the suffix");
    }
}
/*
 * -----------------------------------------------------------------------------
 * File: PointTest.java
 * Owner: Gajjala Bhavani Shankar
 * Roll Number : 112201026
 * Module : Canvas
 *
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.shape;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link Point} class.
 *
 * <p>
 * These tests verify that the Point class correctly stores coordinates,
 * implements equality checks for floating point numbers, and produces
 * consistent hash codes.
 * </p>
 *
 * @author Gajjala Bhavani Shankar
 */
class PointTest {

    /**
     * Tests that the constructor correctly initializes fields and getters
     * return the correct values.
     */
    @Test
    void testConstructorAndGetters() {
        final double xVal = 10.5;
        final double yVal = 20.7;

        final Point point = new Point(xVal, yVal);

        Assertions.assertEquals(xVal, point.getX(), 0.0001,
                "The getX() method should return the value passed to constructor.");
        Assertions.assertEquals(yVal, point.getY(), 0.0001,
                "The getY() method should return the value passed to constructor.");
    }

    /**
     * Tests equality logic.
     * 1. Reflexive (equals self)
     * 2. Symmetric (a equals b)
     * 3. Failure on different values
     * 4. Failure on null
     * 5. Failure on different types
     */
    @Test
    void testEquality() {
        final Point p1 = new Point(5.0, 5.0);
        final Point p2 = new Point(5.0, 5.0);
        final Point p3 = new Point(5.0, 6.0); // Different y
        final Point p4 = new Point(6.0, 5.0); // Different x

        // Reflexive
        Assertions.assertEquals(p1, p1, "A point must be equal to itself.");

        // Symmetric
        Assertions.assertEquals(p1, p2, "Points with same coordinates must be equal.");
        Assertions.assertEquals(p2, p1, "Equality must be symmetric.");

        // Not Equal
        Assertions.assertNotEquals(p1, p3, "Points with different Y should not be equal.");
        Assertions.assertNotEquals(p1, p4, "Points with different X should not be equal.");
        Assertions.assertNotEquals(p1, null, "Point should not be equal to null.");
        Assertions.assertNotEquals(p1, "String", "Point should not be equal to a String.");
    }

    /**
     * Tests that equal points have equal hash codes.
     */
    @Test
    void testHashCode() {
        final Point p1 = new Point(100.0, 200.0);
        final Point p2 = new Point(100.0, 200.0);

        Assertions.assertEquals(p1.hashCode(), p2.hashCode(),
                "Equal points must have identical hash codes.");

        final Point p3 = new Point(101.0, 200.0);
        // While hash collisions are possible, distinct points usually have distinct
        // hashes
        Assertions.assertNotEquals(p1.hashCode(), p3.hashCode());
    }

    /**
     * Tests the toString format.
     */
    @Test
    void testToString() {
        final Point p = new Point(1.5, 2.5);
        final String result = p.toString();

        Assertions.assertEquals("Point(1.5, 2.5)", result,
                "toString should match the expected format.");
    }
}
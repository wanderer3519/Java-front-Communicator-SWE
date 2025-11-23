/*
 * -----------------------------------------------------------------------------
 * File: ShapeIdTest.java
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
 * Unit tests for the {@link ShapeId} class.
 *
 * <p>
 * These tests ensure that the ShapeId wrapper behaves correctly regarding
 * equality, hashing, and null safety.
 * </p>
 *
 * @author Gajjala Bhavani Shankar
 */
class ShapeIdTest {

    /**
     * Tests that the constructor correctly stores the value when provided
     * with a valid string.
     */
    @Test
    void testConstructorAndGetValue() {
        final String rawId = "test-id-123";
        final ShapeId shapeId = new ShapeId(rawId);

        Assertions.assertEquals(rawId, shapeId.getValue(),
                "The getValue() method should return the string passed to the constructor.");
    }

    /**
     * Tests that the constructor throws a NullPointerException when
     * null is passed. This enforces the class invariant.
     */
    @Test
    void testConstructorThrowsOnNull() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            new ShapeId(null);
        }, "Constructor should throw NullPointerException for null input.");
    }

    /**
     * Tests the static factory method randomId().
     * It ensures it returns a non-null object with a non-empty ID.
     */
    @Test
    void testRandomIdGeneration() {
        final ShapeId id1 = ShapeId.randomId();
        final ShapeId id2 = ShapeId.randomId();

        Assertions.assertNotNull(id1, "randomId should not return null.");
        Assertions.assertNotNull(id1.getValue(), "Generated ID value should not be null.");

        // It is statistically impossible for two random UUIDs to be equal
        Assertions.assertNotEquals(id1, id2, "Two random IDs should not be equal.");
    }

    /**
     * Tests the equals() method for various scenarios (Reflexive, Symmetric,
     * Transitive).
     */
    @Test
    void testEquality() {
        final String val = "unique-id";
        final ShapeId id1 = new ShapeId(val);
        final ShapeId id2 = new ShapeId(val);
        final ShapeId id3 = new ShapeId("different-id");

        // 1. Reflexive: an object must equal itself
        Assertions.assertEquals(id1, id1, "An object must be equal to itself.");

        // 2. Symmetric: if a equals b, then b must equal a
        Assertions.assertEquals(id1, id2, "Objects with the same ID string should be equal.");
        Assertions.assertEquals(id2, id1, "Equality should be symmetric.");

        // 3. Not equal to different ID
        Assertions.assertNotEquals(id1, id3, "Objects with different ID strings should not be equal.");

        // 4. Not equal to null
        Assertions.assertNotEquals(id1, null, "Object should not be equal to null.");

        // 5. Not equal to a different type of object
        Assertions.assertNotEquals(id1, "some string", "ShapeId should not be equal to a String.");
    }

    /**
     * Tests that two equal objects produce the same hash code.
     * This is required for correct behavior in HashMaps.
     */
    @Test
    void testHashCode() {
        final String val = "hash-test";
        final ShapeId id1 = new ShapeId(val);
        final ShapeId id2 = new ShapeId(val);

        Assertions.assertEquals(id1.hashCode(), id2.hashCode(),
                "Equal objects must have the same hash code.");
    }

    /**
     * Tests the toString() method format.
     */
    @Test
    void testToString() {
        final String val = "my-id";
        final ShapeId shapeId = new ShapeId(val);

        Assertions.assertEquals("ShapeId(my-id)", shapeId.toString(),
                "toString should return the expected formatted string.");
    }
}
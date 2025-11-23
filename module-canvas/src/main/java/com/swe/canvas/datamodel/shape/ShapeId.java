/*
 * -----------------------------------------------------------------------------
 * File: ShapeId.java
 * Owner: Gajjala Bhavani Shankar
 * Roll Number : 112201026
 * Module : Canvas
 *
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.shape;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * A type-safe wrapper for a shape's unique identifier.
 *
 * <p>
 * This class encapsulates a String ID (typically a UUID) to prevent
 * accidental misuse of plain strings as identifiers. It acts as a strong
 * type to ensure we don't mix up shape IDs with other string data.
 * It is used as the key in the CanvasState map.
 * </p>
 *
 * <p>
 * <b>Thread Safety:</b> This class is immutable. Once created, the ID cannot
 * be changed, making it naturally thread-safe.
 * </p>
 *
 * @author Gajjala Bhavani Shankar
 */
public final class ShapeId implements Serializable {

    /**
     * Used for Java serialization version control.
     * Kept at 1L as the default version.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The underlying string representation of the identifier.
     */
    private final String id;

    /**
     * Constructs a ShapeId from a given string.
     *
     * <p>
     * We validate the input immediately to ensure a ShapeId is never created
     * in an invalid state (e.g., holding a null value).
     * </p>
     *
     * @param identifier The raw string ID.
     * @throws NullPointerException if the identifier is null.
     */
    public ShapeId(final String identifier) {
        // We use requireNonNull to fail fast if the caller passes bad data.
        this.id = Objects.requireNonNull(identifier, "ID cannot be null");
    }

    /**
     * Generates a new, random ShapeId using Java's UUID mechanism.
     *
     * <p>
     * This is a factory method used when creating a brand new shape
     * that doesn't have an identity yet.
     * </p>
     *
     * @return A new unique ShapeId.
     */
    public static ShapeId randomId() {
        return new ShapeId(UUID.randomUUID().toString());
    }

    /**
     * Gets the raw string value of the ID.
     *
     * @return The string ID.
     */
    public String getValue() {
        return id;
    }

    /**
     * Returns the string representation of this ShapeId.
     *
     * @return A string in the format "ShapeId(value)".
     */
    @Override
    public String toString() {
        return "ShapeId(" + id + ")";
    }

    /**
     * Compares this ShapeId to another object.
     *
     * <p>
     * Two ShapeIds are considered equal if they hold the exact same
     * underlying string identifier.
     * </p>
     *
     * @param obj The object to compare with.
     * @return True if the objects represent the same ID, false otherwise.
     */
    @Override
    public boolean equals(final Object obj) {
        // Performance optimization: check if it's the exact same memory reference
        if (this == obj) {
            return true;
        }

        // Check for null or if the classes don't match
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        // Cast and compare the actual string content
        final ShapeId shapeId = (ShapeId) obj;
        return id.equals(shapeId.id);
    }

    /**
     * Generates a hash code for this ShapeId.
     *
     * <p>
     * This delegates to the underlying string's hash code to ensure consistency
     * with the equals method, allowing this object to be used safely in HashMaps.
     * </p>
     *
     * @return The hash code value.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
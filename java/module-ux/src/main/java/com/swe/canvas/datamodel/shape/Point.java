/*
 * -----------------------------------------------------------------------------
 * File: Point.java
 * Owner: Gajjala Bhavani Shankar
 * Roll Number : 112201026
 * Module : Canvas
 *
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.shape;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents an immutable 2D coordinate (x, y).
 *
 * <p>This class serves as a fundamental building block for the geometry of shapes
 * within the canvas. By keeping it immutable, we ensure that passing points
 * around (like in Undo/Redo stacks) is perfectly safe and free of side effects.</p>
 *
 * <p><b>Thread Safety:</b> This class is completely immutable. Once a point is
 * defined, its coordinates cannot change, making it inherently thread-safe.</p>
 *
 * @author Gajjala Bhavani Shankar
 */
public final class Point implements Serializable {

    /**
     * Used for Java serialization version control.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The horizontal x-coordinate.
     */
    private final double x;

    /**
     * The vertical y-coordinate.
     */
    private final double y;

    /**
     * Constructs a new Point with specific coordinates.
     *
     * <p>Note: We use distinct parameter names here to avoid "HiddenField"
     * violations where parameters shadow the class fields.</p>
     *
     * @param xCoordinate The horizontal position.
     * @param yCoordinate The vertical position.
     */
    public Point(final double xCoordinate, final double yCoordinate) {
        this.x = xCoordinate;
        this.y = yCoordinate;
    }

    /**
     * Retrieves the x-coordinate.
     *
     * @return The horizontal component of this point.
     */
    public double getX() {
        return x;
    }

    /**
     * Retrieves the y-coordinate.
     *
     * @return The vertical component of this point.
     */
    public double getY() {
        return y;
    }

    /**
     * Returns a string representation of this point.
     *
     * @return A string formatted as "Point(x, y)".
     */
    @Override
    public String toString() {
        return "Point(" + x + ", " + y + ")";
    }

    /**
     * Compares this Point to another object for equality.
     *
     * <p>We use {@code Double.compare} instead of simple {@code ==} to correctly
     * handle edge cases like NaN (Not a Number) and distinct types of zeros,
     * ensuring robust geometric comparisons.</p>
     *
     * @param obj The object to compare with.
     * @return True if the other object is a Point with identical coordinates.
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        // Check if the object is null or belongs to a different class
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final Point point = (Point) obj;

        // Use Double.compare for precise floating-point comparison
        return Double.compare(point.x, x) == 0
                && Double.compare(point.y, y) == 0;
    }

    /**
     * Generates a hash code based on the coordinate values.
     *
     * @return The hash code value.
     */
    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
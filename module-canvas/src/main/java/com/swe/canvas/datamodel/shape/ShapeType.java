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

/**
 * Enumerates the concrete types of shapes available.
 *
 * <p>
 * This is used for serialization and by the {@link ShapeFactory}
 * to identify which shape type to create.
 * </p>
 *
 * @author Gajjala Bhavani Shankar 
 */
public enum ShapeType {
    /**
     * A shape composed of a list of arbitrary points.
     */
    FREEHAND,

    /**
     * A rectangle defined by two diagonal corner points.
     */
    RECTANGLE,

    /**
     * An ellipse defined by a bounding box (two diagonal points).
     */
    ELLIPSE,

    /**
     * A triangle defined by a bounding box (two diagonal points).
     */
    TRIANGLE,

    /**
     * A straight line defined by two endpoints.
     */
    LINE,

    /**
     * Unknown Shape.
     */
    UNKNOWN
}
/*
 * -----------------------------------------------------------------------------
 * File: EllipseShape.java
 * Owner: Gajjala Bhavani Shankar
 * Roll Number : 112201026
 * Module : Canvas
 *
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.shape;

import java.awt.Color;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete shape representing an ellipse (or circle).
 *
 * <p>An ellipse in this system is defined by a "bounding box" rectangle.
 * The two points provided in the constructor represent the diagonal corners
 * of this imaginary box (e.g., Top-Left and Bottom-Right). The rendering
 * engine inscribes the ellipse within these bounds.</p>
 *
 * <p><b>Thread Safety:</b> This class is not thread-safe. Synchronization
 * must be handled by the state manager (CanvasState).</p>
 *
 * @author Gajjala Bhavani Shankar
 */
public class EllipseShape extends Shape {

    /**
     * Used for Java serialization version control.
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The number of points required to define the bounding box of the ellipse.
     */
    private static final int REQUIRED_POINTS = 2;

    /**
     * Constructs a new EllipseShape.
     *
     * <p>We require exactly two points to define the bounding box.
     * If the width and height of the bounding box are equal, this shape
     * renders as a circle.</p>
     *
     * @param identifier    The unique ID.
     * @param boundingBox   List containing exactly 2 diagonal corner points.
     * @param lineThickness The stroke thickness.
     * @param shapeColor    The shape color.
     * @param creatorId     The creating user's ID.
     * @param updaterId     The last modifying user's ID.
     * @throws IllegalArgumentException if the points list does not contain exactly 2 points.
     */
    public EllipseShape(final ShapeId identifier,
                        final List<Point> boundingBox,
                        final double lineThickness,
                        final Color shapeColor,
                        final String creatorId,
                        final String updaterId) {
        super(identifier, ShapeType.ELLIPSE, boundingBox, lineThickness, shapeColor, creatorId, updaterId);

        if (boundingBox.size() != REQUIRED_POINTS) {
            throw new IllegalArgumentException(
                    "EllipseShape must be defined by exactly " + REQUIRED_POINTS + " points."
            );
        }
    }

    /**
     * Creates a deep copy of this ellipse.
     *
     * <p>It is crucial to create a new ArrayList for the points to ensure
     * that the copied shape is fully independent of the original. This allows
     * one to be moved or resized without affecting the other.</p>
     *
     * @return A new EllipseShape instance with identical properties.
     */
    @Override
    public Shape copy() {
        return new EllipseShape(
                getShapeId(),
                new ArrayList<>(getPoints()), // Use getter
                getThickness(),
                getColor(),
                getCreatedBy(),
                getLastUpdatedBy()
        );
    }
}
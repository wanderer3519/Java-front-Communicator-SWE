/*
 * -----------------------------------------------------------------------------
 * File: RectangleShape.java
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
 * Concrete shape representing a rectangle.
 *
 * <p>A rectangle is strictly defined by exactly two points representing its
 * diagonal corners (e.g., Top-Left and Bottom-Right). The rendering logic
 * will calculate the width and height based on these two coordinates.</p>
 *
 * <p><b>Thread Safety:</b> This class is not thread-safe. Synchronization
 * must be handled by the state manager (CanvasState).</p>
 *
 * @author Gajjala Bhavani Shankar
 */
public class RectangleShape extends Shape {

    /**
     * Used for Java serialization version control.
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The exact number of points required to define a rectangle (diagonal corners).
     */
    private static final int REQUIRED_POINTS = 2;

    /**
     * Constructs a new RectangleShape.
     *
     * <p>We enforce a strict validation here: a rectangle implies geometry defined
     * by exactly two opposing corners. Any other number of points is invalid.</p>
     *
     * @param identifier    The unique ID.
     * @param corners       List containing exactly 2 diagonal corner points.
     * @param lineThickness The stroke thickness.
     * @param shapeColor    The shape color.
     * @param creatorId     The creating user's ID.
     * @param updaterId     The last modifying user's ID.
     * @throws IllegalArgumentException if the points list does not contain exactly 2 points.
     */
    public RectangleShape(final ShapeId identifier,
                          final List<Point> corners,
                          final double lineThickness,
                          final Color shapeColor,
                          final String creatorId,
                          final String updaterId) {
        super(identifier, ShapeType.RECTANGLE, corners, lineThickness, shapeColor, creatorId, updaterId);

        if (corners.size() != REQUIRED_POINTS) {
            throw new IllegalArgumentException(
                "RectangleShape must be defined by exactly " + REQUIRED_POINTS + " points."
            );
        }
    }

    /**
     * Creates a deep copy of this rectangle.
     *
     * <p>This is essential for features like Copy/Paste or Undo/Redo, ensuring
     * that the new rectangle is completely independent of the original.</p>
     *
     * @return A new RectangleShape instance with identical properties.
     */
    @Override
    public Shape copy() {
        return new RectangleShape(
                getShapeId(),
                new ArrayList<>(getPoints()), // Use getter
                getThickness(),
                getColor(),
                getCreatedBy(),
                getLastUpdatedBy()
        );
    }
}
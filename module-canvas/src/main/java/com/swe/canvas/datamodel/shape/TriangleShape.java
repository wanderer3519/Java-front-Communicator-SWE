/*
 * -----------------------------------------------------------------------------
 * File: TriangleShape.java
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
 * Concrete shape representing a triangle defined by a bounding box.
 *
 * <p>Unlike a generic polygon, this triangle is defined by exactly two points
 * representing the diagonal corners of a bounding box (Top-Left, Bottom-Right).
 * The rendering engine is responsible for inscribing the triangle (usually isosceles)
 * within these bounds.</p>
 *
 * <p><b>Thread Safety:</b> This class is not thread-safe. Synchronization
 * must be handled by the state manager (CanvasState).</p>
 *
 * @author Gajjala Bhavani Shankar
 */
public class TriangleShape extends Shape {

    /**
     * Used for Java serialization version control.
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The number of points required to define the bounding box of the triangle.
     */
    private static final int REQUIRED_POINTS = 2;

    /**
     * Constructs a new TriangleShape.
     *
     * <p>We validate that exactly two points are provided. These points define
     * the rectangular area in which the triangle will be drawn.</p>
     *
     * @param identifier    The unique ID.
     * @param boundingBox   List containing exactly 2 diagonal corner points.
     * @param lineThickness The stroke thickness.
     * @param shapeColor    The shape color.
     * @param creatorId     The creating user's ID.
     * @param updaterId     The last modifying user's ID.
     * @throws IllegalArgumentException if the points list does not contain exactly 2 points.
     */
    public TriangleShape(final ShapeId identifier,
                         final List<Point> boundingBox,
                         final double lineThickness,
                         final Color shapeColor,
                         final String creatorId,
                         final String updaterId) {
        super(identifier, ShapeType.TRIANGLE, boundingBox, lineThickness, shapeColor, creatorId, updaterId);

        if (boundingBox.size() != REQUIRED_POINTS) {
            throw new IllegalArgumentException(
                "TriangleShape must be defined by exactly " + REQUIRED_POINTS + " points."
            );
        }
    }

    /**
     * Creates a deep copy of this triangle.
     *
     * <p>Ensures that the new shape instance has its own independent list of points,
     * preventing side effects when modifying the copy.</p>
     *
     * @return A new TriangleShape instance with identical properties.
     */
    @Override
    public Shape copy() {
        return new TriangleShape(
                getShapeId(),
                new ArrayList<>(getPoints()), // Use getter
                getThickness(),
                getColor(),
                getCreatedBy(),
                getLastUpdatedBy()
        );
    }
}
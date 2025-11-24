/*
 * -----------------------------------------------------------------------------
 * File: LineShape.java
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
 * Concrete shape representing a straight line.
 *
 * <p>A Line is the simplest geometric shape, strictly defined by exactly two points:
 * a start point and an end point. Unlike closed shapes (like rectangles),
 * it has no "interior" to fill, though it shares the standard color and thickness
 * properties.</p>
 *
 * <p><b>Thread Safety:</b> This class is not thread-safe. Synchronization
 * must be handled by the state manager (CanvasState).</p>
 *
 * @author Gajjala Bhavani Shankar
 */
public class LineShape extends Shape {

    /**
     * Used for Java serialization version control.
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The strict number of points required to define a line (Start and End).
     */
    private static final int REQUIRED_POINTS = 2;

    /**
     * Constructs a new LineShape.
     *
     * <p>We enforce strict validation here. A line conceptually requires exactly
     * two points. Providing fewer (a point) or more (a polyline) is not allowed
     * for this specific class.</p>
     *
     * @param identifier    The unique ID.
     * @param endpoints     List containing exactly 2 endpoints (Start and End).
     * @param lineThickness The stroke thickness.
     * @param lineColor     The shape color.
     * @param creator       The creating user's ID.
     * @param updater       The last modifying user's ID.
     * @throws IllegalArgumentException if the points list does not contain exactly 2 points.
     */
    public LineShape(final ShapeId identifier,
                     final List<Point> endpoints,
                     final double lineThickness,
                     final Color lineColor,
                     final String creator,
                     final String updater) {
        super(identifier, ShapeType.LINE, endpoints, lineThickness, lineColor, creator, updater);

        if (endpoints.size() != REQUIRED_POINTS) {
            throw new IllegalArgumentException(
                "LineShape must be defined by exactly " + REQUIRED_POINTS + " points."
            );
        }
    }

    /**
     * Creates a deep copy of this line.
     *
     * <p>We create a new ArrayList for the points to ensure that modifying the
     * copy's geometry does not affect the original line.</p>
     *
     * @return A new LineShape instance with identical properties.
     */
    @Override
    public Shape copy() {
        return new LineShape(
                getShapeId(),
                new ArrayList<>(getPoints()), // Use getter
                getThickness(),
                getColor(),
                getCreatedBy(),
                getLastUpdatedBy()
        );
    }
}
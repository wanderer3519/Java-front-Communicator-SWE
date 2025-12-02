/*
 * -----------------------------------------------------------------------------
 * File: FreehandShape.java
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
 * Concrete shape representing a freehand drawing (pencil tool).
 *
 * <p>Unlike geometric shapes (like Rectangles or Lines) which are defined by
 * a fixed number of control points, a Freehand shape is defined by an arbitrary
 * sequence of points. The rendering engine connects these points to form a
 * continuous curve or stroke.</p>
 *
 * <p><b>Thread Safety:</b> This class is not thread-safe. Large freehand shapes
 * (with thousands of points) may have performance implications during copying
 * or rendering, so efficient management by the CanvasState is crucial.</p>
 *
 * @author Gajjala Bhavani Shankar
 */
public class FreehandShape extends Shape {

    /**
     * Used for Java serialization version control.
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new FreehandShape.
     *
     * <p>We permit any number of points for a freehand shape, provided the list
     * is not empty. A single point renders as a dot, while multiple points
     * render as a path.</p>
     *
     * @param identifier    The unique ID.
     * @param pointList     The ordered list of points making up the stroke.
     * @param lineThickness The stroke thickness.
     * @param shapeColor    The shape color.
     * @param creatorId     The creating user's ID.
     * @param updaterId     The last modifying user's ID.
     * @throws IllegalArgumentException if the point list is empty.
     */
    public FreehandShape(final ShapeId identifier,
                         final List<Point> pointList,
                         final double lineThickness,
                         final Color shapeColor,
                         final String creatorId,
                         final String updaterId) {
        super(identifier, ShapeType.FREEHAND, pointList, lineThickness, shapeColor, creatorId, updaterId);

        // A freehand shape must exist physically, so it needs at least one point.
        if (pointList.isEmpty()) {
            throw new IllegalArgumentException("FreehandShape must contain at least one point.");
        }
    }

    /**
     * Creates a deep copy of this freehand shape.
     *
     * <p>Because freehand shapes can contain many points, we ensure a full
     * deep copy of the list to preventing concurrent modification issues
     * between the original shape and its clone (e.g., in Undo stacks).</p>
     *
     * @return A new FreehandShape instance with identical properties.
     */
    @Override
    public Shape copy() {
        return new FreehandShape(
                getShapeId(), // Use getter
                new ArrayList<>(getPoints()), // Use getter and deep copy list
                getThickness(), // Use getter
                getColor(), // Use getter
                getCreatedBy(), // Use getter
                getLastUpdatedBy() // Use getter
        );
    }
}
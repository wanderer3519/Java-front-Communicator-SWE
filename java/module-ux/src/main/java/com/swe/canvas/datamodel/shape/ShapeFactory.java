/*
 * -----------------------------------------------------------------------------
 * File: ShapeFactory.java
 * Owner: Gajjala Bhavani Shankar
 * Roll Number : 112201026
 * Module : Canvas
 *
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.shape;

import java.awt.Color;
import java.util.List;

/**
 * Factory class responsible for creating concrete {@link Shape} instances.
 *
 * <p>This class encapsulates the object creation logic (The "Factory Method" pattern).
 * It decouples the client code (like the UI or Action handlers) from the specific
 * constructors of the shape classes. If we need to add a new shape type later,
 * we only need to modify this class rather than every place where shapes are created.</p>
 *
 * <p><b>Thread Safety:</b> This class is stateless and purely functional,
 * making it inherently thread-safe.</p>
 *
 * @author Gajjala Bhavani Shankar
 */
public class ShapeFactory {

    /**
     * Creates a new shape instance based on the provided type and properties.
     *
     * @param type           The enum type of shape to create (e.g., RECTANGLE, LINE).
     * @param identifier     The unique ID for the new shape.
     * @param shapePoints    The geometric points defining the shape.
     * @param lineThickness  The stroke thickness.
     * @param shapeColor     The color of the shape.
     * @param creatorId      The user ID of the creator (also sets lastUpdatedBy).
     * @return A new, concrete {@link Shape} instance.
     * @throws IllegalArgumentException if the shapeType is not recognized or supported.
     */
    public Shape createShape(final ShapeType type,
                             final ShapeId identifier,
                             final List<Point> shapePoints,
                             final double lineThickness,
                             final Color shapeColor,
                             final String creatorId) {

        // We pass 'creatorId' as both the creator and the initial 'lastUpdatedBy' user.
        switch (type) {
            case FREEHAND:
                return new FreehandShape(identifier, shapePoints, lineThickness, shapeColor, creatorId, creatorId);
            case RECTANGLE:
                return new RectangleShape(identifier, shapePoints, lineThickness, shapeColor, creatorId, creatorId);
            case ELLIPSE:
                return new EllipseShape(identifier, shapePoints, lineThickness, shapeColor, creatorId, creatorId);
            case TRIANGLE:
                return new TriangleShape(identifier, shapePoints, lineThickness, shapeColor, creatorId, creatorId);
            case LINE:
                return new LineShape(identifier, shapePoints, lineThickness, shapeColor, creatorId, creatorId);
            default:
                // This branch handles cases where a new Enum value is added but not yet implemented here.
                throw new IllegalArgumentException("Unknown or unsupported shape type: " + type);
        }
    }
}
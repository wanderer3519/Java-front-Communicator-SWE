/*
 * -----------------------------------------------------------------------------
 * File: Shape.java
 * Owner: Gajjala Bhavani Shankar
 * Roll Number : 112201026
 * Module : Canvas
 *
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.shape;

import java.awt.Color;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Abstract base class representing a drawable geometric shape on a canvas.
 *
 * <p>This class acts as the blueprint for all graphical elements in our application.
 * It holds the shared state that every shape needs: where it is (points), what it
 * looks like (color, thickness), and who owns it (creator metadata).</p>
 *
 * <p>Concrete implementations (like Rectangle or Circle) will extend this to provide
 * the specific math needed to draw themselves.</p>
 *
 * <p><b>Thread Safety:</b> This class is mutable and not thread-safe. Synchronization
 * should be handled by the managing controller (like the CanvasState).</p>
 *
 * @author Gajjala Bhavani Shankar
 */
public abstract class Shape implements Serializable {

    /**
     * Used for Java serialization version control.
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The length of the ID substring used in toString() for brevity.
     */
    private static final int ID_DISPLAY_LENGTH = 8;

    /**
     * Unique identifier for this shape.
     */
    private final ShapeId shapeId;

    /**
     * Identifier of the user who created this shape.
     */
    private final String createdBy;

    /**
     * Type of the shape (e.g., RECTANGLE, LINE, CIRCLE).
     */
    private final ShapeType shapeType;

    /**
     * List of defining points for the shape (e.g., corners or control points).
     */
    private List<Point> points;

    /**
     * Thickness of the shape’s outline.
     */
    private double thickness;

    /**
     * Color used to render the shape.
     */
    private Color color;

    /**
     * Identifier of the user who last modified this shape.
     */
    private String lastUpdatedBy;

    /**
     * Constructs a new {@code Shape} instance with the given properties.
     *
     * <p>This constructor initializes all the mandatory fields. We perform strict
     * null checks here to prevent "partially initialized" shapes from existing
     * in the system.</p>
     *
     * @param identifier     The unique shape identifier.
     * @param type           The shape type enumeration.
     * @param shapePoints    The list of defining points.
     * @param lineThickness  The line thickness.
     * @param shapeColor     The color used to render the shape.
     * @param creatorId      The identifier of the user who created this shape.
     * @param updaterId      The identifier of the user who last modified this shape.
     * @throws NullPointerException if any required argument is {@code null}.
     */
    protected Shape(final ShapeId identifier,
                    final ShapeType type,
                    final List<Point> shapePoints,
                    final double lineThickness,
                    final Color shapeColor,
                    final String creatorId,
                    final String updaterId) {
        this.shapeId = Objects.requireNonNull(identifier, "shapeId cannot be null");
        this.shapeType = Objects.requireNonNull(type, "shapeType cannot be null");
        this.points = Objects.requireNonNull(shapePoints, "points list cannot be null");
        this.thickness = lineThickness;
        this.color = Objects.requireNonNull(shapeColor, "color cannot be null");
        this.createdBy = Objects.requireNonNull(creatorId, "createdBy cannot be null");
        this.lastUpdatedBy = Objects.requireNonNull(updaterId, "lastUpdatedBy cannot be null");
    }

    /**
     * Translates (moves) this shape by the given offset.
     *
     * <p>This method calculates new coordinates for every point in the shape
     * based on the input deltas (dx, dy). It effectively "slides" the shape
     * across the canvas without changing its dimensions.</p>
     *
     * @param dx The horizontal displacement.
     * @param dy The vertical displacement.
     */
    public void translate(final double dx, final double dy) {
        final List<Point> newPoints = new ArrayList<>(points.size());
        for (final Point p : points) {
            newPoints.add(new Point(p.getX() + dx, p.getY() + dy));
        }
        this.points = newPoints;
    }

    /**
     * Creates and returns a deep copy of this shape.
     *
     * <p>Subclasses must implement this to ensure that when we clone a shape
     * (e.g., for Copy-Paste functionality), we get a truly independent object,
     * not just a reference to the old one.</p>
     *
     * @return A deep copy of this shape.
     */
    public abstract Shape copy();

    /**
     * Retrieves the unique identifier of this shape.
     *
     * @return The shape ID object.
     */
    public ShapeId getShapeId() {
        return shapeId;
    }

    /**
     * Retrieves the list of defining points for this shape.
     *
     * @return A list of Point objects.
     */
    public List<Point> getPoints() {
        return points;
    }

    /**
     * Retrieves the line thickness of the shape.
     *
     * @return The thickness as a double value.
     */
    public double getThickness() {
        return thickness;
    }

    /**
     * Retrieves the color used to render this shape.
     *
     * @return The AWT Color object.
     */
    public Color getColor() {
        return color;
    }

    /**
     * Retrieves the user ID of the creator.
     *
     * @return The creator's ID string.
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Retrieves the user ID of the last person who updated this shape.
     *
     * @return The updater's ID string.
     */
    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    /**
     * Retrieves the type classification of this shape.
     *
     * @return The ShapeType enum value.
     */
    public ShapeType getShapeType() {
        return shapeType;
    }

    /**
     * Updates the defining points of this shape.
     *
     * @param shapePoints The new list of points.
     */
    public void setPoints(final List<Point> shapePoints) {
        this.points = shapePoints;
    }

    /**
     * Updates the thickness of the shape’s outline.
     *
     * @param lineThickness The new line thickness.
     */
    public void setThickness(final double lineThickness) {
        this.thickness = lineThickness;
    }

    /**
     * Updates the color used to render this shape.
     *
     * @param shapeColor The new color.
     */
    public void setColor(final Color shapeColor) {
        this.color = shapeColor;
    }

    /**
     * Updates the user who last modified this shape.
     *
     * @param updaterId The last updating user ID.
     */
    public void setLastUpdatedBy(final String updaterId) {
        this.lastUpdatedBy = updaterId;
    }

    /**
     * Compares this shape to another object for equality.
     *
     * <p>We verify every single property. If even one pixel is different,
     * or if the color is slightly off, the shapes are considered distinct.
     * This is crucial for Undo/Redo operations to detect changes.</p>
     *
     * @param obj The object to compare with.
     * @return True if the objects are identical in state.
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final Shape shape = (Shape) obj;
        return Double.compare(shape.thickness, thickness) == 0
                && shapeId.equals(shape.shapeId)
                && points.equals(shape.points)
                && color.equals(shape.color)
                && createdBy.equals(shape.createdBy)
                && lastUpdatedBy.equals(shape.lastUpdatedBy)
                && shapeType == shape.shapeType;
    }

    /**
     * Computes a hash code for this shape based on all its properties.
     *
     * @return The computed hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(shapeId, points, thickness, color,
                createdBy, lastUpdatedBy, shapeType);
    }

    /**
     * Returns a concise string representation of this shape.
     *
     * <p>This is primarily used for debugging logs. To keep logs readable,
     * we only print the first few characters of the Shape ID.</p>
     *
     * @return A summary string.
     */
    @Override
    public String toString() {
        final String rawId = shapeId.getValue();
        final String idVal;

        // Ensure we don't crash if ID is shorter than the display length
        if (rawId.length() > ID_DISPLAY_LENGTH) {
            idVal = rawId.substring(0, ID_DISPLAY_LENGTH);
        } else {
            idVal = rawId;
        }

        return String.format("%s[id=%s, points=%d, color=%s]",
                shapeType, idVal, points.size(), color);
    }
}
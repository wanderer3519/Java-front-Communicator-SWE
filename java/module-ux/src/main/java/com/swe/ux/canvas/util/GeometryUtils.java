/*
 * -----------------------------------------------------------------------------
 * File: GeometryUtils.java
 * Owner: Darla Manohar
 * Roll Number: 112201034
 * Module: Canvas
 *
 * -----------------------------------------------------------------------------
 */

package com.swe.ux.canvas.util;

import com.swe.canvas.datamodel.shape.Point;
import com.swe.canvas.datamodel.shape.Shape;
import com.swe.canvas.datamodel.shape.ShapeType;
import java.util.List;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;

/**
 * Utility for geometric calculations (bounding boxes, hit testing).
 *
 * <p>This class provides static helper methods to calculate bounds and detect
 * collisions/clicks on shapes.</p>
 *
 * @author Darla Manohar
 */
public final class GeometryUtils {

    /**
     * The pixel distance threshold for detecting a "hit" on a line or curve.
     */
    private static final double HIT_THRESHOLD = 5.0;

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private GeometryUtils() {
        // Utility class
    }

    /**
     * Calculates the axis-aligned bounding box of a shape based on its points.
     *
     * @param shape The shape to measure.
     * @return The Bounds of the shape. Returns a 0-sized box if no points exist.
     */
    public static Bounds getBounds(final Shape shape) {
        final List<Point> points = shape.getPoints();
        if (points.isEmpty()) {
            return new BoundingBox(0, 0, 0, 0);
        }

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        for (final Point p : points) {
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
        }

        return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * Tests if a specific coordinate point "hits" or intersects with a shape.
     *
     * @param shape The shape to test against.
     * @param x     The x-coordinate of the click/point.
     * @param y     The y-coordinate of the click/point.
     * @return True if the point is considered inside or on the shape.
     */
    public static boolean hitTest(final Shape shape, final double x, final double y) {
        final Bounds b = getBounds(shape);

        // Optimization: Simple bounding box hit test.
        // We skip this rejection for Lines/Freehand because their bounds might
        // not strictly contain the "thick" stroke hit area near the edges.
        final boolean isComplex = shape.getShapeType() == ShapeType.FREEHAND
            || shape.getShapeType() == ShapeType.LINE;

        if (!b.contains(x, y) && !isComplex) {
            return false;
        }

        switch (shape.getShapeType()) {
            case LINE:
                return isHitLine(shape, x, y);
            case FREEHAND:
                return isHitFreehand(shape, x, y);
            case RECTANGLE:
            case ELLIPSE:
            case TRIANGLE:
            default:
                return b.contains(x, y);
        }
    }

    /**
     * Checks if a point hits a Line shape.
     *
     * @param shape The shape to test.
     * @param x     The x-coordinate.
     * @param y     The y-coordinate.
     * @return      True if the point hits the line.
     */
    private static boolean isHitLine(final Shape shape, final double x, final double y) {
        if (shape.getPoints().size() < 2) {
            return false;
        }
        return distanceToLine(shape.getPoints().get(0),
            shape.getPoints().get(1), x, y) < HIT_THRESHOLD;
    }

    /**
     * Checks if a point hits a Freehand (polyline) shape.
     *
     * @param shape The shape to test.
     * @param x     The x-coordinate.
     * @param y     The y-coordinate.
     * @return      True if the point hits the polyline.
     */
    private static boolean isHitFreehand(final Shape shape, final double x, final double y) {
        final List<Point> points = shape.getPoints();
        for (int i = 0; i < points.size() - 1; i++) {
            final Point p1 = points.get(i);
            final Point p2 = points.get(i + 1);
            if (distanceToLine(p1, p2, x, y) < HIT_THRESHOLD) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculates the minimum distance from a point (px, py) to a line segment defined by p1 and p2.
     *
     * @param p1 The start point of the line segment.
     * @param p2 The end point of the line segment.
     * @param px The x-coordinate of the test point.
     * @param py The y-coordinate of the test point.
     * @return The Euclidean distance from the point to the closest spot on the segment.
     */
    private static double distanceToLine(final Point p1, final Point p2,
        final double px, final double py) {
        final double x1 = p1.getX();
        final double y1 = p1.getY();
        final double x2 = p2.getX();
        final double y2 = p2.getY();

        final double diffX = px - x1;
        final double diffY = py - y1;
        final double spanX = x2 - x1;
        final double spanY = y2 - y1;

        final double dot = diffX * spanX + diffY * spanY;
        final double lenSq = spanX * spanX + spanY * spanY;

        double param = -1;

        if (lenSq != 0) {
            param = dot / lenSq;
        }

        final double closestX;
        final double closestY;

        if (param < 0) {
            closestX = x1;
            closestY = y1;
        } else if (param > 1) {
            closestX = x2;
            closestY = y2;
        } else {
            closestX = x1 + param * spanX;
            closestY = y1 + param * spanY;
        }

        final double dx = px - closestX;
        final double dy = py - closestY;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
/*
 * -----------------------------------------------------------------------------
 * File: CanvasRenderer.java
 * Owner: Gajula Sri Siva Sai Shashank
 * Roll Number: 112201014
 * Module: Canvas
 *
 * -----------------------------------------------------------------------------
 */

package com.swe.ux.canvas;

import com.swe.canvas.datamodel.canvas.CanvasState;
import com.swe.canvas.datamodel.canvas.ShapeState;
import com.swe.canvas.datamodel.shape.Point;
import com.swe.canvas.datamodel.shape.Shape;
import com.swe.canvas.datamodel.shape.ShapeId;
import com.swe.canvas.datamodel.shape.ShapeType;
import com.swe.ux.canvas.util.ColorConverter;
import com.swe.ux.canvas.util.GeometryUtils;
import java.util.List;
import javafx.geometry.Bounds;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Handles the rendering of shapes onto the JavaFX Canvas.
 *
 * <p>This class bridges the Data Model (Shapes) and the UI (JavaFX GraphicsContext).
 * It handles the drawing of committed shapes, transient (ghost) shapes during interactions,
 * and selection highlights.</p>
 *
 * @author Gajula Sri Siva Sai Shashank
 */
public class CanvasRenderer {

    /**
     * Alpha value for fully opaque shapes.
     */
    private static final double ALPHA_OPAQUE = 1.0;

    /**
     * Alpha value for transient "ghost" shapes (e.g., during dragging).
     */
    private static final double ALPHA_GHOST = 0.5;

    /**
     * Padding around the bounding box for selection visualization.
     */
    private static final double SELECTION_PADDING = 5.0;

    /**
     * Total adjustment for width/height of selection box (2 * padding).
     */
    private static final double SELECTION_SIZE_ADJUSTMENT = 10.0;

    /**
     * Dash size for the selection rectangle outline.
     */
    private static final double SELECTION_DASH_SIZE = 5.0;

    /**
     * Divisor for calculating midpoints (e.g., for triangles).
     */
    private static final double MIDPOINT_DIVISOR = 2.0;

    /**
     * Number of vertices in a triangle.
     */
    private static final int TRIANGLE_VERTICES = 3;

    /**
     * Minimum points required to draw a line or shape.
     */
    private static final int MIN_POINTS_DRAW = 2;

    /**
     * The target JavaFX Canvas.
     */
    private final Canvas canvas;

    /**
     * The GraphicsContext associated with the canvas.
     */
    private final GraphicsContext gc;

    /**
     * Constructor for renderer.
     *
     * @param targetCanvas Instance of the JavaFX canvas to draw upon.
     */
    public CanvasRenderer(final Canvas targetCanvas) {
        this.canvas = targetCanvas;
        this.gc = canvas.getGraphicsContext2D();
    }

    /**
     * Main Rendering logic.
     *
     * <p>Clears the canvas and redraws the scene in three layers:
     * 1. Committed shapes.
     * 2. Transient (ghost) shapes.
     * 3. Selection overlays.</p>
     *
     * @param state               The current authoritative canvas state.
     * @param transientShape      The "ghost" shape awaiting network confirmation or currently being drawn.
     * @param selectedShapeId     The ID of the currently selected shape (if any).
     * @param isDraggingSelection Boolean flag indicating if the selection is being moved.
     */
    public void render(final CanvasState state,
                       final Shape transientShape,
                       final ShapeId selectedShapeId,
                       final boolean isDraggingSelection) {
        // We must use the canvas's fixed width/height, not the container's
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // 1. Draw all committed shapes
        for (final Shape shape : state.getVisibleShapes()) {
            // If we are ghosting this shape (e.g., dragging it), don't draw the original.
            if (transientShape != null && shape.getShapeId().equals(transientShape.getShapeId())) {
                continue;
            }
            drawShape(shape, ALPHA_OPAQUE); // Draw fully opaque
        }

        // 2. Draw ghost shape (either new drawing OR shape being moved)
        if (transientShape != null) {
            // Draw ghost with 50% opacity
            drawShape(transientShape, ALPHA_GHOST);
        }

        // 3. Draw selection box
        if (selectedShapeId != null) {
            final ShapeState selectedState = state.getShapeState(selectedShapeId);
            if (selectedState != null && !selectedState.isDeleted()) {
                // If we are dragging the selected shape, draw box around the ghost
                if (transientShape != null && transientShape.getShapeId().equals(selectedShapeId)) {
                    drawBoundingBox(transientShape);
                } else {
                    // Otherwise draw around the shape in the main state
                    drawBoundingBox(selectedState.getShape());
                }
            }
        }
    }

    /**
     * Configures the graphics context and delegates drawing to specific shape handlers.
     * This method is split to reduce cyclomatic complexity.
     *
     * @param shape The shape object to be drawn.
     * @param alpha The opacity level to draw the shape with (0.0 to 1.0).
     */
    private void drawShape(final Shape shape, final double alpha) {
        gc.setStroke(ColorConverter.toFx(shape.getColor()));
        gc.setLineWidth(shape.getThickness());
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        gc.setGlobalAlpha(alpha);

        final List<Point> p = shape.getPoints();
        if (p.isEmpty()) {
            return;
        }

        drawPrimitive(shape.getShapeType(), p);
        gc.setGlobalAlpha(ALPHA_OPAQUE); // Reset alpha
    }

    /**
     * Dispatch logic for different shape types to avoid complex control flow in main draw method.
     *
     * @param type The type of the shape (e.g., LINE, RECTANGLE).
     * @param p    The list of points defining the geometry of the shape.
     */
    private void drawPrimitive(final ShapeType type, final List<Point> p) {
        switch (type) {
            case FREEHAND:
                drawFreehand(p);
                break;
            case LINE:
                if (p.size() >= MIN_POINTS_DRAW) {
                    gc.strokeLine(p.get(0).getX(), p.get(0).getY(), p.get(1).getX(), p.get(1).getY());
                }
                break;
            case RECTANGLE:
                if (p.size() >= MIN_POINTS_DRAW) {
                    drawRect(p.get(0), p.get(1));
                }
                break;
            case ELLIPSE:
                if (p.size() >= MIN_POINTS_DRAW) {
                    drawEllipse(p.get(0), p.get(1));
                }
                break;
            case TRIANGLE:
                if (p.size() >= MIN_POINTS_DRAW) {
                    drawTriangle(p.get(0), p.get(1));
                }
                break;
            default:
                break;
        }
    }

    private void drawFreehand(final List<Point> p) {
        gc.beginPath();
        gc.moveTo(p.get(0).getX(), p.get(0).getY());
        for (int i = 1; i < p.size(); i++) {
            gc.lineTo(p.get(i).getX(), p.get(i).getY());
        }
        gc.stroke();
    }

    private void drawBoundingBox(final Shape shape) {
        final Bounds b = GeometryUtils.getBounds(shape);
        gc.setStroke(Color.CORNFLOWERBLUE);
        gc.setLineWidth(1);
        gc.setLineDashes(SELECTION_DASH_SIZE);
        // Draw slightly larger than the shape
        gc.strokeRect(
                b.getMinX() - SELECTION_PADDING,
                b.getMinY() - SELECTION_PADDING,
                b.getWidth() + SELECTION_SIZE_ADJUSTMENT,
                b.getHeight() + SELECTION_SIZE_ADJUSTMENT
        );
        gc.setLineDashes((double[]) null);
    }

    private void drawRect(final Point p1, final Point p2) {
        gc.strokeRect(Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()),
                Math.abs(p1.getX() - p2.getX()), Math.abs(p1.getY() - p2.getY()));
    }

    private void drawEllipse(final Point p1, final Point p2) {
        gc.strokeOval(Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()),
                Math.abs(p1.getX() - p2.getX()), Math.abs(p1.getY() - p2.getY()));
    }

    private void drawTriangle(final Point p1, final Point p2) {
        final double minX = Math.min(p1.getX(), p2.getX());
        final double minY = Math.min(p1.getY(), p2.getY());
        final double maxX = Math.max(p1.getX(), p2.getX());
        final double maxY = Math.max(p1.getY(), p2.getY());
        gc.strokePolygon(new double[] {minX + (maxX - minX) / MIDPOINT_DIVISOR, minX, maxX},
                new double[] {minY, maxY, maxY}, TRIANGLE_VERTICES);
    }
}
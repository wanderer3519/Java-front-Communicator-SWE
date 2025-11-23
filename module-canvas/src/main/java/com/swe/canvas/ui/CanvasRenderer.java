package com.swe.canvas.ui;

import java.util.List;

import com.swe.canvas.datamodel.canvas.CanvasState;
import com.swe.canvas.datamodel.canvas.ShapeState;
import com.swe.canvas.datamodel.shape.Point;
import com.swe.canvas.datamodel.shape.Shape;
import com.swe.canvas.datamodel.shape.ShapeId;
import com.swe.canvas.ui.util.ColorConverter;
import com.swe.canvas.ui.util.GeometryUtils;

import javafx.geometry.Bounds;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * @author Gajula Sri Siva Sai Shashank
 */

public class CanvasRenderer {

    private final Canvas canvas;
    private final GraphicsContext gc;

    /**
     * Constructor for renderer
     * @param canvas_: Instance of the canvas
     */
    public CanvasRenderer(final Canvas canvas_) {
        this.canvas = canvas_;
        this.gc = canvas.getGraphicsContext2D();
    }

    /**
     * Main Rendering logic
     * @param state current state
     * @param transientShape The "ghost" shape awaiting network confirmation
     * @param selectedShapeId id of the shape selected
     * @param isDraggingSelection dragging or not (Note: isDragging is now part of transientShape logic)
     */
    public void render(final CanvasState state, final Shape transientShape, final ShapeId selectedShapeId, final boolean isDraggingSelection) {
        // We must use the canvas's fixed width/height, not the container's
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // 1. Draw all committed shapes
        for (Shape shape : state.getVisibleShapes()) {
            // If we are ghosting this shape (e.g., dragging it), don't draw the original.
            if (transientShape != null && shape.getShapeId().equals(transientShape.getShapeId())) {
                continue;
            }
            drawShape(shape, 1.0); // Draw fully opaque
        }

        // 2. Draw ghost shape (either new drawing OR shape being moved)
        if (transientShape != null) {
            // Draw ghost with 50% opacity
            drawShape(transientShape, 0.5);
        }

        // 3. Draw selection box
        if (selectedShapeId != null) {
            ShapeState selectedState = state.getShapeState(selectedShapeId);
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

    private void drawShape(final Shape shape, final double alpha) {
        gc.setStroke(ColorConverter.toFx(shape.getColor()));
        gc.setLineWidth(shape.getThickness());
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        gc.setGlobalAlpha(alpha);

        final List<Point> p = shape.getPoints();
        if (p.isEmpty())
            return;

        switch (shape.getShapeType()) {
            case FREEHAND:
                gc.beginPath();
                gc.moveTo(p.get(0).getX(), p.get(0).getY());
                for (int i = 1; i < p.size(); i++) {
                    gc.lineTo(p.get(i).getX(), p.get(i).getY());
                }
                gc.stroke();
                break;
            case LINE:
                if (p.size() >= 2) {
                    gc.strokeLine(p.get(0).getX(), p.get(0).getY(), p.get(1).getX(), p.get(1).getY());
                }
                break;
            case RECTANGLE:
                if (p.size() >= 2) {
                    drawRect(p.get(0), p.get(1));
                }
                break;
            case ELLIPSE:
                if (p.size() >= 2) {
                    drawEllipse(p.get(0), p.get(1));
                }
                break;
            case TRIANGLE:
                if (p.size() >= 2) {
                    drawTriangle(p.get(0), p.get(1));
                }
                break;
        }
        gc.setGlobalAlpha(1.0); // Reset alpha
    }

    private void drawBoundingBox(final Shape shape) {
        final Bounds b = GeometryUtils.getBounds(shape);
        gc.setStroke(Color.CORNFLOWERBLUE);
        gc.setLineWidth(1);
        gc.setLineDashes(5);
        // Draw slightly larger than the shape
        gc.strokeRect(b.getMinX() - 5, b.getMinY() - 5, b.getWidth() + 10, b.getHeight() + 10);
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
        gc.strokePolygon(new double[] { minX + (maxX - minX) / 2.0, minX, maxX },
                new double[] { minY, maxY, maxY }, 3);
    }
}
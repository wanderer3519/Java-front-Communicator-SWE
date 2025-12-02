/*
 * -----------------------------------------------------------------------------
 * File: CanvasShapeCount.java
 * Owner: Darla Manohar
 * Roll Number: 112201034
 * Module: Canvas
 *
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.datamodel.analytics;

import com.swe.ux.model.analytics.ShapeCount;

/**
 * Represents the count of different shape types on the canvas.
 *
 * <p>This class extends the basic shape count model to provide
 * specific increment operations needed for analytics tracking
 * within the canvas module.</p>
 *
 * @author Darla Manohar
 */
public class CanvasShapeCount extends ShapeCount {

    /**
     * Constructs a new CanvasShapeCount with initial values.
     *
     * @param freeHand     Initial count of freehand shapes.
     * @param straightLine Initial count of straight lines.
     * @param rectangle    Initial count of rectangles.
     * @param ellipse      Initial count of ellipses.
     * @param triangle     Initial count of triangles.
     */
    public CanvasShapeCount(final int freeHand,
                            final int straightLine,
                            final int rectangle,
                            final int ellipse,
                            final int triangle) {
        super(freeHand, straightLine, rectangle, ellipse, triangle);
    }

    /**
     * Increments the count of freehand shapes by one.
     *
     * <p>Updates the internal counter for analytics purposes.</p>
     */
    public void incrementFreeHand() {
        this.setFreeHand(this.getFreeHand() + 1);
    }

    /**
     * Increments the count of straight lines by one.
     *
     * <p>Updates the internal counter for analytics purposes.</p>
     */
    public void incrementStraightLine() {
        this.setStraightLine(this.getStraightLine() + 1);
    }

    /**
     * Increments the count of rectangles by one.
     *
     * <p>Updates the internal counter for analytics purposes.</p>
     */
    public void incrementRectangle() {
        this.setRectangle(this.getRectangle() + 1);
    }

    /**
     * Increments the count of ellipses by one.
     *
     * <p>Updates the internal counter for analytics purposes.</p>
     */
    public void incrementEllipse() {
        this.setEllipse(this.getEllipse() + 1);
    }

    /**
     * Increments the count of triangles by one.
     *
     * <p>Updates the internal counter for analytics purposes.</p>
     */
    public void incrementTriangle() {
        this.setTriangle(this.getTriangle() + 1);
    }
}
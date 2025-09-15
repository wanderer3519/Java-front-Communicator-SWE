package com.swe.module_canvas.canvas.shape;

/**
 * Interface for shape.
 */
public interface Shape {

    int getId();

    int getColor();

    int setColor();

    int getStrokeThickness();

    int setStrokeThickness();

    boolean getIsSelected();

    boolean setIsSelected();

    boolean containsPoint();

    void draw();

    void rotate();

    void fill();
}

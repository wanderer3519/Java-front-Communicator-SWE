/*
 * -----------------------------------------------------------------------------
 *  File: ColorConverter.java
 *  Owner: Darla Manohar
 *  Roll Number: 112201034
 *  Module: Canvas
 *
 * -----------------------------------------------------------------------------
 */

package com.swe.ux.canvas.util;

/**
 * Converts between java.awt.Color (Data Model) and javafx.scene.paint.Color (UI).
 *
 * <p>This utility class bridges the gap between the backend data model which uses AWT colors
 * and the JavaFX frontend rendering engine.</p>
 *
 * @author Darla Manohar
 */
public final class ColorConverter {

    /**
     * Normalization factor for alpha channel (converts 0-255 to 0.0-1.0).
     */
    private static final double ALPHA_NORMALIZER = 255.0;

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private ColorConverter() {
        // Utility class
    }

    /**
     * Converts a Java AWT Color to a JavaFX Color.
     *
     * @param awtColor Input AWT color object.
     * @return The corresponding JavaFX Color object.
     */
    public static javafx.scene.paint.Color toFx(final java.awt.Color awtColor) {
        return javafx.scene.paint.Color.rgb(
                awtColor.getRed(),
                awtColor.getGreen(),
                awtColor.getBlue(),
                awtColor.getAlpha() / ALPHA_NORMALIZER);
    }

    /**
     * Converts a JavaFX Color to a Java AWT Color.
     *
     * @param fxColor Input JavaFX color object.
     * @return The corresponding AWT Color object.
     */
    public static java.awt.Color toAwt(final javafx.scene.paint.Color fxColor) {
        return new java.awt.Color(
                (float) fxColor.getRed(),
                (float) fxColor.getGreen(),
                (float) fxColor.getBlue(),
                (float) fxColor.getOpacity());
    }
}
/*
 * -----------------------------------------------------------------------------
 *  File: ColorConverter.java
 *  Owner: Darla Manohar
 *  Roll Number: 112201034
 *  Module: Canvas
 *
 * -----------------------------------------------------------------------------
 */

package com.swe.canvas.ui.util;

/**
 * Converts between java.awt.Color (Data Model) and javafx.scene.paint.Color (UI).
 */
public class ColorConverter {
    /**
     * Convert to javafx
     * @param awtColor Input awt color
     * @return javafx.scene.paint.Color
     */
    public static javafx.scene.paint.Color toFx(final java.awt.Color awtColor) {
        return javafx.scene.paint.Color.rgb(
                awtColor.getRed(),
                awtColor.getGreen(),
                awtColor.getBlue(),
                awtColor.getAlpha() / 255.0);
    }

    /**
     * Convert to awt
     * @param fxColor Input jfx color
     * @return java.awt.Color
     */
    public static java.awt.Color toAwt(final javafx.scene.paint.Color fxColor) {
        return new java.awt.Color(
                (float) fxColor.getRed(),
                (float) fxColor.getGreen(),
                (float) fxColor.getBlue(),
                (float) fxColor.getOpacity());
    }
}
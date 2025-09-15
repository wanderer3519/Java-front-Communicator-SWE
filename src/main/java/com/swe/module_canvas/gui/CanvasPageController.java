package com.swe.module_canvas.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Controller for the Canvas Gui.
 */
public class CanvasPageController {
    /** Welcome text used in gui. */
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }
}

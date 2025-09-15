package com.swe.module_canvas;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main application class for the Distributed GUI Demo in Java.
 */
public class App extends Application {

    /** The width of the application window. */
    private static final double WINDOW_WIDTH = 800.0;
    /** The height of the application window. */
    private static final double WINDOW_HEIGHT = 600.0;

    @Override
    public void start(final Stage primaryStage) throws IOException {
        final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/CanvasPage.fxml"));
        final Scene scene = new Scene(fxmlLoader.load(), WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setTitle("Hello!");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(final String[] args) {
        launch(args);
    }
}

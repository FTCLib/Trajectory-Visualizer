package com.litehed.trajectoryvis;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class VisualizerApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(VisualizerApplication.class.getResource("visualizer.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 660, 555);
        scene.getStylesheets().addAll(this.getClass().getResource("visualizer.css").toExternalForm());
        stage.setTitle("FTCLib Trajectory Visualizer");
        stage.getIcons().add(new Image("https://avatars.githubusercontent.com/u/58642103?s=280&v=4"));
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
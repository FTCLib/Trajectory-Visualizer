package com.litehed.trajectoryvis.extraControls;

import com.litehed.trajectoryvis.ExtraMath;
import com.litehed.trajectoryvis.ftclib.geometry.Translation2d;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.ArrayList;

public class Waypoint extends Circle {

    private double x, y;
    private int id;

    public Waypoint(NumberField xPos, NumberField yPos, ArrayList wpList) {
        super(7, Color.RED);
        setStroke(Color.BLACK);

        x = ExtraMath.clampNumField(xPos, Double.parseDouble(xPos.getText()));
        setTranslateY(ExtraMath.metersToUnits(x));
        y = ExtraMath.clampNumField(yPos, Double.parseDouble(yPos.getText()));
        setTranslateX(ExtraMath.metersToUnits(y));
        
        wpList.add(getWaypoint());
        id = wpList.size() - 1;

        xPos.textProperty().addListener((observable, oldValue, newValue) -> {
            x = ExtraMath.clampNumField(xPos, Double.parseDouble(newValue));
            setTranslateY(ExtraMath.metersToUnits(x));
            wpList.set(id, getWaypoint());
        });

        yPos.textProperty().addListener((observable, oldValue, newValue) -> {
            y = ExtraMath.clampNumField(yPos, Double.parseDouble(newValue));
            setTranslateX(ExtraMath.metersToUnits(y));
            wpList.set(id, getWaypoint());
        });
    }

    public Translation2d getWaypoint() {
        return new Translation2d(x, y);
    }
}

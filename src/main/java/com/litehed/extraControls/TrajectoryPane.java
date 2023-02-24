package com.litehed.extraControls;

import java.util.ArrayList;

import com.litehed.ftclib.geometry.Pose2d;
import com.litehed.ftclib.geometry.Rotation2d;
import com.litehed.ftclib.geometry.Translation2d;
import com.litehed.ftclib.trajectory.Trajectory;
import com.litehed.ftclib.trajectory.TrajectoryConfig;
import com.litehed.ftclib.trajectory.TrajectoryGenerator;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

public class TrajectoryPane extends TitledPane {

    private int id;

    private static ScrollPane wpScroll;
    private static VBox constr, startPos, endPos, intWP;
    private static Button wayPointBtn;
    private CheckBox reversed;

    private static DoubleProperty maxVel = new SimpleDoubleProperty(1.5), maxAccel = new SimpleDoubleProperty(1.5);

    public DoubleProperty startX = new SimpleDoubleProperty(), startY = new SimpleDoubleProperty(),
            startH = new SimpleDoubleProperty();
    public DoubleProperty endX = new SimpleDoubleProperty(), endY = new SimpleDoubleProperty(),
            endH = new SimpleDoubleProperty();

    private ArrayList<Translation2d> interiorWaypoints = new ArrayList<>();

    public TrajectoryPane(int id, Accordion trajAccordion) {
        super("Trajectory " + id, trajAccordion);
        this.id = id;
    }

    public TrajectoryPane(int id) {
        super("Trajectory " + id, generateAccordion());
        this.id = id;
    }

    private static Accordion generateAccordion() {
        Accordion trajAccordion = new Accordion();

        wayPointBtn = new Button("Add Waypoint");

        trajAccordion.getPanes().add(new TitledPane("Constraints", constr = new VBox(
                new Label("Max Velocity m/s"), new NumberField("1.5"),
                new Label("Max Acceleration m/s^2"), new NumberField("1.5"),
                new Label("Reversed"), new CheckBox())));
        trajAccordion.getPanes().add(new TitledPane("Start Pose", startPos = new VBox(
                new Label("Start X"), new NumberField(),
                new Label("Start Y"), new NumberField(),
                new Label("Start Heading"), new NumberField())));
        trajAccordion.getPanes().add(new TitledPane("Interior Waypoints", wpScroll = new ScrollPane(
                intWP = new VBox(
                        wayPointBtn))));
        trajAccordion.getPanes().add(new TitledPane("End Pose", endPos = new VBox(
                new Label("End X"), new NumberField(),
                new Label("End Y"), new NumberField(),
                new Label("End Heading"), new NumberField())));

        wpScroll.setFitToWidth(true);

        manageConstraints();
        return trajAccordion;
    }

    private static void manageConstraints() {

        ((NumberField) constr.getChildren().get(1)).textProperty().addListener((observable, oldValue, newValue) -> {
            if (Double.parseDouble(newValue) < 0)
                ((NumberField) constr.getChildren().get(1)).textProperty().set(oldValue);
            maxVel.set(Double.parseDouble(newValue));
        });
        ((NumberField) constr.getChildren().get(3)).textProperty().addListener((observable, oldValue, newValue) -> {
            if (Double.parseDouble(newValue) < 0)
                ((NumberField) constr.getChildren().get(3)).textProperty().set(oldValue);
            maxAccel.set(Double.parseDouble(newValue));
        });

    }

    public Trajectory getTrajectory() {
        Pose2d start = new Pose2d(startX.get(), startY.get(), new Rotation2d(Math.toRadians(startH.get())));
        Pose2d end = new Pose2d(endX.get(), endY.get(), new Rotation2d(Math.toRadians(endH.get())));

        reversed = (CheckBox) constr.getChildren().get(5);

        TrajectoryConfig config = new TrajectoryConfig(maxVel.get(), maxAccel.get());
        config.setReversed(reversed.isSelected());

        return TrajectoryGenerator.generateTrajectory(start, interiorWaypoints, end, config);
    }

    public ScrollPane getWpScroll() {
        return wpScroll;
    }

    public VBox getConstr() {
        return constr;
    }

    public VBox getStartPos() {
        return startPos;
    }

    public VBox getEndPos() {
        return endPos;
    }

    public VBox getIntWP() {
        return intWP;
    }

    public Button getWayPointBtn() {
        return wayPointBtn;
    }

    public int getTrajectoryId() {
        return id;
    }

    public void addWaypoint(Translation2d waypoint) {
        interiorWaypoints.add(waypoint);
    }

    public void removeWaypoint(Translation2d waypoint) {
        interiorWaypoints.remove(waypoint);
    }

    public void clearWaypoints() {
        interiorWaypoints.clear();
    }

    public ArrayList<Translation2d> getInteriorWaypoints() {
        return interiorWaypoints;
    }

    public DoubleProperty[] getStartPosArray() {
        return new DoubleProperty[] { startX, startY, startH };
    }

    public DoubleProperty[] getEndPosArray() {
        return new DoubleProperty[] { endX, endY, endH };
    }

    public DoubleProperty[] getConstraintsArray() {
        return new DoubleProperty[] { maxVel, maxAccel };
    }

    public boolean isReversed() {
        return (reversed != null) ? reversed.isSelected() : false;
    }

    public void setTrajectoryId(int id) {
        this.id = id;
        this.setText("Trajectory " + id);
    }

}

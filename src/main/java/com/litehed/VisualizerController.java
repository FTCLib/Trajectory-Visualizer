package com.litehed;

import com.litehed.extraControls.NumberField;
import com.litehed.extraControls.Waypoint;
import com.litehed.ftclib.geometry.Pose2d;
import com.litehed.ftclib.geometry.Rotation2d;
import com.litehed.ftclib.geometry.Translation2d;
import com.litehed.ftclib.trajectory.Trajectory;
import com.litehed.ftclib.trajectory.TrajectoryConfig;
import com.litehed.ftclib.trajectory.TrajectoryGenerator;
import javafx.animation.*;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;
import org.controlsfx.glyphfont.Glyph;

import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class VisualizerController {

    @FXML
    ImageView robot, background;
    @FXML
    VBox trajBox;
    @FXML
    HBox btnBox;
    @FXML
    Button addTrajBtn;
    @FXML
    StackPane fieldPane;
    @FXML
    Label coords;
    @FXML
    Canvas canvas;

    private final DoubleProperty robotWidth = new SimpleDoubleProperty(60.0);
    private final DoubleProperty robotHeight = new SimpleDoubleProperty(60.0);
    private final DoubleProperty startX = new SimpleDoubleProperty();
    private final DoubleProperty startY = new SimpleDoubleProperty();
    private final DoubleProperty startH = new SimpleDoubleProperty();
    private DoubleProperty endX = new SimpleDoubleProperty(), endY = new SimpleDoubleProperty(), endH = new SimpleDoubleProperty();
    private DoubleProperty maxVel = new SimpleDoubleProperty(1.5), maxAccel = new SimpleDoubleProperty(1.5);
    private final ArrayList<Translation2d> interiorWaypoints = new ArrayList<>();
    private final Accordion trajEditor = new Accordion();
    private final PathTransition pathTransition;
    private SequentialTransition rotateTransition;
    private SequentialTransition rotateList;
    private final VBox constr, startPos, intWP, endPos, robotBox;
    private final Rectangle endRect, startRect;
    private final Button playBtn;
    private PlayerBtn playType = PlayerBtn.PLAY;
    private Timeline timeline = new Timeline();
    private final ProgressBar progressBar;
    private final DoubleProperty time = new SimpleDoubleProperty(0.0);
    private final Label timeLabel = new Label();
    private Button wayPointBtn;
    private FileChooser fileChooser;
    private FileChooser exportChooser;
    private boolean isTrajCreated = false;
    private ArrayList<Translation2d> tempPoints = new ArrayList<>();
    private CheckBox reversed;

    public VisualizerController() {
        fileChooser = new FileChooser();
        fileChooser.setInitialFileName("trajectory");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("poggers", "*.pog"));
        exportChooser = new FileChooser();
        exportChooser.setInitialFileName("Trajectory");
        exportChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Java", "*.java"));

        endRect = new Rectangle(robotHeight.get(), robotWidth.get());
        startRect = new Rectangle(robotHeight.get(), robotWidth.get());
        endRect.widthProperty().bind(robotHeight);
        endRect.heightProperty().bind(robotWidth);
        startRect.widthProperty().bind(robotHeight);
        startRect.heightProperty().bind(robotWidth);
        endRect.setOpacity(0.3);
        startRect.setOpacity(0.3);

        wayPointBtn = new Button("Add Waypoint");
        wayPointBtn.setOnMouseClicked(mouseEvent -> addWaypoint(0.0, 0.0));

        playBtn = new Button("", new Glyph("FontAwesome", "PLAY"));

        pathTransition = new PathTransition();
        rotateList = new SequentialTransition();
        playBtn.setOnMouseClicked(mouseEvent -> handlePlay());

        progressBar = new ProgressBar();
        progressBar.progressProperty().bind(time);
        progressBar.setTranslateX(10.0);
        progressBar.setTranslateY(5.0);
        progressBar.setPrefWidth(350.0);
        timeLabel.setTranslateX(12.0);
        timeLabel.setTranslateY(5.0);

        progressBar.addEventHandler(MouseEvent.MOUSE_DRAGGED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                double newProgress = event.getX() / progressBar.getWidth();
                Duration newDuration = Duration.seconds(newProgress * trajectory().getTotalTimeSeconds());
                time.set(newProgress);
                timeline.playFrom(newDuration);
                if(playType == PlayerBtn.RESUME) timeline.pause();
                pathTransition.playFrom(newDuration);
                if(playType == PlayerBtn.RESUME) pathTransition.pause();
                rotateTransition.playFrom(newDuration);
                if(playType == PlayerBtn.RESUME) rotateTransition.pause();
                if(playType == PlayerBtn.PLAY) {
                    playType = PlayerBtn.RESUME;
                    playBtn.setGraphic(new Glyph("FontAwesome", "PAUSE"));
                }
            }
          });
          
        trajEditor.getPanes().add(new TitledPane("Constraints", constr = new VBox(
                new Label("Max Velocity m/s"), new NumberField("1.5"),
                new Label("Max Acceleration m/s^2"), new NumberField("1.5"),
                new Label("Reversed"), new CheckBox()
        )));
        trajEditor.getPanes().add(new TitledPane("Start Pose", startPos = new VBox(
                new Label("Start X"), new NumberField(),
                new Label("Start Y"), new NumberField(),
                new Label("Start Heading"), new NumberField()
        )));
        ScrollPane wpScroll;
        trajEditor.getPanes().add(new TitledPane("Interior Waypoints", wpScroll = new ScrollPane(
                intWP = new VBox(
                        wayPointBtn
                )
        )));
        trajEditor.getPanes().add(new TitledPane("End Pose", endPos = new VBox(
                new Label("End X"), new NumberField(),
                new Label("End Y"), new NumberField(),
                new Label("End Heading"), new NumberField()
        )));

        trajEditor.getPanes().add(new TitledPane("Robot", robotBox = new VBox(
                new Label("Length"), new NumberField("18.0"),
                new Label("Width"), new NumberField("18.0")
        )));

        for (int i = 1; i < startPos.getChildren().size(); i += 2) {
            NumberField startField = (NumberField) startPos.getChildren().get(i);
            NumberField endField = (NumberField) endPos.getChildren().get(i);

            int finalI = i;
            startField.textProperty().addListener((observable, oldValue, newValue) -> {
                double amnt = Double.parseDouble(newValue);
                if (finalI == 1)
                    startX.set(ExtraMath.clampNumField(startField, amnt));
                else if (finalI == 3)
                    startY.set(ExtraMath.clampNumField(startField, amnt));
                else if (finalI == 5)
                    startH.set(amnt);

                robot.setTranslateX(startRect.getTranslateX());
                robot.setTranslateY(startRect.getTranslateY());
                robot.setRotate(startRect.getRotate());

                drawPath();
            });

            endField.textProperty().addListener((observable, oldValue, newValue) -> {
                double amnt = Double.parseDouble(newValue);
                if (finalI == 1) {
                    endX.set(ExtraMath.clampNumField(endField, amnt));
                } else if (finalI == 3) {
                    endY.set(ExtraMath.clampNumField(endField, amnt));
                } else if (finalI == 5) {
                    endH.set(amnt);
                }
                drawPath();
            });
        }

        startRect.translateYProperty().bind(startX.multiply(-125.64918746859));
        startRect.translateXProperty().bind(startY.multiply(-125.64918746859));
        startRect.rotateProperty().bind(startH.multiply(-1.0));

        endRect.translateYProperty().bind(endX.multiply(-125.64918746859));
        endRect.translateXProperty().bind(endY.multiply(-125.64918746859));
        endRect.rotateProperty().bind(endH.multiply(-1.0));

        NumberField widthField = (NumberField) robotBox.getChildren().get(1);
        NumberField lengthField = (NumberField) robotBox.getChildren().get(3);

        widthField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (Double.parseDouble(newValue) > 1)
                robotWidth.set(ExtraMath.robotConv(Double.parseDouble(newValue)));
        });

        lengthField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (Double.parseDouble(newValue) > 1)
                robotHeight.set(ExtraMath.robotConv(Double.parseDouble(newValue)));
        });

        wpScroll.setFitToWidth(true);
        manageConstraints();
    }

    public Trajectory trajectory() {
        Pose2d start = new Pose2d(startX.get(), startY.get(), new Rotation2d(Math.toRadians(startH.get())));
        Pose2d end = new Pose2d(endX.get(), endY.get(), new Rotation2d(Math.toRadians(endH.get())));

        reversed = (CheckBox) constr.getChildren().get(5);

        TrajectoryConfig config = new TrajectoryConfig(maxVel.get(), maxAccel.get());
        config.setReversed(reversed.isSelected());

        return TrajectoryGenerator.generateTrajectory(start, interiorWaypoints, end, config);
    }

    @FXML
    protected void onFieldClick() {
        if (!isTrajCreated)
            return;
        System.out.println("Trajectory Time: " + trajectory().getTotalTimeSeconds());
        System.out.println("Cur Time: " + timeline.getCurrentTime());
        drawPath();
        if (timeline.getStatus() == Animation.Status.STOPPED)
            timeLabel.textProperty().bind(Bindings.format("%.2f Seconds", trajectory().getTotalTimeSeconds()));
    }

    @FXML
    protected void onFieldMouseHover(MouseEvent mouseEvent) {
        DecimalFormat coordFormat = new DecimalFormat("#.0000");
        coords.setText("Coords: (" +
                coordFormat.format(ExtraMath.unitsToMeters(mouseEvent.getY() - background.getFitWidth() / 2.0)) + " , "
                + coordFormat.format(ExtraMath.unitsToMeters(mouseEvent.getX() - background.getFitWidth() / 2.0)) + ")");
    }

    @FXML
    protected void onTrajCreateClick() {
        isTrajCreated = true;
        trajBox.getChildren().add(trajEditor);
        fieldPane.getChildren().add(2, startRect);
        fieldPane.getChildren().add(1, endRect);
        addTrajBtn.setVisible(false);
        btnBox.getChildren().add(playBtn);
        btnBox.getChildren().add(progressBar);
        btnBox.getChildren().add(timeLabel);

        robot.setPreserveRatio(false);
        robot.fitWidthProperty().bind(robotHeight);
        robot.fitHeightProperty().bind(robotWidth);
    }


    @FXML
    protected void onSaveClick() throws IOException {
        if (!isTrajCreated) {
            Notifications.create()
                    .title("Save Error")
                    .text("Why would you save nothing?")
                    .hideAfter(Duration.seconds(5))
                    .show();
            return;
        }
        File saveFile = fileChooser.showSaveDialog(canvas.getScene().getWindow());
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(saveFile));
        SaveManager saveManager = new SaveManager(saveFile);
        saveManager.writeMode(bufferedWriter);
        saveManager.writeDoubles(maxVel, maxAccel, robotHeight.divide(10.0 / 3.0), robotWidth.divide(10.0 / 3.0));
        saveManager.writeBools(((CheckBox) constr.getChildren().get(5)).isSelected());
        saveManager.writeDoubles(startX, startY, startH, endX, endY, endH);
        saveManager.writeWaypoints(interiorWaypoints);
        bufferedWriter.flush();
        bufferedWriter.close();
    }

    @FXML
    protected void onLoadClick() throws IOException {
        if (!isTrajCreated) {
            Notifications.create()
                    .title("Load Error")
                    .text("Please click Create Trajectory before loading a new one :)")
                    .hideAfter(Duration.seconds(5))
                    .show();
            return;
        }
        File loadFile = fileChooser.showOpenDialog(canvas.getScene().getWindow());
        FileReader fileReader = new FileReader(loadFile);
        SaveManager saveManager = new SaveManager(loadFile);
        saveManager.readMode(new BufferedReader(fileReader));
        saveManager.syncNumberFields((NumberField) constr.getChildren().get(1),
                (NumberField) constr.getChildren().get(3), (NumberField) robotBox.getChildren().get(1),
                (NumberField) robotBox.getChildren().get(3));
        saveManager.syncBools((CheckBox) constr.getChildren().get(5));
        saveManager.syncNumberFields(1, (NumberField) startPos.getChildren().get(1),
                (NumberField) startPos.getChildren().get(3), (NumberField) startPos.getChildren().get(5),
                (NumberField) endPos.getChildren().get(1), (NumberField) endPos.getChildren().get(3),
                (NumberField) endPos.getChildren().get(5));

        intWP.getChildren().clear();
        interiorWaypoints.clear();
        intWP.getChildren().add(wayPointBtn);
        if (tempPoints.size() > 0)
            fieldPane.getChildren().remove(1, tempPoints.size() + 1);
        tempPoints.clear();
        saveManager.syncWaypoints(tempPoints);
        for (Translation2d waypoint : tempPoints) {
            addWaypoint(waypoint.getX(), waypoint.getY());
        }

        fileReader.close();
        saveManager.closeScanner();
        drawPath();
    }

    @FXML
    protected void onReloadClick() throws IOException {
        Stage stage = (Stage) canvas.getScene().getWindow();
        stage.close();
        App visualizerApplication = new App();
        visualizerApplication.start(new Stage());
    }

    @FXML
    protected void javaExportClick() throws IOException {
        if (!isTrajCreated) {
            Notifications.create()
                    .title("Export Error")
                    .text("What are you even exporting?")
                    .hideAfter(Duration.seconds(5))
                    .show();
            return;
        }
        File saveFile = exportChooser.showSaveDialog(canvas.getScene().getWindow());
        FileWriter fileWriter = new FileWriter(saveFile);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.write("public class " + saveFile.getName().replaceAll(".java", "") + " {\n");
        bufferedWriter.write("    public static Trajectory trajectory() {\n" +
                "        Pose2d start = new Pose2d(" + startX.get() + ", " + startY.get() + ", new Rotation2d(Math.toRadians(" + startH.get() + ")));\n" +
                "        Pose2d end = new Pose2d(" + endX.get() + ", " + endY.get() + ", new Rotation2d(Math.toRadians(" + endH.get() + ")));\n" +
                "\n" +
                "        ArrayList<Translation2d> interiorWaypoints = new ArrayList<>();\n");
        for (Translation2d translation2d : interiorWaypoints) {
            bufferedWriter.write("        interiorWaypoints.add(" + translation2d.getX() + ", " + translation2d.getY() + ");\n");
        }
        bufferedWriter.write("\n" +
                "        TrajectoryConfig config = new TrajectoryConfig(" + maxVel.get() + ", " + maxAccel.get() + ");\n" +
                "        config.setReversed(" + reversed.isSelected() + ");\n" +
                "\n" +
                "        return TrajectoryGenerator.generateTrajectory(start, interiorWaypoints, end, config);\n" +
                "    }");
        bufferedWriter.newLine();
        bufferedWriter.write("}");
        bufferedWriter.flush();
        bufferedWriter.close();
    }

    private void addWaypoint(double xPos, double yPos) {
        Label wpNum = new Label("Waypoint " + (int) (intWP.getChildren().size() / 5.0 + 0.8));
        Button delBtn = new Button("", new Glyph("FontAwesome", "TRASH_ALT"));
        NumberField x = new NumberField("" + xPos);
        NumberField y = new NumberField("" + yPos);
        wpNum.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, Font.getDefault().getSize()));
        delBtn.setTranslateX(40);

        Waypoint waypoint = new Waypoint(x, y, interiorWaypoints);

        HBox waypointBox = new HBox(wpNum, delBtn);

        intWP.getChildren().add(waypointBox);
        intWP.getChildren().add(new Label("X"));
        intWP.getChildren().add(x);
        intWP.getChildren().add(new Label("Y"));
        intWP.getChildren().add(y);
        fieldPane.getChildren().add(1, waypoint);

        x.textProperty().addListener((observable, oldValue, newValue) -> drawPath());
        y.textProperty().addListener((observable, oldValue, newValue) -> drawPath());
        delBtn.setOnMouseClicked(mouseEvent -> {
            intWP.getChildren().remove(intWP.getChildren().indexOf(waypointBox), intWP.getChildren().indexOf(y) + 1);
            fieldPane.getChildren().remove(fieldPane.getChildren().indexOf(waypoint));
//            interiorWaypoints.remove(interiorWaypoints.indexOf(waypoint.getWaypoint()));
            interiorWaypoints.remove(waypoint.getWaypoint());
            int i = 0;
            for (Node hbox : intWP.getChildren()) {
                if (hbox instanceof HBox) {
                    Label wpID = (Label) ((HBox) hbox).getChildren().get(0);
                    wpID.setText("Waypoint " + (int) (i / 5.0 + 0.8));
                }
                i++;
            }
            drawPath();
        });
        drawPath();
    }

    public void manageConstraints() {
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

    public Polyline drawPath() {
        canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        int index = 0;
        double[] xArr = new double[(int) (trajectory().getTotalTimeSeconds() / 0.05)];
        double[] yArr = new double[(int) (trajectory().getTotalTimeSeconds() / 0.05)];
        ArrayList<Double> xyArr = new ArrayList<>();
        for (double sample = 0.05; sample < trajectory().getTotalTimeSeconds(); sample += 0.05) {
            xArr[index] = ExtraMath.metersToUnits(trajectory().sample(sample).poseMeters.getY()) + 225;
            yArr[index] = ExtraMath.metersToUnits(trajectory().sample(sample).poseMeters.getX()) + 225;
            xyArr.add(xArr[index] - 225 + robot.getFitWidth() / 2);
            xyArr.add(yArr[index] - 225 + robot.getFitWidth() / 2);
            index++;
        }
        Polyline polyline = new Polyline();
        polyline.getPoints().addAll(xyArr);

        canvas.getGraphicsContext2D().setLineWidth(4.0);
        canvas.getGraphicsContext2D().setStroke(Color.BLUE);
        canvas.getGraphicsContext2D().strokePolyline(xArr, yArr, xArr.length);

        return polyline;
    }

    public SequentialTransition createRotationList() {
        rotateList = new SequentialTransition();
        for (double sample = 0.01; sample < trajectory().getTotalTimeSeconds(); sample += 0.01) {
            RotateTransition tempTrans = new RotateTransition();
            tempTrans.setNode(robot);
            tempTrans.setDuration(Duration.seconds(0.01));
            tempTrans.setToAngle(-Math.toDegrees(trajectory().sample(sample).poseMeters.getHeading()));
            rotateList.getChildren().add(tempTrans);
        }
        return rotateList;
    }

    public void handlePlay() {
        switch (playType) {
            case PLAY: {
                if (timeline != null)
                    timeline.stop();
                if (trajectory().getTotalTimeSeconds() < 0.1)
                    break;
                timeLabel.textProperty().bind(Bindings.format("%.2f Seconds", time.multiply(trajectory().getTotalTimeSeconds())));
                time.set(0.0);
                timeline = new Timeline();
                timeline.getKeyFrames().add(
                        new KeyFrame(Duration.seconds(trajectory().getTotalTimeSeconds()),
                                new KeyValue(time, 1)
                        ));
                timeline.playFromStart();

                pathTransition.setNode(robot);
                pathTransition.setDuration(Duration.seconds(trajectory().getTotalTimeSeconds()));
                pathTransition.setPath(drawPath());
                rotateTransition = createRotationList();
                pathTransition.play();
                rotateTransition.play();

                playType = PlayerBtn.PAUSE;
                playBtn.setGraphic(new Glyph("FontAwesome", "PAUSE"));
                pathTransition.setOnFinished(onFinished -> {
                    playBtn.setGraphic(new Glyph("FontAwesome", "REPEAT"));
                    playType = PlayerBtn.PLAY;
                });
                break;
            }
            case PAUSE: {
                pathTransition.pause();
                rotateList.pause();
                timeline.pause();
                System.out.println("Paused");
                playType = PlayerBtn.RESUME;
                playBtn.setGraphic(new Glyph("FontAwesome", "PLAY"));
                break;
            }
            case RESUME: {
                pathTransition.play();
                rotateList.play();
                timeline.play();
                System.out.println("Resumed");
                playType = PlayerBtn.PAUSE;
                playBtn.setGraphic(new Glyph("FontAwesome", "PAUSE"));
                break;
            }
        }
    }

    public enum PlayerBtn {
        PLAY, PAUSE, RESUME
    }

}
package com.litehed;

import com.litehed.extraControls.NumberField;
import com.litehed.extraControls.TrajectoryPane;
import com.litehed.extraControls.Waypoint;
import com.litehed.ftclib.geometry.Translation2d;
import com.litehed.ftclib.trajectory.Trajectory;
import javafx.animation.*;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
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
    StackPane fieldPane;
    @FXML
    Label coords;
    @FXML
    Canvas canvas;

    private final DoubleProperty robotWidth = new SimpleDoubleProperty(60.0);
    private final DoubleProperty robotHeight = new SimpleDoubleProperty(60.0);
    private final PathTransition pathTransition;
    private SequentialTransition rotateTransition;
    private SequentialTransition rotateList;
    private final VBox robotBox;
    private final Rectangle endRect, startRect;
    private final Button playBtn;
    private PlayerBtn playType = PlayerBtn.PLAY;
    private Timeline timeline = new Timeline();
    private final ProgressBar progressBar;
    private final DoubleProperty time = new SimpleDoubleProperty(0.0);
    private final Label timeLabel = new Label();
    private FileChooser fileChooser;
    private FileChooser exportChooser;
    private ArrayList<Translation2d> tempPoints = new ArrayList<>();
    private TrajectoryManager trajectoryManager;

    public VisualizerController() {
        initializeFileChoosers();

        trajectoryManager = new TrajectoryManager(new TrajectoryPane(0));

        // Create start and end path indicators
        endRect = new Rectangle(robotHeight.get(), robotWidth.get());
        startRect = new Rectangle(robotHeight.get(), robotWidth.get());
        endRect.widthProperty().bind(robotHeight);
        endRect.heightProperty().bind(robotWidth);
        startRect.widthProperty().bind(robotHeight);
        startRect.heightProperty().bind(robotWidth);
        endRect.setOpacity(0.3);
        startRect.setOpacity(0.3);

        // Initialize the transitions to visualize robot movement
        pathTransition = new PathTransition();
        rotateList = new SequentialTransition();
        playBtn = new Button("", new Glyph("FontAwesome", "PLAY"));
        playBtn.setOnMouseClicked(mouseEvent -> handlePlay());

        // Create progress bar to control and view where along the path the robot is
        progressBar = new ProgressBar();
        progressBar.progressProperty().bind(time);
        progressBar.setTranslateX(10.0);
        progressBar.setTranslateY(5.0);
        progressBar.setPrefWidth(350.0);
        timeLabel.setTranslateX(12.0);
        timeLabel.setTranslateY(5.0);

        progressBar.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            Trajectory trajectoryTest = trajectoryManager.getTrajectoryAt(0).getTrajectory();
            double newProgress = event.getX() / progressBar.getWidth();
            Duration newDuration = Duration.seconds(newProgress * trajectoryTest.getTotalTimeSeconds());
            time.set(newProgress);
            timeline.playFrom(newDuration);
            if (playType == PlayerBtn.RESUME)
                timeline.pause();
            pathTransition.playFrom(newDuration);
            if (playType == PlayerBtn.RESUME)
                pathTransition.pause();
            rotateTransition.playFrom(newDuration);
            if (playType == PlayerBtn.RESUME)
                rotateTransition.pause();
            if (playType == PlayerBtn.PLAY) {
                playType = PlayerBtn.RESUME;
                playBtn.setGraphic(new Glyph("FontAwesome", "PAUSE"));
            }

        });

        // Robot Size Management
        robotBox = new VBox(
                new Label("Length (in)"), new NumberField("18.0"),
                new Label("Width (in)"), new NumberField("18.0"));

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

    }

    private void initializeFileChoosers() {
        fileChooser = new FileChooser();
        fileChooser.setInitialFileName("trajectory");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("poggers", "*.pog"));

        exportChooser = new FileChooser();
        exportChooser.setInitialFileName("Trajectory");
        exportChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Java", "*.java"));
    }

    @FXML
    public void initialize() {
        Accordion actions = new Accordion();
        TrajectoryPane trajectoryPane = new TrajectoryPane(0);

        trajectoryPane.getWayPointBtn().setOnMouseClicked(mouseEvent -> addWaypoint(0.0, 0.0));
        actions.getPanes().add(new TitledPane("Robot", robotBox));
        actions.getPanes().add(trajectoryPane);
        trajBox.getChildren().add(actions);
        fieldPane.getChildren().add(2, startRect);
        fieldPane.getChildren().add(1, endRect);
        btnBox.getChildren().add(playBtn);
        btnBox.getChildren().add(progressBar);
        btnBox.getChildren().add(timeLabel);

        robot.setPreserveRatio(false);
        robot.fitWidthProperty().bind(robotHeight);
        robot.fitHeightProperty().bind(robotWidth);
        Button addAfterAction = new Button("Add Action");
        addAfterAction.setOnMouseClicked(event -> {
            TrajectoryPane tempPane = createTrajPane();
            actions.getPanes().add(tempPane);
            createPosListeners(tempPane);
        });

        createPosListeners(trajectoryPane);
        trajectoryManager.setTrajectoryAt(0, trajectoryPane);
        trajBox.getChildren().add(addAfterAction);

        // Bind conversions
        // DoubleProperty[] startPosArr = trajectoryManager.getTrajectoryAt(0).startX
        startRect.translateYProperty()
                .bind(trajectoryManager.getTrajectoryAt(0).startX.multiply(-Constants.PIXELS_PER_METER));
        startRect.translateXProperty()
                .bind(trajectoryManager.getTrajectoryAt(0).startY.multiply(-Constants.PIXELS_PER_METER));
        startRect.rotateProperty().bind(trajectoryManager.getTrajectoryAt(0).startH.multiply(-1.0));

        endRect.translateYProperty()
                .bind(trajectoryManager.getTrajectoryAt(0).endX.multiply(-Constants.PIXELS_PER_METER));
        endRect.translateXProperty()
                .bind(trajectoryManager.getTrajectoryAt(0).endY.multiply(-Constants.PIXELS_PER_METER));
        endRect.rotateProperty().bind(trajectoryManager.getTrajectoryAt(0).endH.multiply(-1.0));
    }

    private void createPosListeners(TrajectoryPane trajectory) {

        for (int i = 1; i < trajectory.getStartPos().getChildren().size(); i += 2) {
            NumberField startField = (NumberField) trajectory.getStartPos().getChildren().get(i);
            NumberField endField = (NumberField) trajectory.getEndPos().getChildren().get(i);

            int finalI = i;
            startField.textProperty().addListener((observable, oldValue, newValue) -> {
                double amnt = Double.parseDouble(newValue);
                if (finalI == 1)
                    trajectory.startX.set(ExtraMath.clampNumField(startField, amnt));
                else if (finalI == 3)
                    trajectory.startY.set(ExtraMath.clampNumField(startField, amnt));
                else if (finalI == 5)
                    trajectory.startH.set(amnt);

                robot.setTranslateX(startRect.getTranslateX());
                robot.setTranslateY(startRect.getTranslateY());
                robot.setRotate(startRect.getRotate());

                drawPath();
            });

            endField.textProperty().addListener((observable, oldValue, newValue) -> {
                double amnt = Double.parseDouble(newValue);
                if (finalI == 1) {
                    trajectory.endX.set(ExtraMath.clampNumField(endField, amnt));
                } else if (finalI == 3) {
                    trajectory.endY.set(ExtraMath.clampNumField(endField, amnt));
                } else if (finalI == 5) {
                    trajectory.endH.set(amnt);
                }
                drawPath();
            });
        }

        trajectory.getConstr().getChildren().get(5).setOnMouseClicked(event -> drawPath());
    }

    public TrajectoryPane createTrajPane() {
        int trajID = trajectoryManager.getTrajAmount();
        TrajectoryPane trajPane = new TrajectoryPane(trajID);
        trajectoryManager.addTrajectory(trajPane);
        return trajPane;
    }

    @FXML
    protected void onFieldClick() {
        Trajectory trajectoryTest = trajectoryManager.getTrajectoryAt(0).getTrajectory();
        System.out.println("Trajectory Time: " + trajectoryTest.getTotalTimeSeconds());
        System.out.println("Cur Time: " + timeline.getCurrentTime());
        drawPath();
        if (timeline.getStatus() == Animation.Status.STOPPED)
            timeLabel.textProperty().bind(Bindings.format("%.2f Seconds", trajectoryTest.getTotalTimeSeconds()));
    }

    @FXML
    protected void onFieldMouseHover(MouseEvent mouseEvent) {
        DecimalFormat coordFormat = new DecimalFormat("#.0000");
        coords.setText("Coords: (" +
                coordFormat.format(ExtraMath.unitsToMeters(mouseEvent.getY() - background.getFitWidth() / 2.0)) + " , "
                + coordFormat.format(ExtraMath.unitsToMeters(mouseEvent.getX() - background.getFitWidth() / 2.0))
                + ")");
    }

    @FXML
    protected void onSaveClick() throws IOException {
        DoubleProperty[] constraints = trajectoryManager.getTrajectoryAt(0).getConstraintsArray();
        DoubleProperty[] startPosArr = trajectoryManager.getTrajectoryAt(0).getStartPosArray();
        DoubleProperty[] endPosArr = trajectoryManager.getTrajectoryAt(0).getEndPosArray();
        ArrayList<Translation2d> interiorWaypoints = trajectoryManager.getTrajectoryAt(0).getInteriorWaypoints();

        VBox constr = trajectoryManager.getTrajectoryAt(0).getConstr();
        File saveFile = fileChooser.showSaveDialog(canvas.getScene().getWindow());
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(saveFile));
        SaveManager saveManager = new SaveManager(saveFile);
        saveManager.writeMode(bufferedWriter);
        saveManager.writeDoubles(constraints[0], constraints[1], robotHeight.divide(10.0 / 3.0),
                robotWidth.divide(10.0 / 3.0));
        saveManager.writeBools(((CheckBox) constr.getChildren().get(5)).isSelected());
        saveManager.writeDoubles(startPosArr[0], startPosArr[1], startPosArr[2], endPosArr[0], endPosArr[1],
                endPosArr[2]);
        saveManager.writeWaypoints(interiorWaypoints);
        bufferedWriter.flush();
        bufferedWriter.close();
    }

    @FXML
    protected void onLoadClick() throws IOException {
        VBox constr = trajectoryManager.getTrajectoryAt(0).getConstr();
        VBox intWP = trajectoryManager.getTrajectoryAt(0).getIntWP();
        VBox startPos = trajectoryManager.getTrajectoryAt(0).getStartPos();
        VBox endPos = trajectoryManager.getTrajectoryAt(0).getEndPos();
        Button wayPointBtn = trajectoryManager.getTrajectoryAt(0).getWayPointBtn();

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
        trajectoryManager.getTrajectoryAt(0).clearWaypoints();
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
        DoubleProperty[] constraints = trajectoryManager.getTrajectoryAt(0).getConstraintsArray();
        DoubleProperty[] startPosArr = trajectoryManager.getTrajectoryAt(0).getStartPosArray();
        DoubleProperty[] endPosArr = trajectoryManager.getTrajectoryAt(0).getEndPosArray();
        ArrayList<Translation2d> interiorWaypoints = trajectoryManager.getTrajectoryAt(0).getInteriorWaypoints();

        // if (!isInitialized) {
        // Notifications.create()
        // .title("Export Error")
        // .text("What are you even exporting?")
        // .hideAfter(Duration.seconds(5))
        // .show();
        // return;
        // }
        File saveFile = exportChooser.showSaveDialog(canvas.getScene().getWindow());
        FileWriter fileWriter = new FileWriter(saveFile);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.write("public class " + saveFile.getName().replaceAll(".java", "") + " {\n");
        bufferedWriter.write("    public static Trajectory trajectory() {\n" +
                "        Pose2d start = new Pose2d(" + startPosArr[0].get() + ", " + startPosArr[1].get()
                + ", new Rotation2d(Math.toRadians(" + startPosArr[2].get() + ")));\n" +
                "        Pose2d end = new Pose2d(" + endPosArr[0].get() + ", " + endPosArr[1].get()
                + ", new Rotation2d(Math.toRadians("
                + endPosArr[2].get() + ")));\n" +
                "\n" +
                "        ArrayList<Translation2d> interiorWaypoints = new ArrayList<>();\n");
        for (Translation2d translation2d : interiorWaypoints) {
            bufferedWriter.write(
                    "        interiorWaypoints.add(" + translation2d.getX() + ", " + translation2d.getY() + ");\n");
        }
        bufferedWriter.write("\n" +
                "        TrajectoryConfig config = new TrajectoryConfig(" + constraints[0].get() + ", "
                + constraints[1].get()
                + ");\n" +
                "        config.setReversed(" + trajectoryManager.getTrajectoryAt(0).isReversed() + ");\n" +
                "\n" +
                "        return TrajectoryGenerator.generateTrajectory(start, interiorWaypoints, end, config);\n" +
                "    }");
        bufferedWriter.newLine();
        bufferedWriter.write("}");
        bufferedWriter.flush();
        bufferedWriter.close();
    }

    public void addWaypoint(double xPos, double yPos) {
        ArrayList<Translation2d> interiorWaypoints = trajectoryManager.getTrajectoryAt(0).getInteriorWaypoints();
        VBox intWP = trajectoryManager.getTrajectoryAt(0).getIntWP();

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
            // interiorWaypoints.remove(interiorWaypoints.indexOf(waypoint.getWaypoint()));
            trajectoryManager.getTrajectoryAt(0).removeWaypoint(waypoint.getWaypoint());
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

    public Polyline drawPath() {

        Trajectory trajectoryTest = trajectoryManager.getTrajectoryAt(0).getTrajectory();

        canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        int index = 0;
        double[] xArr = new double[(int) (trajectoryTest.getTotalTimeSeconds() / 0.05)];
        double[] yArr = new double[(int) (trajectoryTest.getTotalTimeSeconds() / 0.05)];
        ArrayList<Double> xyArr = new ArrayList<>();
        for (double sample = 0.05; sample < trajectoryTest.getTotalTimeSeconds(); sample += 0.05) {
            xArr[index] = ExtraMath.metersToUnits(trajectoryTest.sample(sample).poseMeters.getY()) + 225;
            yArr[index] = ExtraMath.metersToUnits(trajectoryTest.sample(sample).poseMeters.getX()) + 225;
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
        Trajectory trajectoryTest = trajectoryManager.getTrajectoryAt(0).getTrajectory();

        rotateList = new SequentialTransition();
        for (double sample = 0.01; sample < trajectoryTest.getTotalTimeSeconds(); sample += 0.01) {
            RotateTransition tempTrans = new RotateTransition();
            tempTrans.setNode(robot);
            tempTrans.setDuration(Duration.seconds(0.01));
            tempTrans.setToAngle(-Math.toDegrees(trajectoryTest.sample(sample).poseMeters.getHeading()));
            rotateList.getChildren().add(tempTrans);
        }
        return rotateList;
    }

    public void handlePlay() {
        Trajectory trajectoryTest = trajectoryManager.getTrajectoryAt(0).getTrajectory();

        switch (playType) {
            case PLAY: {
                if (timeline != null)
                    timeline.stop();
                if (trajectoryTest.getTotalTimeSeconds() < 0.1)
                    break;
                timeLabel.textProperty()
                        .bind(Bindings.format("%.2f Seconds", time.multiply(trajectoryTest.getTotalTimeSeconds())));
                time.set(0.0);
                timeline = new Timeline();
                timeline.getKeyFrames().add(
                        new KeyFrame(Duration.seconds(trajectoryTest.getTotalTimeSeconds()),
                                new KeyValue(time, 1)));
                timeline.playFromStart();

                pathTransition.setNode(robot);
                pathTransition.setDuration(Duration.seconds(trajectoryTest.getTotalTimeSeconds()));
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
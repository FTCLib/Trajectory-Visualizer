module com.litehed.ftclibpathvis {
    requires javafx.controls;
    requires javafx.fxml;
    requires ejml.simple;
    requires ejml.core;

    requires org.controlsfx.controls;

    opens com.litehed.trajectoryvis to javafx.fxml;
    exports com.litehed.trajectoryvis;
    exports com.litehed.trajectoryvis.extraControls;
    opens com.litehed.trajectoryvis.extraControls to javafx.fxml;
}
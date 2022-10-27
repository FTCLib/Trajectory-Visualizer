module com.litehed {
    requires transitive javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;

    requires org.controlsfx.controls;
    
    requires ejml.simple;
    requires ejml.core;

    opens com.litehed to javafx.fxml;
    exports com.litehed;
    exports com.litehed.extraControls;

    exports com.litehed.ftclib.controller;
    exports com.litehed.ftclib.geometry;
    exports com.litehed.ftclib.kinematics;
    exports com.litehed.ftclib.spline;
    exports com.litehed.ftclib.trajectory;
    exports com.litehed.ftclib.trajectory.constraint;

    opens com.litehed.extraControls to javafx.fxml;
}

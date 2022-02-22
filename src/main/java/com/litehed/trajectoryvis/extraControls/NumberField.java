package com.litehed.trajectoryvis.extraControls;

import javafx.scene.control.TextField;

public class NumberField extends TextField {

    public NumberField() {
        super("0.0");
        listener();
    }

    public NumberField(String num) {
        super(num);
        listener();
    }

    public void listener() {
        this.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("-?\\d{0,7}([\\.]\\d{0,4})?")) {
                setText(oldValue);
            }
        });
    }

}

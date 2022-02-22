package com.litehed.trajectoryvis;

import com.litehed.trajectoryvis.extraControls.NumberField;

public final class ExtraMath {

    public static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    public static double clampNumField(NumberField numField, double amnt) {
        if (amnt > 1.7907 || amnt < -1.7907) {
            double num = (amnt < 0) ? -1.7907 : 1.7907;
            numField.setText("" + num);
            return num;
        }
        return amnt;
    }

    public static double metersToUnits(double meters) {
        return -125.64918746859 * meters;
    }

    public static double unitsToMeters(double units) {
        return -0.0079586666666667 * units;
    }

    public static double robotConv(double size) {
        return size * 3.333;
    }
}

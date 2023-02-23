package com.litehed;

import com.litehed.extraControls.NumberField;

public final class ExtraMath {

    public static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    public static double clampNumField(NumberField numField, double amnt) {
        if (amnt > Constants.MAX_FIELD_METERS || amnt < Constants.MIN_FIELD_METERS) {
            double num = (amnt < 0) ? Constants.MIN_FIELD_METERS : Constants.MAX_FIELD_METERS;
            numField.setText("" + num);
            return num;
        }
        return amnt;
    }

    public static double metersToUnits(double meters) {
        return -Constants.PIXELS_PER_METER * meters;
    }

    public static double unitsToMeters(double units) {
        return -Constants.METERS_PER_PIXEL * units;
    }

    public static double robotConv(double size) {
        return size * 3.333;
    }
}

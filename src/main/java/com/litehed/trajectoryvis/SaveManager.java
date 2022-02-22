package com.litehed.trajectoryvis;

import com.litehed.trajectoryvis.extraControls.NumberField;
import com.litehed.trajectoryvis.ftclib.geometry.Translation2d;
import javafx.beans.binding.DoubleExpression;
import javafx.scene.control.CheckBox;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class SaveManager {

    private File file;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private Scanner scan;
    private int curVar = 0;

    public SaveManager(File file) throws FileNotFoundException {
        this.file = file;
        scan = new Scanner(file);
    }

    public void syncNumberFields(NumberField... numberFields) {
        int i = 0;
        while (scan.hasNextDouble()) {
            double temp = scan.nextDouble();
            numberFields[i].textProperty().set("" + temp);
            i++;
        }
    }

    public void syncNumberFields(int line, NumberField... numberFields) {
        for (int i = 0; i < line; i++) {
            System.out.println(scan.nextLine());
        }
        int numFieldValue = 0;
        while (scan.hasNextDouble()) {
            double temp = scan.nextDouble();
            numberFields[numFieldValue].textProperty().set("" + temp);
            numFieldValue++;
        }
    }

    public void syncBools(CheckBox... checkBox) {
        System.out.println(scan.nextLine());
        int numFieldValue = 0;
        while (scan.hasNextDouble()) {
            boolean temp = scan.nextBoolean();
            checkBox[numFieldValue].selectedProperty().set(temp);
            numFieldValue++;
        }
    }

    public void syncWaypoints(ArrayList<Translation2d> waypoints) {
        scan.nextLine();
        System.out.println(scan.next());
        while (scan.hasNextDouble()) {
            waypoints.add(new Translation2d(scan.nextDouble(), scan.nextDouble()));
        }
    }

    public void readMode(BufferedReader bufferedReader) {
        this.bufferedReader = bufferedReader;
    }

    public void writeMode(BufferedWriter bufferedWriter) {
        this.bufferedWriter = bufferedWriter;
    }

    public void writeDoubles(DoubleExpression... vars) throws IOException {
        for (DoubleExpression var : vars) {
            bufferedWriter.write(var.get() + " ");
        }
        bufferedWriter.newLine();
    }

    public void writeWaypoints(ArrayList<Translation2d> waypoints) throws IOException {
        bufferedWriter.write("Waypoints");
        bufferedWriter.newLine();
        for (Translation2d wp : waypoints) {
            bufferedWriter.write(wp.getX() + " " + wp.getY() + " ");
        }
        bufferedWriter.newLine();
    }

    public void writeBools(boolean... vars) throws IOException {
        for (boolean var : vars) {
            bufferedWriter.write(var + " ");
        }
        bufferedWriter.newLine();
    }

    public void closeScanner() {
        scan.close();
    }
}

package com.litehed;

import java.util.ArrayList;

import com.litehed.extraControls.TrajectoryPane;

public class TrajectoryManager {

    private ArrayList<TrajectoryPane> trajectories;
    
    public TrajectoryManager() {
        trajectories = new ArrayList<>();
    }

    public TrajectoryManager(TrajectoryPane trajectoryPane) {
        trajectories = new ArrayList<>();
        trajectories.add(trajectoryPane);
    }

    public void addTrajectory(TrajectoryPane trajectory) {
        trajectories.add(trajectory);
    }

    public void removeTrajectory(TrajectoryPane trajectory) {
        trajectories.remove(trajectory);
    }

    public TrajectoryPane getTrajectoryAt(int index) {
        return trajectories.get(index);
    }

    public void setTrajectoryAt(int index, TrajectoryPane trajectory) {
        trajectories.set(index, trajectory);
    }

    public int getTrajAmount() {
        return trajectories.size();
    }

}

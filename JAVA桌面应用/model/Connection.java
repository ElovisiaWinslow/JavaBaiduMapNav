package model;

import java.io.Serializable;

public class Connection implements Serializable {
    public String fromStation;
    public String toStation;
    public String lineName;
    public int timeCost;

    public Connection(String from, String to, String line, int time) {
        this.fromStation = from;
        this.toStation = to;
        this.lineName = line;
        this.timeCost = time;
    }
}
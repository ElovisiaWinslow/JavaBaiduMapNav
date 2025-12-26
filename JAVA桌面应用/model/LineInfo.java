package model;

import java.io.Serializable;

public class LineInfo implements Serializable {
    public String lineName;
    public String firstTime;
    public String lastTime;

    public LineInfo(String lineName, String firstTime, String lastTime) {
        this.lineName = lineName;
        this.firstTime = firstTime;
        this.lastTime = lastTime;
    }
}
package model;

public class GeoCoordinate {
    public double longitude;
    public double latitude;
    
    public GeoCoordinate(double longitude, double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }
    
    @Override
    public String toString() {
        return String.format("(%.6f, %.6f)", longitude, latitude);
    }
}
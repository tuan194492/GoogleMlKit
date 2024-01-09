package com.google.mlkit.vision.demo.java.custom;// FaceData.java

public class FaceData {
    private String name;
    private String faceMeshPoints;

    public FaceData(String name, String faceMeshPoints) {
        this.name = name;
        this.faceMeshPoints = faceMeshPoints;
    }

    public String getName() {
        return name;
    }

    public String getFaceMeshPoints() {
        return faceMeshPoints;
    }
}

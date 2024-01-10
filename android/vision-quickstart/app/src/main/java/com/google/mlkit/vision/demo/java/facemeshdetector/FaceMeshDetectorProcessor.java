/*
 * Copyright 2022 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mlkit.vision.demo.java.facemeshdetector;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Task;
import com.google.android.odml.image.MlImage;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.demo.GraphicOverlay;
import com.google.mlkit.vision.demo.java.VisionProcessorBase;
import com.google.mlkit.vision.demo.java.custom.DatabaseHelper;
import com.google.mlkit.vision.demo.java.custom.FaceData;
import com.google.mlkit.vision.demo.preference.PreferenceUtils;
import com.google.mlkit.vision.facemesh.FaceMesh;
import com.google.mlkit.vision.facemesh.FaceMeshDetection;
import com.google.mlkit.vision.facemesh.FaceMeshDetector;
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions;
import com.google.mlkit.vision.facemesh.FaceMeshPoint;

import java.util.ArrayList;
import java.util.List;

/** Selfie Face Detector Demo. */
public class FaceMeshDetectorProcessor extends VisionProcessorBase<List<FaceMesh>> {

  private static final String TAG = "SelfieFaceProcessor";

  private ArrayList<FaceData> faceData;


  private final FaceMeshDetector detector;

  public FaceMeshDetectorProcessor(Context context) {
    super(context);
    FaceMeshDetectorOptions.Builder optionsBuilder = new FaceMeshDetectorOptions.Builder();
    if (PreferenceUtils.getFaceMeshUseCase(context) == FaceMeshDetectorOptions.BOUNDING_BOX_ONLY) {
      optionsBuilder.setUseCase(FaceMeshDetectorOptions.BOUNDING_BOX_ONLY);
    }

    detector = FaceMeshDetection.getClient(optionsBuilder.build());
  }

  private boolean compareVectors(ArrayList<Double> v1, ArrayList<Double> v2, double delta) {
    double count = 0;
//                                    Log.d("TAG", "compareVectors: "+ v1.size() +" "+ v2.size());
    for (int i = 0; i < v1.size(); i++) {
      if (Math.abs(v1.get(i) - v2.get(i)) > delta) {
        count +=1;
      }
    }
    if(count/36 < 0.4){
      return true;
    }
//    Log.d("TAG", "compareVectorss: "+ count);

    return false;
  }
  private ArrayList<Double> stringToArrayList(String inputString) {
    ArrayList<Double> doubleArrayList = new ArrayList<>();
    // Convert the array to an ArrayList
    String[] stringNumbersArray = inputString.substring(1, inputString.length() - 1).split(",");

    for (String stringNumber : stringNumbersArray) {
      double doubleNumber = Double.parseDouble(stringNumber.trim());
      doubleArrayList.add(doubleNumber);
    }

    return doubleArrayList;
  }
  private double distance(FaceMeshPoint point1, FaceMeshPoint point2){
    float x1 = point1.getPosition().getX();
    float y1 = point1.getPosition().getY();
    float z1 = point1.getPosition().getZ();

    float x2 = point2.getPosition().getX();
    float y2 = point2.getPosition().getY();
    float z2 = point2.getPosition().getZ();

    return Math.sqrt(Math.pow((x1-x2), 2) + Math.pow((y1-y2), 2) + Math.pow((z1-z2), 2));
  }
  @Override
  public void stop() {
    super.stop();
    detector.close();
  }

  @Override
  protected Task<List<FaceMesh>> detectInImage(InputImage image) {
    return detector.process(image);
  }

  @Override
  protected void onSuccess(
      @NonNull List<FaceMesh> faces, @NonNull GraphicOverlay graphicOverlay) {
    graphicOverlay.setFaceMeshesList(faces);

    double delta = 1;

    DatabaseHelper dbHelper = new DatabaseHelper(graphicOverlay.getContext());
    faceData = dbHelper.getAllFaceData();
    for (FaceMesh faceMesh : faces) {
      ArrayList<Double> distanceList = new ArrayList<>();
      List<FaceMeshPoint> faceMeshPoints = new ArrayList<>();
      FaceMeshPoint rootPoint = faceMesh.getPoints(12).get(3);
      for (int i=1;i<=12;i++){
        int len = faceMesh.getPoints(i).size();
        faceMeshPoints.add(faceMesh.getPoints(i).get(0));
        faceMeshPoints.add(faceMesh.getPoints(i).get(len - 1));
      }
      int l = faceMeshPoints.size();
      for(int i=0; i<l;i+=2){
        distanceList.add(this.distance(faceMeshPoints.get(i), faceMeshPoints.get(i+1)));
        distanceList.add(this.distance(faceMeshPoints.get(i), rootPoint));
        distanceList.add(this.distance(faceMeshPoints.get(i+1), rootPoint));
      }
//                                        Log.d("D", "onSuccess: " + distanceList.toString());
      for(FaceData face : faceData){
        String distanceString = face.getFaceMeshPoints();
        ArrayList<Double>distance = this.stringToArrayList(distanceString);
        if(this.compareVectors(distanceList, distance, delta)){
          Log.d("Z", "tenjsid" + face.getName());
        }
        else {
          Log.d("Z", "Khong biet ai ca");
        }
      }
      graphicOverlay.add(new FaceMeshGraphic(graphicOverlay, faceMesh));
    }

  }

  @Override
  protected void onFailure(@NonNull Exception e) {
    Log.e(TAG, "Face detection failed " + e);
  }
}

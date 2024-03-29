/*
 * Copyright 2020 Google LLC. All rights reserved.
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

package com.google.mlkit.vision.demo.java;

import android.content.Intent;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory;
import com.google.android.gms.common.annotation.KeepName;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.barcode.ZoomSuggestionOptions.ZoomCallback;
import com.google.mlkit.vision.demo.CameraXViewModel;
import com.google.mlkit.vision.demo.GraphicOverlay;
import com.google.mlkit.vision.demo.R;
import com.google.mlkit.vision.demo.VisionImageProcessor;
import com.google.mlkit.vision.demo.java.barcodescanner.BarcodeScannerProcessor;
import com.google.mlkit.vision.demo.java.facedetector.FaceDetectorProcessor;
import com.google.mlkit.vision.demo.java.facemeshdetector.FaceMeshDetectorProcessor;
import com.google.mlkit.vision.demo.java.labeldetector.LabelDetectorProcessor;
import com.google.mlkit.vision.demo.java.objectdetector.ObjectDetectorProcessor;
import com.google.mlkit.vision.demo.java.posedetector.PoseDetectorProcessor;
import com.google.mlkit.vision.demo.java.segmenter.SegmenterProcessor;
import com.google.mlkit.vision.demo.java.textdetector.TextRecognitionProcessor;
import com.google.mlkit.vision.demo.preference.PreferenceUtils;
import com.google.mlkit.vision.demo.preference.SettingsActivity;
import com.google.mlkit.vision.facemesh.FaceMesh;
import com.google.mlkit.vision.facemesh.FaceMeshPoint;
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import com.google.mlkit.vision.pose.PoseDetectorOptionsBase;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.ArrayList;
import java.util.List;

/** Live preview demo app for ML Kit APIs using CameraX. */
@KeepName
@RequiresApi(VERSION_CODES.LOLLIPOP)
public final class CameraXLivePreviewActivity extends AppCompatActivity
    implements OnItemSelectedListener, CompoundButton.OnCheckedChangeListener {
  private static final String TAG = "CameraXLivePreview";

  private static final String OBJECT_DETECTION = "Object Detection";
  private static final String OBJECT_DETECTION_CUSTOM = "Custom Object Detection";
  private static final String CUSTOM_AUTOML_OBJECT_DETECTION =
      "Custom AutoML Object Detection (Flower)";
  private static final String FACE_DETECTION = "Face Detection";
  private static final String BARCODE_SCANNING = "Barcode Scanning";
  private static final String IMAGE_LABELING = "Image Labeling";
  private static final String IMAGE_LABELING_CUSTOM = "Custom Image Labeling (Birds)";
  private static final String CUSTOM_AUTOML_LABELING = "Custom AutoML Image Labeling (Flower)";
  private static final String POSE_DETECTION = "Pose Detection";
  private static final String SELFIE_SEGMENTATION = "Selfie Segmentation";
  private static final String TEXT_RECOGNITION_LATIN = "Text Recognition Latin";
  private static final String TEXT_RECOGNITION_CHINESE = "Text Recognition Chinese";
  private static final String TEXT_RECOGNITION_DEVANAGARI = "Text Recognition Devanagari";
  private static final String TEXT_RECOGNITION_JAPANESE = "Text Recognition Japanese";
  private static final String TEXT_RECOGNITION_KOREAN = "Text Recognition Korean";
  private static final String FACE_MESH_DETECTION = "Face Mesh Detection (Beta)";

  private static final String STATE_SELECTED_MODEL = "selected_model";

  private PreviewView previewView;
  private GraphicOverlay graphicOverlay;

  private Button registerButton;


  private List<FaceMesh> faceMeshRegisterList;

  public List<FaceMesh> getFaceMeshRegisterList() {
    if (this.faceMeshRegisterList == null) {
      this.faceMeshRegisterList = new ArrayList<>();
    }
    return faceMeshRegisterList;
  }

  public void setFaceMeshRegisterList(List<FaceMesh> faceMeshRegisterList) {
    this.faceMeshRegisterList = faceMeshRegisterList;
  }

  private Button saveImageButton;
  private Button finishRegisterButton;

  @Nullable private ProcessCameraProvider cameraProvider;
  @Nullable private Camera camera;
  @Nullable private Preview previewUseCase;
  @Nullable private ImageAnalysis analysisUseCase;
  @Nullable private VisionImageProcessor imageProcessor;
  private boolean needUpdateGraphicOverlayImageSourceInfo;

  private String selectedModel = OBJECT_DETECTION;
  private int lensFacing = CameraSelector.LENS_FACING_BACK;
  private CameraSelector cameraSelector;

  private void onClickRecord(View view) {
    List<FaceMesh> faceMeshes = this.graphicOverlay.getFaceMeshesList();
    if (faceMeshes != null) {
      ArrayList<ArrayList<Float>> faceMeshPointList = new ArrayList<>();
      ArrayList<Double> distanceList = new ArrayList<>();

      for (FaceMesh faceMesh : faceMeshes) {
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
      }
      Log.d("Z", "onClickRecord: " + distanceList.toString());
      Intent intent = new Intent(CameraXLivePreviewActivity.this, RegisterActivity.class);
      Bundle bundle = new Bundle();
      bundle.putString("faceMeshPoints", distanceList.toString());
      intent.putExtras(bundle);
      startActivity(intent);
    }
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
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate");

    if (savedInstanceState != null) {
      selectedModel = savedInstanceState.getString(STATE_SELECTED_MODEL, OBJECT_DETECTION);
    }
    cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();

    setContentView(R.layout.activity_vision_camerax_live_preview);
    previewView = findViewById(R.id.preview_view);
    if (previewView == null) {
      Log.d(TAG, "previewView is null");
    }
    graphicOverlay = findViewById(R.id.graphic_overlay);
    if (graphicOverlay == null) {
      Log.d(TAG, "graphicOverlay is null");
    }

    registerButton = findViewById(R.id.register_button);
    finishRegisterButton = findViewById(R.id.finish_register_button);
    saveImageButton = findViewById(R.id.save_image_button);

    registerButton.setOnClickListener(this::onClickBeginRegister);
    saveImageButton.setOnClickListener(this::onSaveImage);
    finishRegisterButton.setOnClickListener(this::onClickFinishRegister);

    this.currentState = State.Processing;
    this.onChangeState();

    Spinner spinner = findViewById(R.id.spinner);
    List<String> options = new ArrayList<>();
//    options.add(OBJECT_DETECTION);
//    options.add(OBJECT_DETECTION_CUSTOM);
//    options.add(CUSTOM_AUTOML_OBJECT_DETECTION);
//    options.add(FACE_DETECTION);
//    options.add(BARCODE_SCANNING);
//    options.add(IMAGE_LABELING);
//    options.add(IMAGE_LABELING_CUSTOM);
//    options.add(CUSTOM_AUTOML_LABELING);
//    options.add(POSE_DETECTION);
//    options.add(SELFIE_SEGMENTATION);
//    options.add(TEXT_RECOGNITION_LATIN);
//    options.add(TEXT_RECOGNITION_CHINESE);
//    options.add(TEXT_RECOGNITION_DEVANAGARI);
//    options.add(TEXT_RECOGNITION_JAPANESE);
//    options.add(TEXT_RECOGNITION_KOREAN);
    options.add(FACE_MESH_DETECTION);

    // Creating adapter for spinner
    ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, R.layout.spinner_style, options);
    // Drop down layout style - list view with radio button
    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    // attaching data adapter to spinner
    spinner.setAdapter(dataAdapter);
    spinner.setOnItemSelectedListener(this);

    ToggleButton facingSwitch = findViewById(R.id.facing_switch);
    facingSwitch.setOnCheckedChangeListener(this);

    new ViewModelProvider(this, AndroidViewModelFactory.getInstance(getApplication()))
        .get(CameraXViewModel.class)
        .getProcessCameraProvider()
        .observe(
            this,
            provider -> {
              cameraProvider = provider;
              bindAllCameraUseCases();
            });

    ImageView settingsButton = findViewById(R.id.settings_button);
    settingsButton.setOnClickListener(
        v -> {
          Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
          intent.putExtra(
              SettingsActivity.EXTRA_LAUNCH_SOURCE,
              SettingsActivity.LaunchSource.CAMERAX_LIVE_PREVIEW);
          startActivity(intent);
        });
  }

  private void onClickBeginRegister(View view) {
      Toast.makeText(this, "Start registering", Toast.LENGTH_SHORT).show();
      this.currentState = State.Registering;
      this.onChangeState();
      this.setFaceMeshRegisterList(null);
  }



  private void onSaveImage(View view) {
      List<FaceMesh> faceMeshes = this.graphicOverlay.getFaceMeshesList();
      if (faceMeshes.size() == 0) {
          Toast.makeText(this, "No face detected", Toast.LENGTH_SHORT).show();
      } else if (faceMeshes.size() > 1) {
          Toast.makeText(this, "More than 1 face detected", Toast.LENGTH_SHORT).show();
      } else {
        Toast.makeText(this, "Save image successfully", Toast.LENGTH_SHORT).show();
        this.getFaceMeshRegisterList().add(faceMeshes.get(0));
      }
  }

  private void onClickFinishRegister(View view) {
    if (this.getFaceMeshRegisterList().size() >= 1) {
      Bundle bundle = new Bundle();
      List<FaceMesh> faceMeshes = this.getFaceMeshRegisterList();
      ArrayList<String> faceDistanceList = new ArrayList<>();
        if (faceMeshes != null) {
          for (FaceMesh faceMesh : faceMeshes) {
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
            faceDistanceList.add(distanceList.toString());
          }
//          Log.d("Z", "onClickRecord: " + distanceList.toString());
          Intent intent = new Intent(CameraXLivePreviewActivity.this, RegisterActivity.class);
          bundle.putStringArrayList("faceMeshPoints", faceDistanceList);
          intent.putExtras(bundle);
          startActivity(intent);
        }
      }
    this.currentState = State.Processing;
    this.onChangeState();

  }

  private enum State {
    Processing,
    Registering
  }

  private State currentState;

  public void onChangeState() {
     if (currentState == State.Processing) {
          finishRegisterButton.setVisibility(View.GONE);
          saveImageButton.setVisibility(View.GONE);
          registerButton.setVisibility(View.VISIBLE);
     } else if (currentState == State.Registering) {
         finishRegisterButton.setVisibility(View.VISIBLE);
         saveImageButton.setVisibility(View.VISIBLE);
         registerButton.setVisibility(View.GONE);
     }
  }

  @Override
  protected void onSaveInstanceState(@NonNull Bundle bundle) {
    super.onSaveInstanceState(bundle);
    bundle.putString(STATE_SELECTED_MODEL, selectedModel);
  }

  @Override
  public synchronized void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
    // An item was selected. You can retrieve the selected item using
    // parent.getItemAtPosition(pos)
    selectedModel = parent.getItemAtPosition(pos).toString();
    Log.d(TAG, "Selected model: " + selectedModel);
    bindAnalysisUseCase();
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {
    // Do nothing.
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    if (cameraProvider == null) {
      return;
    }
    int newLensFacing =
        lensFacing == CameraSelector.LENS_FACING_FRONT
            ? CameraSelector.LENS_FACING_BACK
            : CameraSelector.LENS_FACING_FRONT;
    CameraSelector newCameraSelector =
        new CameraSelector.Builder().requireLensFacing(newLensFacing).build();
    try {
      if (cameraProvider.hasCamera(newCameraSelector)) {
        Log.d(TAG, "Set facing to " + newLensFacing);
        lensFacing = newLensFacing;
        cameraSelector = newCameraSelector;
        bindAllCameraUseCases();
        return;
      }
    } catch (CameraInfoUnavailableException e) {
      // Falls through
    }
    Toast.makeText(
            getApplicationContext(),
            "This device does not have lens with facing: " + newLensFacing,
            Toast.LENGTH_SHORT)
        .show();
  }

  @Override
  public void onResume() {
    super.onResume();
    bindAllCameraUseCases();
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (imageProcessor != null) {
      imageProcessor.stop();
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (imageProcessor != null) {
      imageProcessor.stop();
    }
  }

  private void bindAllCameraUseCases() {
    if (cameraProvider != null) {
      // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.
      cameraProvider.unbindAll();
      bindPreviewUseCase();
      bindAnalysisUseCase();
    }
  }

  private void bindPreviewUseCase() {
    if (!PreferenceUtils.isCameraLiveViewportEnabled(this)) {
      return;
    }
    if (cameraProvider == null) {
      return;
    }
    if (previewUseCase != null) {
      cameraProvider.unbind(previewUseCase);
    }

    Preview.Builder builder = new Preview.Builder();
    Size targetResolution = PreferenceUtils.getCameraXTargetResolution(this, lensFacing);
    if (targetResolution != null) {
      builder.setTargetResolution(targetResolution);
    }
    previewUseCase = builder.build();
    previewUseCase.setSurfaceProvider(previewView.getSurfaceProvider());
    camera =
        cameraProvider.bindToLifecycle(/* lifecycleOwner= */ this, cameraSelector, previewUseCase);
  }

  private void bindAnalysisUseCase() {
    if (cameraProvider == null) {
      return;
    }
    if (analysisUseCase != null) {
      cameraProvider.unbind(analysisUseCase);
    }
    if (imageProcessor != null) {
      imageProcessor.stop();
    }

    try {
      switch (selectedModel) {
        case OBJECT_DETECTION:
          Log.i(TAG, "Using Object Detector Processor");
          ObjectDetectorOptions objectDetectorOptions =
              PreferenceUtils.getObjectDetectorOptionsForLivePreview(this);
          imageProcessor = new ObjectDetectorProcessor(this, objectDetectorOptions);
          break;
        case OBJECT_DETECTION_CUSTOM:
          Log.i(TAG, "Using Custom Object Detector Processor");
          LocalModel localModel =
              new LocalModel.Builder()
                  .setAssetFilePath("custom_models/object_labeler.tflite")
                  .build();
          CustomObjectDetectorOptions customObjectDetectorOptions =
              PreferenceUtils.getCustomObjectDetectorOptionsForLivePreview(this, localModel);
          imageProcessor = new ObjectDetectorProcessor(this, customObjectDetectorOptions);
          break;
        case CUSTOM_AUTOML_OBJECT_DETECTION:
          Log.i(TAG, "Using Custom AutoML Object Detector Processor");
          LocalModel customAutoMLODTLocalModel =
              new LocalModel.Builder().setAssetManifestFilePath("automl/manifest.json").build();
          CustomObjectDetectorOptions customAutoMLODTOptions =
              PreferenceUtils.getCustomObjectDetectorOptionsForLivePreview(
                  this, customAutoMLODTLocalModel);
          imageProcessor = new ObjectDetectorProcessor(this, customAutoMLODTOptions);
          break;
        case TEXT_RECOGNITION_CHINESE:
          Log.i(TAG, "Using on-device Text recognition Processor for Latin and Chinese.");
          imageProcessor =
              new TextRecognitionProcessor(
                  this, new ChineseTextRecognizerOptions.Builder().build());
          break;
        case TEXT_RECOGNITION_DEVANAGARI:
          Log.i(TAG, "Using on-device Text recognition Processor for Latin and Devanagari.");
          imageProcessor =
              new TextRecognitionProcessor(
                  this, new DevanagariTextRecognizerOptions.Builder().build());
          break;
        case TEXT_RECOGNITION_JAPANESE:
          Log.i(TAG, "Using on-device Text recognition Processor for Latin and Japanese.");
          imageProcessor =
              new TextRecognitionProcessor(
                  this, new JapaneseTextRecognizerOptions.Builder().build());
          break;
        case TEXT_RECOGNITION_KOREAN:
          Log.i(TAG, "Using on-device Text recognition Processor for Latin and Korean.");
          imageProcessor =
              new TextRecognitionProcessor(this, new KoreanTextRecognizerOptions.Builder().build());
          break;
        case TEXT_RECOGNITION_LATIN:
          Log.i(TAG, "Using on-device Text recognition Processor for Latin.");
          imageProcessor =
              new TextRecognitionProcessor(this, new TextRecognizerOptions.Builder().build());
          break;
        case FACE_DETECTION:
          Log.i(TAG, "Using Face Detector Processor");
          imageProcessor = new FaceDetectorProcessor(this);
          break;
        case BARCODE_SCANNING:
          Log.i(TAG, "Using Barcode Detector Processor");
          ZoomCallback zoomCallback = null;
          if (PreferenceUtils.shouldEnableAutoZoom(this)) {
            zoomCallback =
                zoomLevel -> {
                  Log.i(TAG, "Set zoom ratio " + zoomLevel);
                  @SuppressWarnings("FutureReturnValueIgnored")
                  ListenableFuture<Void> ignored =
                      camera.getCameraControl().setZoomRatio(zoomLevel);
                  return true;
                };
          }
          imageProcessor = new BarcodeScannerProcessor(this, zoomCallback);
          break;
        case IMAGE_LABELING:
          Log.i(TAG, "Using Image Label Detector Processor");
          imageProcessor = new LabelDetectorProcessor(this, ImageLabelerOptions.DEFAULT_OPTIONS);
          break;
        case IMAGE_LABELING_CUSTOM:
          Log.i(TAG, "Using Custom Image Label (Birds) Detector Processor");
          LocalModel localClassifier =
              new LocalModel.Builder()
                  .setAssetFilePath("custom_models/bird_classifier.tflite")
                  .build();
          CustomImageLabelerOptions customImageLabelerOptions =
              new CustomImageLabelerOptions.Builder(localClassifier).build();
          imageProcessor = new LabelDetectorProcessor(this, customImageLabelerOptions);
          break;
        case CUSTOM_AUTOML_LABELING:
          Log.i(TAG, "Using Custom AutoML Image Label Detector Processor");
          LocalModel customAutoMLLabelLocalModel =
              new LocalModel.Builder().setAssetManifestFilePath("automl/manifest.json").build();
          CustomImageLabelerOptions customAutoMLLabelOptions =
              new CustomImageLabelerOptions.Builder(customAutoMLLabelLocalModel)
                  .setConfidenceThreshold(0)
                  .build();
          imageProcessor = new LabelDetectorProcessor(this, customAutoMLLabelOptions);
          break;
        case POSE_DETECTION:
          PoseDetectorOptionsBase poseDetectorOptions =
              PreferenceUtils.getPoseDetectorOptionsForLivePreview(this);
          boolean shouldShowInFrameLikelihood =
              PreferenceUtils.shouldShowPoseDetectionInFrameLikelihoodLivePreview(this);
          boolean visualizeZ = PreferenceUtils.shouldPoseDetectionVisualizeZ(this);
          boolean rescaleZ = PreferenceUtils.shouldPoseDetectionRescaleZForVisualization(this);
          boolean runClassification = PreferenceUtils.shouldPoseDetectionRunClassification(this);
          imageProcessor =
              new PoseDetectorProcessor(
                  this,
                  poseDetectorOptions,
                  shouldShowInFrameLikelihood,
                  visualizeZ,
                  rescaleZ,
                  runClassification,
                  /* isStreamMode = */ true);
          break;
        case SELFIE_SEGMENTATION:
          imageProcessor = new SegmenterProcessor(this);
          break;
        case FACE_MESH_DETECTION:
          imageProcessor = new FaceMeshDetectorProcessor(this);
          break;
        default:
          throw new IllegalStateException("Invalid model name");
      }
    } catch (Exception e) {
      Log.e(TAG, "Can not create image processor: " + selectedModel, e);
      Toast.makeText(
              getApplicationContext(),
              "Can not create image processor: " + e.getLocalizedMessage(),
              Toast.LENGTH_LONG)
          .show();
      return;
    }

    ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
    Size targetResolution = PreferenceUtils.getCameraXTargetResolution(this, lensFacing);
    if (targetResolution != null) {
      builder.setTargetResolution(targetResolution);
    }
    analysisUseCase = builder.build();

    needUpdateGraphicOverlayImageSourceInfo = true;
    analysisUseCase.setAnalyzer(
        // imageProcessor.processImageProxy will use another thread to run the detection underneath,
        // thus we can just runs the analyzer itself on main thread.
        ContextCompat.getMainExecutor(this),
        imageProxy -> {
          if (needUpdateGraphicOverlayImageSourceInfo) {
            boolean isImageFlipped = lensFacing == CameraSelector.LENS_FACING_FRONT;
            int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
            if (rotationDegrees == 0 || rotationDegrees == 180) {
              graphicOverlay.setImageSourceInfo(
                  imageProxy.getWidth(), imageProxy.getHeight(), isImageFlipped);
            } else {
              graphicOverlay.setImageSourceInfo(
                  imageProxy.getHeight(), imageProxy.getWidth(), isImageFlipped);
            }
            needUpdateGraphicOverlayImageSourceInfo = false;
          }
          try {
            imageProcessor.processImageProxy(imageProxy, graphicOverlay);
          } catch (MlKitException e) {
            Log.e(TAG, "Failed to process image. Error: " + e.getLocalizedMessage());
            Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT)
                .show();
          }
        });

    cameraProvider.bindToLifecycle(/* lifecycleOwner= */ this, cameraSelector, analysisUseCase);
  }
}

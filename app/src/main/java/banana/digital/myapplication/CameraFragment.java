package banana.digital.myapplication;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.PermissionRequest;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.vision.Detector;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetector;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetectorOptions;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class CameraFragment extends Fragment {

    public CameraFragment() {
        // Required empty public constructor
    }

    TextureView textureView;
    TextView textView;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private FirebaseVisionLabelDetector firebaseVisionLabelDetector;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        textureView = view.findViewById(R.id.texture_view);
        textView = view.findViewById(R.id.text);
    }

    @Override
    public void onResume() {
        super.onResume();
        initializeDetector();
        if (textureView.isAvailable()) checkPermission();
        else {
            textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
                    checkPermission();
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

                }
            });
        }
    }

    private void initializeDetector() {
        FirebaseVisionLabelDetectorOptions options =
                new FirebaseVisionLabelDetectorOptions.Builder()
                        .setConfidenceThreshold(0.8f)
                        .build();
        firebaseVisionLabelDetector = FirebaseVision.getInstance().getVisionLabelDetector(options);
    }

    private void checkPermission() {
        Dexter.withActivity(getActivity())
            .withPermission(Manifest.permission.CAMERA)
            .withListener(new PermissionListener() {
                @Override public void onPermissionGranted(PermissionGrantedResponse response) {
                    startThread();
                    openCamera();
                }
                @Override public void onPermissionDenied(PermissionDeniedResponse response) {/* ... */}

                @Override
                public void onPermissionRationaleShouldBeShown(com.karumi.dexter.listener.PermissionRequest permission, PermissionToken token) {
                }
            }).check();

    }

    private void startThread() {
        backgroundThread = new HandlerThread("CameraFragment");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        final CameraManager cameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        if (cameraManager != null) {
            try {
                final String[] cameraIds = cameraManager.getCameraIdList();
                String backCameraId = null;
                for (String id : cameraIds) {
                    final CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                    if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK) {
                        backCameraId = id;
                    }
                }

                if (backCameraId != null) {
                    cameraManager.openCamera(backCameraId, new CameraDevice.StateCallback() {
                        @Override
                        public void onOpened(@NonNull CameraDevice cameraDevice) {
                            captureCamera(cameraDevice);
                        }

                        @Override
                        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                            cameraDevice.close();
                        }

                        @Override
                        public void onError(@NonNull CameraDevice cameraDevice, int i) {

                        }
                    }, backgroundHandler);
                }

            } catch (CameraAccessException e) {

            }
        }
    }

    private void captureCamera(@NonNull final CameraDevice cameraDevice) {
        final Surface surface = new Surface(textureView.getSurfaceTexture());
        final List<Surface> surfaces = Arrays.asList(surface);
        try {
            cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    startRecord(cameraDevice, cameraCaptureSession, surface);
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {

        }
    }

    private void startRecord(final CameraDevice cameraDevice, CameraCaptureSession cameraCaptureSession, Surface surface) {
        try {
            CaptureRequest.Builder requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            requestBuilder.addTarget(surface);
            requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            cameraCaptureSession.setRepeatingRequest(requestBuilder.build(), new CameraCaptureSession.CaptureCallback(){
                @Override
                public void onCaptureStarted(@androidx.annotation.NonNull @NonNull CameraCaptureSession session, @androidx.annotation.NonNull @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                    firebaseStartLabelDetecting();
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {

        }
    }

    private boolean firebaseLabelDetectingStarted = false;

    private void firebaseStartLabelDetecting() {
        if (!firebaseLabelDetectingStarted) {
            firebaseLabelDetectingStarted = true;
            processFirebaseLabelDetecting();
        }
    }

    private void processFirebaseLabelDetecting() {
        Bitmap bitmap = textureView.getBitmap();
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        firebaseVisionLabelDetector.detectInImage(image)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionLabel>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionLabel> firebaseVisionLabels) {
                        final List<String> labels = new ArrayList<>();
                        for (FirebaseVisionLabel visionLabel : firebaseVisionLabels) {
                            labels.add(visionLabel.getLabel());
                        }
                        textView.setText(TextUtils.join(",", labels));
                        processFirebaseLabelDetecting();
                    }
                });
    }

}

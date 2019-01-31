package banana.digital.myapplication;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.label.FirebaseVisionLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetector;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetectorOptions;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.FileNotFoundException;
import java.io.IOException;
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
    private FirebaseVisionFaceDetector firebaseVisionFaceDetector;

    FaceView faceView;

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
        faceView = view.findViewById(R.id.face_view);
    }

    @Override
    public void onResume() {
        super.onResume();
        initializeLabelDetector();
        initializeFaceDetector();
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

    private void initializeLabelDetector() {
        FirebaseVisionLabelDetectorOptions options =
                new FirebaseVisionLabelDetectorOptions.Builder()
                        .setConfidenceThreshold(0.8f)
                        .build();
        firebaseVisionLabelDetector = FirebaseVision.getInstance().getVisionLabelDetector(options);
    }

    private void initializeFaceDetector() {
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
                        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.NO_LANDMARKS)
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.NO_CLASSIFICATIONS)
                        .build();
        firebaseVisionFaceDetector = FirebaseVision.getInstance().getVisionFaceDetector(options);
    }

    private void initializeTensorFlowLabelDetector() {
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
                //String backCameraId = null;
                String frontCameraId = null;
                for (String id : cameraIds) {
                    final CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                    if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_FRONT) {
                        frontCameraId = id;
                    }
                }

                if (frontCameraId != null) {
                    cameraManager.openCamera(frontCameraId, new CameraDevice.StateCallback() {
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
                    firebaseStartFaceDetecting();
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
        final long now = System.currentTimeMillis();
        Bitmap bitmap = textureView.getBitmap();
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        firebaseVisionLabelDetector.detectInImage(image)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionLabel>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionLabel> firebaseVisionLabels) {
                        String result = (System.currentTimeMillis() - now) + "ms\n";
                        final List<String> labels = new ArrayList<>();
                        for (FirebaseVisionLabel visionLabel : firebaseVisionLabels) {
                            result += visionLabel.getLabel() + " - " + visionLabel.getConfidence() + "\n";
                            labels.add(visionLabel.getLabel());
                        }
                        //textView.setText(result);
                        processFirebaseLabelDetecting();
                    }
                });
    }

    private boolean firebaseFaceDetectingStarted = false;

    private void firebaseStartFaceDetecting() {
        if (!firebaseFaceDetectingStarted) {
            firebaseFaceDetectingStarted = true;
            processFirebaseFaceDetecting();
        }
    }

    private void processFirebaseFaceDetecting() {
        Bitmap bitmap = textureView.getBitmap();
        Bitmap scaleBitmap = Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * 0.3f), (int) (bitmap.getHeight() * 0.3f), true);
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(scaleBitmap);
        firebaseVisionFaceDetector.detectInImage(image)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {
                        faceView.showFaces(firebaseVisionFaces, 1f / 0.3f);
                        processFirebaseFaceDetecting();
                    }
                });
//        final int width = 400;
//        Bitmap bitmap = textureView.getBitmap();
//        final float scale = (float) (width / bitmap.getWidth());
//        final int height = (int) (bitmap.getHeight() * scale);
//        Bitmap scaleBitmap = Bitmap.createScaledBitmap(bitmap, (width), (height), true);
//
//        byte[] bytes = getYV12(width, height, scaleBitmap);
//        FirebaseVisionImageMetadata metadata = new FirebaseVisionImageMetadata.Builder()
//                .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_YV12)
//                .build();
//        FirebaseVisionImage image = FirebaseVisionImage.fromByteArray(bytes, metadata);
//        firebaseVisionFaceDetector.detectInImage(image)
//                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
//                    @Override
//                    public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {
//                        System.out.println("onSuccess" + firebaseVisionFaces.size());
//                        faceView.showFaces(firebaseVisionFaces, 1 / scale);
//                        processFirebaseFaceDetecting();
//                    }
//                });
    }

    private byte [] getYV12(int inputWidth, int inputHeight, Bitmap scaled) {

        int [] argb = new int[inputWidth * inputHeight];

        scaled.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight);

        byte [] yuv = new byte[inputHeight * inputWidth + 2 * (int) Math.ceil(inputHeight/2.0) *(int) Math.ceil(inputWidth/2.0)];
        encodeYV12(yuv, argb, inputWidth, inputHeight);

        scaled.recycle();

        return yuv;
    }

    private void encodeYV12(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = width * height;

        int yIndex = 0;
        int uIndex = frameSize;
        int vIndex = frameSize + (frameSize / 4);

        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff) >> 0;

                // well known RGB to YUV algorithm
                Y = ( (  66 * R + 129 * G +  25 * B + 128) >> 8) +  16;
                U = ( ( -38 * R -  74 * G + 112 * B + 128) >> 8) + 128;
                V = ( ( 112 * R -  94 * G -  18 * B + 128) >> 8) + 128;

                // YV12 has a plane of Y and two chroma plans (U, V) planes each sampled by a factor of 2
                //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
                //    pixel AND every other scanline.
                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uIndex++] = (byte)((V<0) ? 0 : ((V > 255) ? 255 : V));
                    yuv420sp[vIndex++] = (byte)((U<0) ? 0 : ((U > 255) ? 255 : U));
                }

                index ++;
            }
        }
    }

}

package banana.digital.myapplication;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.PermissionRequest;
import android.widget.TextView;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.single.PermissionListener;


public class CameraFragment extends Fragment {

    public CameraFragment() {
        // Required empty public constructor
    }

    TextureView textureView;
    TextView textView;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;


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

    private void captureCamera(@NonNull CameraDevice cameraDevice) {

    }

}

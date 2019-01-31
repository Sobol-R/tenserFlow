package banana.digital.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class ImageFragment extends Fragment {

    ImageView imageView;
    Button button;
    FaceView faceView;

    private FirebaseVisionFaceDetector firebaseVisionFaceDetector;
    Bitmap bitmap;

    public ImageFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_image, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeFaceDetector();
        imageView = view.findViewById(R.id.image);
        faceView = view.findViewById(R.id.face_view_2);
        button = view.findViewById(R.id.choose_image);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), 4);
            }
        });
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

    private boolean firebaseFaceDetectingStarted = false;

    private void firebaseStartFaceDetecting() {
        if (!firebaseFaceDetectingStarted) {
            firebaseFaceDetectingStarted = true;
            processFirebaseFaceDetecting();
        }
    }

    private void processFirebaseFaceDetecting() {
        Bitmap scaleBitmap = Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * 0.3f), (int) (bitmap.getHeight() * 0.3f), true);
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(scaleBitmap);
        firebaseVisionFaceDetector.detectInImage(image)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {
                        System.out.println("onSuccess" + firebaseVisionFaces.size());
                        faceView.showFaces(firebaseVisionFaces, 1f / 0.3f);
                        processFirebaseFaceDetecting();
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 4 && resultCode == Activity.RESULT_OK) {
            Uri selectedImage = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), selectedImage);
                imageView.setImageBitmap(bitmap);
                firebaseStartFaceDetecting();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}

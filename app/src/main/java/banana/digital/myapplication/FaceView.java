package banana.digital.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.google.firebase.ml.vision.face.FirebaseVisionFace;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

public class FaceView extends View {

    List <FirebaseVisionFace> mFaces = new ArrayList<>();

    Paint paint;

    public FaceView(Context context) {
        super(context);
        initialize();
    }

    public FaceView(Context context, @Nullable @android.support.annotation.Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public FaceView(Context context, @Nullable @android.support.annotation.Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    public FaceView(Context context, @Nullable @android.support.annotation.Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize();
    }

    public void showFaces(List<FirebaseVisionFace> faces) {
        mFaces = faces;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mFaces != null) {
            for (FirebaseVisionFace face : mFaces) {
                canvas.drawRect(face.getBoundingBox().left / 0.3f, face.getBoundingBox().top / 0.3f,
                        face.getBoundingBox().right / 0.3f, face.getBoundingBox().bottom / 0.3f, paint);
            }
        }
    }

    private void initialize() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(10);
    }
}

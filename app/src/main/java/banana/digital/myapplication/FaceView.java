package banana.digital.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

public class FaceView extends View {

    List <FirebaseVisionFace> mFaces = new ArrayList<>();
    float scale;

    Paint paint;
    Path path;

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

    public void showFaces(List<FirebaseVisionFace> faces, float ratio) {
        mFaces = faces;
        scale = ratio;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mFaces != null) {
            for (FirebaseVisionFace face : mFaces) {
                FirebaseVisionFaceContour contour = face.getContour(FirebaseVisionFaceContour.ALL_POINTS);
                List <FirebaseVisionPoint> points = contour.getPoints();
                for (FirebaseVisionPoint point : points) {
                    canvas.drawCircle(point.getX() * scale, point.getY() * scale, 0.01f, paint);
                }
//                for (int i = 0; i < points.size() - 1; i++) {
//                    FirebaseVisionPoint point = points.get(i + 1);
//                    if (i == 0) path.moveTo(points.get(i).getX() * scale, points.get(i).getY() * scale);
//                    path.lineTo(point.getX() * scale, point.getY() * scale);
//                }
                path.rewind();
            }
        }
    }

    private void initialize() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(10);
        path = new Path();
    }
}

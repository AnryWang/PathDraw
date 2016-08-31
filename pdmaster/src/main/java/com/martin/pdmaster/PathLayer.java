package com.martin.pdmaster;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.Log;

import com.caverock.androidsvg.PreserveAspectRatio;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import java.util.ArrayList;
import java.util.List;

/**
 * 作者：MartinBZDQSM on 2016/8/28 0028.
 * 博客：http://www.jianshu.com/users/78f0e5f4a403/latest_articles
 * github：https://github.com/MartinBZDQSM
 * <p>
 * 该类修改自 PathView的SvgUtils 链接：https://github.com/geftimov/android-pathview
 */
public class PathLayer {
    /**
     * It is for logging purposes.
     */
    private static final String LOG_TAG = "PathLayer";
    /**
     * 一张svg可能会有多条path路径
     */
    private final List<SvgPath> mPaths = new ArrayList<>();

    /**
     * The init svg.
     */
    private SVG mSvg;

    /**
     * Init the SVGUtils with a paint1 for coloring.
     */
    public PathLayer() {
    }

    /**
     * Loading the svg from the resources.
     *
     * @param context     Context object to get the resources.
     * @param svgResource int resource id of the svg.
     */
    public void load(Context context, int svgResource) {
        if (mSvg != null)
            return;
        try {
            mSvg = SVG.getFromResource(context, svgResource);
            mSvg.setDocumentPreserveAspectRatio(PreserveAspectRatio.UNSCALED);
        } catch (SVGParseException e) {
            Log.e(LOG_TAG, "Could not load specified SVG resource", e);
        }
    }

    /**
     * 渲染svg到canvas上，把path回调回来
     *
     * @param width  - the width to scale down the view to,
     * @param height - the height to scale down the view to,
     * @return All the paths from the svg.
     */
    public List<SvgPath> getPathsForViewport(final int width, final int height, final float strokeWidth) {
        Canvas canvas = new Canvas() {
            private final Matrix mMatrix = new Matrix();

            @Override
            public int getWidth() {
                return width;
            }

            @Override
            public int getHeight() {
                return height;
            }

            @Override
            public void drawPath(Path path, Paint paint) {
                Path dst = new Path();
                //noinspection deprecation
                getMatrix(mMatrix);
                path.transform(mMatrix, dst);
                mPaths.add(new SvgPath(dst));
            }
        };

        rescaleCanvas(width, height, strokeWidth, canvas);

        return mPaths;
    }

    /**
     * 按实际比例进行缩放
     *
     * @param width       The width of the canvas.
     * @param height      The height of the canvas.
     * @param strokeWidth Width of the path to add to scaling.
     * @param canvas      The canvas to be drawn.
     */
    private void rescaleCanvas(int width, int height, float strokeWidth, Canvas canvas) {
        if (mSvg == null)
            return;
        final RectF viewBox = mSvg.getDocumentViewBox();

        final float scale = Math.min(width
                        / (viewBox.width()),
                height / (viewBox.height()));

        canvas.translate((width - viewBox.width() * scale) / 2.0f,
                (height - viewBox.height() * scale) / 2.0f);
        canvas.scale(scale, scale);

        mSvg.renderToCanvas(canvas);
    }


    /**
     * Path with bounds for scalling , length and paint.
     */
    public static class SvgPath {

        /**
         * Region of the path.
         */
        private static final Region REGION = new Region();
        /**
         * This is done for clipping the bounds of the path.
         */
        private static final Region MAX_CLIP =
                new Region(Integer.MIN_VALUE, Integer.MIN_VALUE,
                        Integer.MAX_VALUE, Integer.MAX_VALUE);
        /**
         * The path itself.
         */
        final Path path;
        /**
         * The length of the path.
         */
        float length;
        /**
         * Listener to notify that an animation step has happened.
         */
        AnimationStepListener animationStepListener;
        /**
         * The bounds of the path.
         */
        final Rect bounds;
        /**
         * The measure of the path, we can use it later to get segment of it.
         */
        final PathMeasure measure;

        /**
         * Constructor to add the path and the paint.
         *
         * @param path The path that comes from the rendered svg.
         */
        SvgPath(Path path) {
            this.path = path;

            measure = new PathMeasure(path, false);
            this.length = measure.getLength();

            REGION.setPath(path, MAX_CLIP);
            bounds = REGION.getBounds();
        }

        /**
         * Sets the animation step listener.
         *
         * @param animationStepListener AnimationStepListener.
         */
        public void setAnimationStepListener(AnimationStepListener animationStepListener) {
            this.animationStepListener = animationStepListener;
        }

        /**
         * Sets the length of the path.
         *
         * @param length The length to be set.
         */
        public void setLength(float length) {
            path.reset();
            measure.getSegment(0.0f, length, path, true);
            path.rLineTo(0.0f, 0.0f);

            if (animationStepListener != null) {
                animationStepListener.onAnimationStep();
            }
        }

        /**
         * @return The length of the path.
         */
        public float getLength() {
            return length;
        }
    }

    public interface AnimationStepListener {

        /**
         * Called when an animation step happens.
         */
        void onAnimationStep();
    }

    public List<SvgPath> getPaths() {
        return mPaths;
    }
}

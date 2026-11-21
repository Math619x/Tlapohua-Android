package com.rey.celdas;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

final class ForestTravel {
    private final Dialog dialog;
    private final ValueAnimator motion;
    private Runnable finished;

    static boolean motionEnabled() {
        return Build.VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled();
    }

    ForestTravel(Activity activity, boolean backwards, Runnable finished) {
        this.finished = finished;
        dialog = new Dialog(activity, android.R.style.Theme_Material_NoActionBar_Fullscreen);
        dialog.getWindow().setWindowAnimations(0);
        float density = activity.getResources().getDisplayMetrics().density;
        int edge = Math.round(18 * density);
        int radius = 12;
        FrameLayout layout = new FrameLayout(activity);
        Trail trail = new Trail(activity, backwards);
        layout.addView(trail, new FrameLayout.LayoutParams(-1, -1));

        TextView caption = new TextView(activity);
        caption.setText(backwards ? "Una ráfaga te empuja hacia atrás" : "Avanzas por el bosque");
        caption.setTextColor(TlapohuaArt.CREAM);
        caption.setTextSize(16);
        caption.setGravity(Gravity.CENTER);
        caption.setPadding(edge, edge / 2, edge, edge / 2);
        caption.setBackground(TlapohuaArt.tile(TlapohuaArt.BASALT, density, radius, false));
        FrameLayout.LayoutParams captionPosition = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        captionPosition.setMargins(edge, edge, edge, 0);
        layout.addView(caption, captionPosition);

        Button skip = new Button(activity);
        skip.setText("Saltar");
        skip.setAllCaps(false);
        skip.setTextColor(TlapohuaArt.CREAM);
        skip.setBackground(TlapohuaArt.tile(TlapohuaArt.BASALT, density, radius, true));
        skip.setOnClickListener(view -> dialog.dismiss());
        FrameLayout.LayoutParams buttonPosition = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.END);
        buttonPosition.setMargins(edge, 0, edge, edge);
        layout.addView(skip, buttonPosition);
        layout.setOnApplyWindowInsetsListener((view, insets) -> {
            layout.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });

        motion = ValueAnimator.ofFloat(0f, 1f);
        motion.setDuration(backwards ? 1350 : 1550);
        motion.setInterpolator(new AccelerateDecelerateInterpolator());
        motion.addUpdateListener(animation -> {
            trail.progress = (Float) animation.getAnimatedValue();
            trail.invalidate();
        });
        motion.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                if (dialog.isShowing()) dialog.dismiss();
            }
        });
        dialog.setOnDismissListener(window -> {
            motion.removeAllListeners();
            motion.cancel();
            Runnable next = this.finished;
            this.finished = null;
            if (next != null && !activity.isFinishing() && !activity.isDestroyed() && !activity.isChangingConfigurations()) {
                next.run();
            }
        });
        dialog.setContentView(layout);
    }

    void show() {
        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.getWindow().getDecorView().requestApplyInsets();
        motion.start();
    }

    void pause() {
        motion.pause();
    }

    void resume() {
        if (motion.isPaused()) motion.resume();
    }

    void dispose() {
        finished = null;
        dialog.dismiss();
    }

    private static final class Trail extends View {
        private static final float WIDTH = 360f;
        private static final float CAMERA_TRAVEL = 4.2f;
        private static final float NEAR = .72f, FAR = 19f;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final boolean backwards;
        private LinearGradient sky;
        private float height, horizon, camera, cameraCenter, sway;
        private float progress;

        Trail(Activity activity, boolean backwards) {
            super(activity);
            this.backwards = backwards;
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        @Override protected void onSizeChanged(int width, int screenHeight, int oldWidth, int oldHeight) {
            if (width == 0) return;
            height = WIDTH * screenHeight / width;
            sky = new LinearGradient(0, 0, 0, height * .59f,
                0xff224d49, 0xffd7d8a3, Shader.TileMode.CLAMP);
        }

        @Override protected void onDraw(Canvas canvas) {
            if (height <= 0) return;
            float scale = getWidth() / WIDTH;
            canvas.save();
            canvas.scale(scale, scale);
            camera = (backwards ? 1f - progress : progress) * CAMERA_TRAVEL;
            float motion = wave(progress * (float) Math.PI);
            sway = wave(progress * 20f) * motion * (backwards ? 5 : 2.3f);
            cameraCenter = bend(camera);
            horizon = height * .43f + wave(progress * 24f) * motion * Math.min(4f, height * .012f);
            paint.setShader(sky);
            canvas.drawRect(0, 0, WIDTH, height, paint);
            paint.setShader(null);
            float sunY = horizon - Math.min(42, height * .14f);
            paint.setColor(0x59ffe6a0);
            canvas.drawCircle(WIDTH * .65f - sway, sunY, 30, paint);
            paint.setColor(0xffe8d99c);
            canvas.drawCircle(WIDTH * .65f - sway, sunY, 21, paint);
            ridge(canvas, 0xff83a683, 13f, .9f);
            ridge(canvas, 0xff507c63, 9f, 2.3f);

            ribbon(canvas, 0xff658165, .76f);
            ribbon(canvas, 0xffbda77a, .63f);
            ribbon(canvas, 0xffd5bc8d, .47f);
            for (int i = 24; i >= 0; i--) {
                float world = 1.2f + i * 1.13f;
                float depth = world - camera;
                if (depth <= NEAR || depth > FAR) continue;
                float center = bend(world);
                float margin = .85f + (i % 4) * .14f;
                stone(canvas, center + (i % 2 == 0 ? -.45f : .45f), depth);
                float otherDepth = depth + .33f;
                tree(canvas, bend(world + .33f) + margin + .09f, otherDepth, i + 2);
                tree(canvas, center - margin, depth, i);
            }
            if (backwards) gust(canvas, motion);
            canvas.restore();
        }

        private void ridge(Canvas canvas, int color, float peaks, float phase) {
            path.reset();
            path.moveTo(0, horizon + 12);
            for (int i = 0; i <= 24; i++) {
                float x = i * WIDTH / 24;
                float taper = .30f + .70f * Math.abs(x - WIDTH / 2) / (WIDTH / 2);
                float canopy = Math.abs(wave(i * 1.23f + phase + camera * .06f));
                path.lineTo(x, horizon - (4 + canopy * peaks) * taper);
            }
            path.lineTo(WIDTH, height);
            path.lineTo(0, height);
            fillPath(canvas, color);
        }

        private void ribbon(Canvas canvas, int color, float halfWidth) {
            path.reset();
            path.moveTo(projectX(bend(camera + NEAR) - halfWidth, NEAR), ground(NEAR));
            for (int i = 1; i <= 25; i++) ribbonPoint(i, -halfWidth);
            for (int i = 25; i >= 0; i--) ribbonPoint(i, halfWidth);
            fillPath(canvas, color);
        }

        private void ribbonPoint(int step, float edge) {
            float part = step / 25f;
            float depth = NEAR + (FAR - NEAR) * part * part;
            path.lineTo(projectX(bend(camera + depth) + edge, depth), ground(depth));
        }

        private void tree(Canvas canvas, float worldX, float depth, int seed) {
            float x = projectX(worldX, depth), y = ground(depth);
            float size = (height - horizon) * (.86f + (seed % 5) * .082f) / depth;
            float trunk = Math.max(1f, size * .115f);
            paint.setColor(depth < 3.5f ? 0xff463d2d : 0xff555443);
            canvas.drawRect(x - trunk, y - size * .73f, x + trunk, y, paint);
            paint.setColor(0x567f6b49);
            canvas.drawRect(x + trunk * .18f, y - size * .70f, x + trunk * .66f, y - size * .08f, paint);
            crown(canvas, x, y - size * .58f, size * .39f, depth < 3.5f ? 0xff254838 : 0xff3c6952);
            crown(canvas, x + size * .045f, y - size * .81f, size * .31f, depth < 3.5f ? 0xff32674e : 0xff538368);
            crown(canvas, x - size * .015f, y - size, size * .22f, depth < 3.5f ? 0xff48795a : 0xff699676);
        }

        private void crown(Canvas canvas, float x, float y, float size, int color) {
            path.reset();
            path.moveTo(x, y - size * .85f);
            path.lineTo(x + size * .72f, y - size * .27f);
            path.lineTo(x + size, y + size * .35f);
            path.lineTo(x + size * .35f, y + size * .52f);
            path.lineTo(x - size * .49f, y + size * .52f);
            path.lineTo(x - size, y + size * .29f);
            path.lineTo(x - size * .75f, y - size * .27f);
            fillPath(canvas, color);
        }

        private void stone(Canvas canvas, float worldX, float depth) {
            float x = projectX(worldX, depth), y = ground(depth);
            float size = 9f / depth;
            paint.setColor(0xffa18e6d);
            canvas.drawOval(x - size, y - size * .36f, x + size, y + size * .36f, paint);
        }

        private void gust(Canvas canvas, float strength) {
            paint.setStrokeCap(Paint.Cap.ROUND);
            for (int i = 0; i < 11; i++) {
                float x = WIDTH * (1.35f - (progress * 1.62f + i * .173f) % 1.70f);
                float y = height * (.12f + (i % 7) * .12f) + wave(i + progress * 7f) * 13;
                paint.setColor(0xfff5f5d1);
                paint.setAlpha(Math.round(strength * (70 + i % 3 * 24)));
                paint.setStrokeWidth(1.2f + i % 3);
                canvas.drawLine(x, y, x + 35 + i % 4 * 15, y - 12, paint);
            }
            paint.setAlpha(255);
        }

        private void fillPath(Canvas canvas, int color) {
            path.close();
            paint.setColor(color);
            canvas.drawPath(path, paint);
        }

        private float projectX(float worldX, float depth) {
            return WIDTH / 2 + (worldX - cameraCenter) * 150f / depth + sway;
        }

        private float ground(float depth) {
            return horizon + (height - horizon) * 1.04f / depth;
        }

        private static float bend(float worldZ) { return .24f * wave(worldZ * .51f); }

        private static float wave(float phase) { return (float) Math.sin(phase); }
    }
}

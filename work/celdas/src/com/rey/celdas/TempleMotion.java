package com.rey.celdas;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class TempleMotion {
    enum Scene { GATE, COUNTDOWN, FINAL }

    private final Dialog dialog;
    private final ValueAnimator animation;

    TempleMotion(Activity activity, Scene scene, int rank, String title, String detail, Runnable complete) {
        dialog = new Dialog(activity, android.R.style.Theme_Material_NoActionBar_Fullscreen);
        dialog.getWindow().setWindowAnimations(0);
        float density = activity.getResources().getDisplayMetrics().density;
        int padding = Math.round(20 * density);
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(padding, padding, padding, padding);
        content.setBackgroundColor(TlapohuaArt.BASALT);
        content.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(padding + insets.getSystemWindowInsetLeft(), padding + insets.getSystemWindowInsetTop(),
                padding + insets.getSystemWindowInsetRight(), padding + insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        addCaption(content, title, 24);
        addCaption(content, detail, 16);

        Stage stage = new Stage(activity, scene, rank);
        content.addView(stage, new LinearLayout.LayoutParams(-1, 0, 1));
        Button skip = new Button(activity);
        skip.setText(scene == Scene.COUNTDOWN ? "Empezar ahora" : "Saltar");
        skip.setAllCaps(false);
        skip.setTextColor(TlapohuaArt.CREAM);
        skip.setBackground(TlapohuaArt.tile(TlapohuaArt.TERRACOTTA, density, 10, true));
        content.addView(skip, new LinearLayout.LayoutParams(-1, -2));
        dialog.setContentView(content);

        animation = ValueAnimator.ofFloat(0f, 1f);
        animation.setDuration(scene == Scene.COUNTDOWN ? 2100 : 1350);
        if (scene == Scene.COUNTDOWN) animation.setInterpolator(new LinearInterpolator());
        animation.addUpdateListener(value -> {
            stage.progress = (float) value.getAnimatedValue();
            stage.invalidate();
        });
        Runnable advance = () -> {
            if (!dialog.isShowing()) return;
            dialog.dismiss();
            if (!activity.isFinishing() && !activity.isDestroyed()) complete.run();
        };
        animation.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator ignored) { advance.run(); }
        });
        skip.setOnClickListener(view -> advance.run());
        dialog.setOnDismissListener(window -> stop());
    }

    private void addCaption(LinearLayout parent, String label, int size) {
        TextView text = new TextView(parent.getContext());
        text.setText(label);
        text.setTextSize(size);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setTextColor(TlapohuaArt.CREAM);
        text.setGravity(Gravity.CENTER);
        parent.addView(text, new LinearLayout.LayoutParams(-1, -2));
    }

    void show() {
        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.getWindow().getDecorView().requestApplyInsets();
        animation.start();
    }

    private void stop() {
        animation.removeAllListeners();
        animation.cancel();
    }

    void dismiss() {
        stop();
        dialog.dismiss();
    }

    private static final class Stage extends View {
        private final Scene scene;
        private final Drawable emblem;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float progress;

        Stage(Activity activity, Scene scene, int rank) {
            super(activity);
            this.scene = scene;
            emblem = TlapohuaArt.rankSeal(rank);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        @Override protected void onDraw(Canvas canvas) {
            float scale = Math.min(getWidth(), getHeight()) / 240f;
            canvas.save();
            canvas.translate((getWidth() - 240 * scale) / 2, (getHeight() - 240 * scale) / 2);
            canvas.scale(scale, scale);
            if (scene == Scene.COUNTDOWN) {
                int step = Math.min(2, (int) (progress * 3));
                paint.setColor(0xff2b6059);
                canvas.drawCircle(120, 117, 68, paint);
                paint.setColor(TlapohuaArt.GOLD);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTypeface(Typeface.DEFAULT_BOLD);
                paint.setTextSize(86);
                canvas.drawText(String.valueOf(3 - step), 120, 147, paint);
            } else if (scene == Scene.GATE) {
                paint.setColor(0xff356b60);
                canvas.drawCircle(120, 115, 75, paint);
                emblem.setBounds(78, 73, 162, 157);
                emblem.draw(canvas);
                float shift = 110 * (1 - (1 - progress) * (1 - progress));
                canvas.clipRect(9, 40, 231, 190);
                drawDoor(canvas, 9 - shift, 120 - shift);
                drawDoor(canvas, 120 + shift, 231 + shift);
            } else {
                paint.setColor(TlapohuaArt.GOLD);
                canvas.drawCircle(120, 58, 24 * progress, paint);
                for (int i = 0; i < 7; i++) {
                    int y = 195 - 16 * i;
                    paint.setColor(progress * 8 > i ? 0xff55aaa0 : 0xff3a665d);
                    canvas.drawRect(18 + i * 12, y, 222 - i * 12, y + 14, paint);
                }
                if (progress > .65f) {
                    drawFinalBoard(canvas);
                }
            }
            canvas.restore();
        }

        private void drawFinalBoard(Canvas canvas) {
            float lift = Math.min(1f, (progress - .65f) / .35f);
            float top = 27 - 15 * lift;
            paint.setColor(TlapohuaArt.GOLD);
            canvas.drawRect(83, top, 157, top + 74, paint);
            paint.setColor(0xffcee3c7);
            canvas.drawRect(88, top + 5, 152, top + 69, paint);
            paint.setColor(TlapohuaArt.JADE);
            paint.setStrokeWidth(1);
            for (int i = 1; i < 8; i++) {
                canvas.drawLine(88 + 8 * i, top + 5, 88 + 8 * i, top + 69, paint);
                canvas.drawLine(88, top + 5 + 8 * i, 152, top + 5 + 8 * i, paint);
            }
        }

        private void drawDoor(Canvas canvas, float left, float right) {
            paint.setColor(0xff5c6d60);
            canvas.drawRect(left, 40, right, 190, paint);
            paint.setColor(0xff818e75);
            canvas.drawRect(left + 6, 46, right - 6, 184, paint);
            paint.setColor(0xff43584d);
            for (int i = 0; i < 6; i++) {
                int y = 56 + i * 22;
                canvas.drawRect(left + 12 + i * 4, y, right - 12 - i * 4, y + 4, paint);
            }
        }
    }
}

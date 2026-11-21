package com.rey.celdas;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

final class BoardReplay extends View {
    private final Puzzle puzzle;
    private final int[] cells;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private BoardAttempt.Frame last;

    private BoardReplay(Activity activity, Puzzle puzzle) {
        super(activity);
        this.puzzle = puzzle;
        cells = new int[puzzle.cage.length];
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    static void show(Activity activity, Puzzle puzzle, BoardAttempt.Review review, String result, boolean animate, Runnable done) {
        float density = activity.getResources().getDisplayMetrics().density;
        BoardReplay grid = new BoardReplay(activity, puzzle);
        TextView caption = new TextView(activity);
        caption.setTextSize(16);
        caption.setPadding(0, Math.round(8 * density), 0, 0);
        caption.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);
        caption.setText("Así quedó tu entrega.");
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        int margin = Math.round(20 * density);
        content.setPadding(margin, Math.round(8 * density), margin, 0);
        content.addView(grid, new LinearLayout.LayoutParams(-1, -2));
        content.addView(caption);
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(content);
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("Repetición de tus jugadas")
            .setView(scroll).setCancelable(false).setPositiveButton("Saltar animación", null).create();
        dialog.show();
        Button next = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        int delay = Math.min(320, 6400 / review.frames.size());
        class Playback implements Runnable {
            private int frame;

            @Override public void run() {
                if (frame == review.frames.size()) {
                    caption.setText((review.solved ? "" : "✗ Primera jugada incorrecta.\n") + result);
                    next.setText("Continuar");
                    next.setOnClickListener(view -> {
                        dialog.dismiss();
                        if (!activity.isFinishing() && !activity.isDestroyed()) done.run();
                    });
                    return;
                }
                BoardAttempt.Frame step = review.frames.get(frame++);
                grid.display(step);
                caption.setText(step.points < 0 ? "✗ Jugada " + frame + " · error"
                    : "Jugada " + frame + "   +" + step.points + " puntos");
                grid.postDelayed(this, frame == review.frames.size() ? 550 : delay);
            }

            void skip() {
                grid.removeCallbacks(this);
                while (frame < review.frames.size()) grid.display(review.frames.get(frame++));
                run();
            }
        }
        Playback playback = new Playback();
        next.setOnClickListener(view -> playback.skip());
        dialog.setOnDismissListener(window -> grid.removeCallbacks(playback));
        if (!animate) playback.skip();
        else grid.postDelayed(playback, 260);
    }

    private void display(BoardAttempt.Frame frame) {
        last = frame;
        cells[frame.cell] = frame.value;
        invalidate();
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int limit = Math.round(330 * getResources().getDisplayMetrics().density);
        setMeasuredDimension(width, Math.min(width, limit));
    }

    @Override protected void onDraw(Canvas canvas) {
        float size = getHeight() - 4f;
        if (size <= 0) return;
        canvas.save();
        canvas.translate((getWidth() - size) / 2f, 2);
        canvas.scale(size / CollectionPdf.GRID_SIDE, size / CollectionPdf.GRID_SIDE);
        float square = CollectionPdf.GRID_SIDE / (float) puzzle.size;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xfffffbef);
        canvas.drawRect(0, 0, CollectionPdf.GRID_SIDE, CollectionPdf.GRID_SIDE, paint);
        if (last != null) {
            float x = last.cell % puzzle.size * square, y = last.cell / puzzle.size * square;
            paint.setColor(last.points < 0 ? 0xfff1b4ad : 0xffc8e9cd);
            canvas.drawRect(x, y, x + square, y + square, paint);
        }
        CollectionPdf.drawGrid(canvas, paint, puzzle);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(square * .52f);
        paint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] == 0) continue;
            boolean error = last != null && last.cell == i && last.points < 0;
            paint.setColor(error ? 0xffa22923 : 0xff17463d);
            canvas.drawText(String.valueOf(cells[i]), (i % puzzle.size + .5f) * square,
                (i / puzzle.size + .78f) * square, paint);
            if (error) {
                paint.setTextSize(square * .26f);
                canvas.drawText("×", (i % puzzle.size + .82f) * square, (i / puzzle.size + .29f) * square, paint);
                paint.setTextSize(square * .52f);
            }
        }
        canvas.restore();
    }
}

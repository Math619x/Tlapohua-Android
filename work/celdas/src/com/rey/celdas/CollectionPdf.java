package com.rey.celdas;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.view.View;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

final class CollectionPdf {
    private static final int WIDTH = 612, HEIGHT = 792;
    private static final int MARGIN = 56;
    static final int GRID_SIDE = WIDTH - 2 * MARGIN;
    private static final int TOP = 164;

    private CollectionPdf() {}

    static final class Preview extends View {
        private final List<Puzzle> puzzles;
        private final Paint paper = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int index;

        Preview(Context context, List<Puzzle> puzzles) {
            super(context);
            this.puzzles = puzzles;
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        }

        void showPage(int page) {
            index = page;
            Puzzle puzzle = puzzles.get(page);
            setContentDescription(puzzle.title + ". Tablero en blanco de " + puzzle.size + " por " + puzzle.size + ".");
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            float edge = 4 * getResources().getDisplayMetrics().density;
            float scale = Math.min((getWidth() - 2 * edge) / WIDTH, (getHeight() - 2 * edge) / HEIGHT);
            if (scale <= 0) return;
            float left = (getWidth() - WIDTH * scale) / 2;
            float top = (getHeight() - HEIGHT * scale) / 2;
            paper.setColor(0x323e2c1a);
            canvas.drawRect(left + edge / 2, top + edge / 2,
                left + WIDTH * scale + edge / 2, top + HEIGHT * scale + edge / 2, paper);
            canvas.save();
            canvas.translate(left, top);
            canvas.scale(scale, scale);
            paper.setColor(Color.WHITE);
            canvas.drawRect(0, 0, WIDTH, HEIGHT, paper);
            drawPage(canvas, puzzles.get(index), index + 1, puzzles.size());
            canvas.restore();
        }
    }

    static void write(ContentResolver resolver, Uri destination, List<Puzzle> puzzles) throws IOException {
        write(resolver, destination, puzzles, "", false);
    }

    static void write(ContentResolver resolver, Uri destination, List<Puzzle> puzzles, String round, boolean answers) throws IOException {
        PdfDocument pdf = new PdfDocument();
        try {
            for (int i = 0; i < puzzles.size(); i++) {
                PdfDocument.Page page = pdf.startPage(new PdfDocument.PageInfo.Builder(WIDTH, HEIGHT, i + 1).create());
                try {
                    drawPage(page.getCanvas(), puzzles.get(i), i + 1, puzzles.size(), round, answers);
                } finally {
                    pdf.finishPage(page);
                }
            }
            try (OutputStream file = resolver.openOutputStream(destination, "wt")) {
                if (file == null) throw new IOException("No se pudo abrir el archivo.");
                pdf.writeTo(file);
            }
        } finally {
            pdf.close();
        }
    }

    private static void drawPage(Canvas canvas, Puzzle puzzle, int page, int total) {
        drawPage(canvas, puzzle, page, total, "", false);
    }

    private static void drawPage(Canvas canvas, Puzzle puzzle, int page, int total, String round, boolean answers) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(12);
        String heading = puzzle.contest ? "TLAPOHUA  /  " + (answers ? "CLAVE DE RESPUESTAS" : "ENTRENAMIENTO DE CONCURSO")
            : "TLAPOHUA  /  MI COLECCIÓN";
        canvas.drawText(heading, MARGIN, 60, paint);
        paint.setTextSize(27);
        float titleWidth = paint.measureText(puzzle.title);
        if (titleWidth > GRID_SIDE) paint.setTextSize(paint.getTextSize() * GRID_SIDE / titleWidth);
        canvas.drawText(puzzle.title, MARGIN, 98, paint);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(11);
        if (puzzle.contest) {
            canvas.drawText(round + " · Tablero " + page + " de " + total, MARGIN, 123, paint);
            canvas.drawText("No repitas en filas ni columnas. Las restas y divisiones se pueden leer en ambos órdenes.", MARGIN, 140, paint);
            canvas.drawText("Se permiten números iguales en una región si están en filas y columnas distintas.", MARGIN, 154, paint);
        } else {
            canvas.drawText("Usa los números del 1 al " + puzzle.size + " sin repetir en filas, columnas ni regiones.", MARGIN, 123, paint);
            canvas.drawText("Los números de cada región deben cumplir la operación de su pista.", MARGIN, 140, paint);
        }

        canvas.save();
        canvas.translate(MARGIN, TOP);
        drawGrid(canvas, paint, puzzle);
        if (answers) {
            float side = GRID_SIDE / (float) puzzle.size;
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(side * .48f);
            for (int i = 0; i < puzzle.solution.length; i++) {
                canvas.drawText(String.valueOf(puzzle.solution[i]),
                    (i % puzzle.size + .5f) * side, (i / puzzle.size + .76f) * side, paint);
            }
            paint.setTextAlign(Paint.Align.LEFT);
        }
        canvas.restore();

        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(11);
        canvas.drawText("Tlapohua   /   " + puzzle.size + " × " + puzzle.size, MARGIN, 719, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(page + " / " + total, WIDTH - MARGIN, 719, paint);
    }

    static void drawGrid(Canvas canvas, Paint paint, Puzzle puzzle) {
        float cellSize = GRID_SIDE / (float) puzzle.size;
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setStrokeWidth(.7f);
        paint.setColor(0xff888888);
        for (int i = 1; i < puzzle.size; i++) {
            float offset = i * cellSize;
            canvas.drawLine(offset, 0, offset, GRID_SIDE, paint);
            canvas.drawLine(0, offset, GRID_SIDE, offset, paint);
        }
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(2.2f);
        paint.setStrokeCap(Paint.Cap.SQUARE);
        for (int i = 0; i < puzzle.cage.length; i++) {
            int column = i % puzzle.size;
            int row = i / puzzle.size;
            float x = column * cellSize;
            float y = row * cellSize;
            if (column > 0 && puzzle.cage[i] != puzzle.cage[i - 1]) canvas.drawLine(x, y, x, y + cellSize, paint);
            if (row > 0 && puzzle.cage[i] != puzzle.cage[i - puzzle.size]) canvas.drawLine(x, y, x + cellSize, y, paint);
        }
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(0, 0, GRID_SIDE, GRID_SIDE, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(Math.min(20f, cellSize * .25f));
        for (int region = 0; region < puzzle.first.length; region++) {
            int cell = puzzle.first[region];
            float x = (cell % puzzle.size) * cellSize + 6;
            float y = (cell / puzzle.size) * cellSize + paint.getTextSize() + 3;
            canvas.drawText(puzzle.clue(region), x, y, paint);
        }
    }
}

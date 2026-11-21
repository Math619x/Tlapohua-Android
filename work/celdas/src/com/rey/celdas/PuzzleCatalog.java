package com.rey.celdas;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class PuzzleCatalog {
    static List<Puzzle> loadContest(Context context, String asset) throws IOException {
        List<Puzzle> puzzles = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open(asset), StandardCharsets.UTF_8))) {
            reader.readLine();
            for (String line; (line = reader.readLine()) != null; ) {
                String[] f = line.split("\t", -1);
                if (f.length != 8) throw new IOException("Entrada de concurso inválida");
                puzzles.add(new Puzzle(f[0], f[1], f[2], Integer.parseInt(f[3]), f[4], f[5], f[6], f[7]).forContest());
            }
        }
        if (puzzles.isEmpty()) throw new IOException("Catálogo de concurso vacío");
        return puzzles;
    }

    static List<Puzzle> load(Context context) throws IOException {
        // The bundled catalog is read-only. Progress is stored separately using each board's ID.
        File copy = File.createTempFile("tableros-", ".sqlite", context.getCacheDir());
        try {
            try (InputStream input = context.getAssets().open("tableros.sqlite");
                 FileOutputStream output = new FileOutputStream(copy)) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = input.read(buffer)) != -1) output.write(buffer, 0, length);
            }
            List<Puzzle> puzzles = new ArrayList<>();
            try (SQLiteDatabase database = SQLiteDatabase.openDatabase(copy.getPath(), null,
                     SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                 Cursor cursor = database.rawQuery(
                     "SELECT id, family, title, size, regions, targets, operations, solution FROM puzzles ORDER BY size, id",
                     null)) {
                while (cursor.moveToNext()) {
                    puzzles.add(new Puzzle(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getInt(3),
                        cursor.getString(4), cursor.getString(5), cursor.getString(6), cursor.getString(7)));
                }
            }
            if (puzzles.isEmpty()) throw new IOException("El catálogo no tiene tableros.");
            return puzzles;
        } finally {
            copy.delete();
        }
    }
}

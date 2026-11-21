package com.rey.celdas;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.AtomicFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

final class IntroLogo {
    private static final int MAX_SIDE = 768;

    private static AtomicFile file(Context context) {
        return new AtomicFile(new File(context.getFilesDir(), "logo-de-inicio.png"));
    }

    static synchronized Bitmap load(Context context) {
        try (FileInputStream input = file(context).openRead()) {
            return BitmapFactory.decodeStream(input);
        } catch (IOException missing) {
            return null;
        }
    }

    static synchronized boolean exists(Context context) {
        return file(context).getBaseFile().isFile();
    }

    static void save(Context context, Uri uri) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(input, null, bounds);
        }
        int longest = Math.max(bounds.outWidth, bounds.outHeight);
        if (bounds.outWidth < 1 || bounds.outHeight < 1) throw new IOException("Imagen inválida.");
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        int shortest = Math.min(bounds.outWidth, bounds.outHeight);
        while (longest / options.inSampleSize > MAX_SIDE && shortest / options.inSampleSize > 1)
            options.inSampleSize *= 2;
        if (longest / options.inSampleSize > MAX_SIDE) throw new IOException("Proporción de imagen no admitida.");
        Bitmap image;
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            image = BitmapFactory.decodeStream(input, null, options);
        }
        if (image == null) throw new IOException("Imagen inválida.");
        try {
            write(context, image);
        } finally {
            image.recycle();
        }
    }

    private static synchronized void write(Context context, Bitmap image) throws IOException {
        AtomicFile logo = file(context);
        FileOutputStream output = null;
        try {
            output = logo.startWrite();
            if (!image.compress(Bitmap.CompressFormat.PNG, 100, output)) throw new IOException("No se pudo guardar el logo.");
            logo.finishWrite(output);
            output = null;
        } finally {
            if (output != null) logo.failWrite(output);
        }
    }

    static synchronized void remove(Context context) {
        file(context).delete();
    }
}

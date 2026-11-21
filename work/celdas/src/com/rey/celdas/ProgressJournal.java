package com.rey.celdas;

import android.content.Context;
import android.util.AtomicFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.util.Map;

final class ProgressJournal implements ProgressRestore.Journal {
    private final AtomicFile file;

    ProgressJournal(Context context) {
        file = new AtomicFile(new File(context.getFilesDir(), "restauracion-pendiente.bin"));
    }

    @Override public Map<String, ?> read() throws IOException {
        if (!file.getBaseFile().isFile() && !new File(file.getBaseFile().getPath() + ".bak").isFile()) return null;
        try (FileInputStream in = file.openRead()) {
            return ProgressBackup.read(in);
        }
    }

    @Override public void write(Map<String, ?> values) throws IOException {
        FileOutputStream out = file.startWrite();
        try {
            ProgressBackup.write(new FilterOutputStream(out) {
                @Override public void close() throws IOException { flush(); }
            }, values);
            file.finishWrite(out);
        } catch (IOException | RuntimeException error) {
            file.failWrite(out);
            throw error;
        }
    }

    @Override public void clear() throws IOException {
        file.delete();
        if (file.getBaseFile().exists() || new File(file.getBaseFile().getPath() + ".bak").exists())
            throw new IOException("No se pudo cerrar la restauración.");
    }
}

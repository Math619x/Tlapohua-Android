package com.rey.celdas;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

final class ProgressRestore {
    interface Store {
        Map<String, ?> read();
        boolean replace(Map<String, ?> values);
    }

    interface Journal {
        Map<String, ?> read() throws IOException;
        void write(Map<String, ?> values) throws IOException;
        void clear() throws IOException;
    }

    static boolean recover(Store store, Journal journal) throws IOException {
        Map<String, ?> previous = journal.read();
        if (previous == null) return false;
        if (!store.replace(previous)) throw new IOException("No se pudo recuperar el progreso.");
        journal.clear();
        return true;
    }

    static void restore(Store store, Journal journal, Map<String, ?> restored) throws IOException {
        Map<String, ?> previous = new LinkedHashMap<>(store.read());
        journal.write(previous);
        if (!store.replace(restored)) {
            if (store.replace(previous)) journal.clear();
            throw new IOException("No se pudo guardar la restauración.");
        }
        journal.clear();
    }
}

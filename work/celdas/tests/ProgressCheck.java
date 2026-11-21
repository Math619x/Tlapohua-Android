package com.rey.celdas;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

final class ProgressCheck {
    public static void main(String[] args) throws Exception {
        Map<String, Object> progress = new LinkedHashMap<>();
        progress.put("friends-local-name", "Sol · águila");
        progress.put("torneo-puntos-rango", 750);
        progress.put("torneo-rango-cambio-1", -10);
        progress.put("record-size-9", 1200000L);
        progress.put("display-large-numbers", true);
        progress.put("patio-2-values", "1,0,0,1");
        char[] longHistory = new char[100000];
        Arrays.fill(longHistory, '7');
        progress.put("patio-2-attempt", new String(longHistory));
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ProgressBackup.write(stream, progress);
        byte[] bytes = stream.toByteArray();
        Map<String, Object> restored = ProgressBackup.read(new ByteArrayInputStream(bytes));
        if (!progress.equals(restored) || !(restored.get("record-size-9") instanceof Long))
            throw new AssertionError("El respaldo cambió los datos o sus tipos.");
        reject(Arrays.copyOf(bytes, bytes.length - 1));
        reject(new byte[]{1, 2, 3});
        byte[] damaged = bytes.clone();
        damaged[damaged.length - 5] ^= 1;
        reject(damaged);
        byte[] expanded;
        try (GZIPInputStream zipped = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            ByteArrayOutputStream raw = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int length; (length = zipped.read(buffer)) != -1;) raw.write(buffer, 0, length);
            expanded = raw.toByteArray();
        }
        expanded[3] = 2;
        ByteArrayOutputStream futureVersion = new ByteArrayOutputStream();
        try (GZIPOutputStream compressed = new GZIPOutputStream(futureVersion)) { compressed.write(expanded); }
        reject(futureVersion.toByteArray());
        if (!progress.equals(restored)) throw new AssertionError("La validación alteró el progreso.");
        TimedPractice original = new TimedPractice("patio-2", '+', 5, 5000, 100000);
        TimedPractice anotherPhone = original.rebased(9000000, 160000);
        if (anotherPhone.remaining(9000000, 160000) != 240000
                || original.rebased(1, 400000).remaining(1, 400000) != 0)
            throw new AssertionError("La restauración debe respetar el plazo del reloj original.");
        TimedPractice interrupted = TimedPractice.decode(original.encode());
        if (interrupted == null || interrupted.remaining(65000, 160000) != 240000
                || interrupted.remaining(305000, 400000) != 0)
            throw new AssertionError("Cerrar y volver a abrir debe conservar el plazo del reto.");
        checkRestoreTransaction(progress);
        System.out.println("Respaldo: tipos y texto largo conservados, archivos truncados o dañados rechazados, plazo entre teléfonos verificado.");
    }

    private static void checkRestoreTransaction(Map<String, Object> restored) throws Exception {
        Map<String, Object> original = new LinkedHashMap<>();
        original.put("torneo-puntos-rango", 10);
        class Store implements ProgressRestore.Store {
            Map<String, ?> state = new LinkedHashMap<>(original);
            boolean fail, interrupt;
            @Override public Map<String, ?> read() { return state; }
            @Override public boolean replace(Map<String, ?> values) {
                state = new LinkedHashMap<>(values);
                if (interrupt) { interrupt = false; throw new InterruptedRestore(); }
                if (fail) { fail = false; return false; }
                return true;
            }
        }
        class Journal implements ProgressRestore.Journal {
            Map<String, ?> saved;
            @Override public Map<String, ?> read() { return saved; }
            @Override public void write(Map<String, ?> values) { saved = new LinkedHashMap<>(values); }
            @Override public void clear() { saved = null; }
        }
        Store store = new Store();
        Journal journal = new Journal();
        ProgressRestore.restore(store, journal, restored);
        if (!store.state.equals(restored) || journal.saved != null) throw new AssertionError("Reemplazo incompleto.");
        store.state = new LinkedHashMap<>(original);
        store.fail = true;
        try {
            ProgressRestore.restore(store, journal, restored);
            throw new AssertionError("El fallo de guardado debe notificarse.");
        } catch (IOException expected) { }
        if (!store.state.equals(original) || journal.saved != null) throw new AssertionError("Se perdió el progreso ante un fallo de guardado.");
        store.interrupt = true;
        try {
            ProgressRestore.restore(store, journal, restored);
            throw new AssertionError("Falta simular la interrupción.");
        } catch (InterruptedRestore expected) { }
        if (journal.saved == null || !ProgressRestore.recover(store, journal)
                || !store.state.equals(original) || journal.saved != null || ProgressRestore.recover(store, journal))
            throw new AssertionError("La recuperación debe restaurar una instantánea completa, una sola vez.");
    }

    private static final class InterruptedRestore extends Error {
        private static final long serialVersionUID = 1L;
    }

    private static void reject(byte[] bytes) throws IOException {
        try {
            ProgressBackup.read(new ByteArrayInputStream(bytes));
            throw new AssertionError("El respaldo dañado debe rechazarse antes de restaurar.");
        } catch (IOException expected) { }
    }
}

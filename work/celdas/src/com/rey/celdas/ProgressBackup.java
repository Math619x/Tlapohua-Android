package com.rey.celdas;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

final class ProgressBackup {
    private static final int MAGIC = 0x544c5001;
    private static final int MAX_ENTRIES = 100000, MAX_STRING = 2_000_000, MAX_EXPANDED = 32_000_000;

    static void write(OutputStream destination, Map<String, ?> values) throws IOException {
        if (values.isEmpty() || values.size() > MAX_ENTRIES) throw new IOException("Cantidad de datos inválida.");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(MAGIC);
            out.writeInt(values.size());
            for (Map.Entry<String, ?> item : new TreeMap<>(values).entrySet()) {
                writeText(out, item.getKey());
                Object value = item.getValue();
                if (value instanceof String) {
                    out.writeByte(1);
                    writeText(out, (String) value);
                } else if (value instanceof Integer) {
                    out.writeByte(2);
                    out.writeInt((Integer) value);
                } else if (value instanceof Long) {
                    out.writeByte(3);
                    out.writeLong((Long) value);
                } else if (value instanceof Boolean) {
                    out.writeByte(4);
                    out.writeBoolean((Boolean) value);
                } else throw new IOException("Tipo de dato no admitido.");
                if (bytes.size() > MAX_EXPANDED) throw new IOException("El respaldo es demasiado grande.");
            }
        }
        try (GZIPOutputStream compressed = new GZIPOutputStream(destination)) {
            bytes.writeTo(compressed);
        }
    }

    static Map<String, Object> read(InputStream source) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPInputStream compressed = new GZIPInputStream(source)) {
            byte[] buffer = new byte[8192];
            for (int count; (count = compressed.read(buffer)) != -1;) {
                if (bytes.size() + count > MAX_EXPANDED) throw new IOException("El respaldo es demasiado grande.");
                bytes.write(buffer, 0, count);
            }
        }
        try (DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
            if (in.readInt() != MAGIC) throw new IOException("Formato de respaldo desconocido.");
            int count = in.readInt();
            if (count <= 0 || count > MAX_ENTRIES) throw new IOException("Cantidad de datos inválida.");
            Map<String, Object> values = new LinkedHashMap<>();
            for (int i = 0; i < count; i++) {
                String key = readText(in);
                if (key.isEmpty() || values.containsKey(key)) throw new IOException("Clave de respaldo inválida.");
                Object value;
                switch (in.readByte()) {
                    case 1: value = readText(in); break;
                    case 2: value = in.readInt(); break;
                    case 3: value = in.readLong(); break;
                    case 4: value = in.readBoolean(); break;
                    default: throw new IOException("Dato de respaldo inválido.");
                }
                values.put(key, value);
            }
            if (in.read() != -1) throw new IOException("El respaldo tiene datos adicionales.");
            return values;
        }
    }

    private static void writeText(DataOutputStream out, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_STRING) throw new IOException("Texto de respaldo demasiado largo.");
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String readText(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0 || length > MAX_STRING || length > in.available()) throw new IOException("Texto de respaldo inválido.");
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}

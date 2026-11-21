package com.rey.celdas;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

final class FriendCard {
    private static final int MAGIC = 0x544c4601;
    static final int MAX_BYTES = 1024;
    final UUID id;
    final String name;
    final long updatedAt;
    final int points, collected, rankedSolved;
    final long[] times;

    FriendCard(UUID id, String name, long updatedAt, int points, int collected, int rankedSolved, long[] times) {
        if (!validName(name) || updatedAt <= 0 || points < 0 || collected < 0 || rankedSolved < 0 || times.length != 8)
            throw new IllegalArgumentException("Ficha inválida.");
        for (long time : times) if (time < 0) throw new IllegalArgumentException("Tiempo inválido.");
        this.id = id;
        this.name = name;
        this.updatedAt = updatedAt;
        this.points = points;
        this.collected = collected;
        this.rankedSolved = rankedSolved;
        this.times = times.clone();
    }

    static boolean validName(String name) {
        if (name.trim().isEmpty() || name.length() > 24) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isISOControl(c) || Character.getType(c) == Character.FORMAT
                    || Character.getType(c) == Character.LINE_SEPARATOR || Character.getType(c) == Character.PARAGRAPH_SEPARATOR)
                return false;
        }
        return true;
    }

    byte[] encode() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(MAGIC);
            out.writeLong(id.getMostSignificantBits());
            out.writeLong(id.getLeastSignificantBits());
            out.writeUTF(name);
            out.writeLong(updatedAt);
            out.writeInt(points);
            out.writeInt(collected);
            out.writeInt(rankedSolved);
            for (long time : times) out.writeLong(time);
        }
        return bytes.toByteArray();
    }

    static FriendCard read(InputStream source) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[256];
        for (int count; (count = source.read(buffer)) != -1;) {
            if (bytes.size() + count > MAX_BYTES) throw new IOException("Ficha demasiado grande.");
            bytes.write(buffer, 0, count);
        }
        return decode(bytes.toByteArray());
    }

    static FriendCard decode(byte[] bytes) throws IOException {
        if (bytes.length > MAX_BYTES) throw new IOException("Ficha demasiado grande.");
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (in.readInt() != MAGIC) throw new IOException("Formato de ficha desconocido.");
            UUID id = new UUID(in.readLong(), in.readLong());
            String name = in.readUTF();
            long updated = in.readLong();
            int points = in.readInt(), collected = in.readInt(), rankedSolved = in.readInt();
            long[] times = new long[8];
            for (int i = 0; i < times.length; i++) times[i] = in.readLong();
            if (in.read() != -1) throw new IOException("Ficha con datos extra.");
            return new FriendCard(id, name, updated, points, collected, rankedSolved, times);
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Datos de ficha inválidos.", invalid);
        }
    }

    boolean newerThan(FriendCard other) { return other == null || updatedAt > other.updatedAt; }
}

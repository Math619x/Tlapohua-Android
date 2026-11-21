package com.rey.celdas;

import java.util.ArrayList;
import java.util.List;

final class BoardAttempt {
    static final class Entry {
        final int cell, points;
        final boolean hint;

        Entry(int cell, boolean hint, int points) {
            this.cell = cell;
            this.hint = hint;
            this.points = points;
        }
    }

    static final class Frame {
        final int cell, value, points;

        Frame(int cell, int value, int points) {
            this.cell = cell;
            this.value = value;
            this.points = points;
        }
    }

    static final class Review {
        final List<Frame> frames;
        final boolean solved;

        Review(List<Frame> frames, boolean solved) {
            this.frames = frames;
            this.solved = solved;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    BoardAttempt(int[] values, String saved) {
        boolean[] included = new boolean[values.length];
        for (String item : saved.split(";")) {
            String[] fields = item.split(",");
            if (fields.length != 3) continue;
            try {
                int cell = Integer.parseInt(fields[0]);
                int points = Integer.parseInt(fields[2]);
                if (cell >= 0 && cell < values.length && values[cell] != 0 && !included[cell] && points >= -1) {
                    entries.add(new Entry(cell, "1".equals(fields[1]), points));
                    included[cell] = true;
                }
            } catch (NumberFormatException ignored) { }
        }
        for (int i = 0; i < values.length; i++) {
            if (!included[i] && values[i] != 0) entries.add(new Entry(i, false, -1));
        }
    }

    int indexOf(int cell) {
        for (int i = 0; i < entries.size(); i++) if (entries.get(i).cell == cell) return i;
        return -1;
    }

    Entry at(int index) { return index < 0 ? null : entries.get(index); }

    boolean locked(int cell) {
        Entry entry = at(indexOf(cell));
        return entry != null && entry.points >= 0;
    }

    void set(int cell, int value, boolean hint) {
        restore(cell, entries.size(), value == 0 ? null : new Entry(cell, hint, -1));
    }

    void restore(int cell, int index, Entry entry) {
        int current = indexOf(cell);
        if (current >= 0) entries.remove(current);
        if (entry != null) entries.add(Math.min(index, entries.size()), entry);
    }

    int total() {
        int total = 0;
        for (Entry entry : entries) total += Math.max(0, entry.points);
        return total;
    }

    Review review(int[] values, int[] solution) {
        List<Frame> frames = new ArrayList<>();
        int streak = 0;
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            int value = values[entry.cell];
            if (value != solution[entry.cell]) {
                frames.add(new Frame(entry.cell, value, -1));
                for (Entry remaining : entries.subList(i, entries.size())) values[remaining.cell] = 0;
                entries.subList(i, entries.size()).clear();
                return new Review(frames, false);
            }
            int points = entry.points;
            if (points < 0) {
                points = entry.hint ? 0 : 5 * ++streak;
                entries.set(i, new Entry(entry.cell, entry.hint, points));
            }
            frames.add(new Frame(entry.cell, value, points));
        }
        return new Review(frames, true);
    }

    String encode() {
        StringBuilder saved = new StringBuilder();
        for (Entry entry : entries) {
            if (saved.length() > 0) saved.append(';');
            saved.append(entry.cell).append(',').append(entry.hint ? 1 : 0).append(',').append(entry.points);
        }
        return saved.toString();
    }
}

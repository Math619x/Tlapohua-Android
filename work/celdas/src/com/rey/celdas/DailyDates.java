package com.rey.celdas;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

final class DailyDates {
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final Locale MEXICO = new Locale("es", "MX");
    private static final long DAY_MS = 86400000L;

    private DailyDates() { }

    static Calendar today() {
        Calendar local = Calendar.getInstance();
        return date(local.get(Calendar.YEAR), local.get(Calendar.MONTH) + 1, local.get(Calendar.DAY_OF_MONTH));
    }

    static Calendar parse(String key) {
        if (!key.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) return null;
        try {
            return date(Integer.parseInt(key.substring(0, 4)), Integer.parseInt(key.substring(5, 7)),
                Integer.parseInt(key.substring(8, 10)));
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    private static Calendar date(int year, int month, int day) {
        Calendar utc = new GregorianCalendar(UTC, Locale.ROOT);
        utc.clear();
        utc.setLenient(false);
        utc.set(year, month - 1, day);
        utc.getTimeInMillis();
        return utc;
    }

    static Calendar month(Calendar day) {
        return date(day.get(Calendar.YEAR), day.get(Calendar.MONTH) + 1, 1);
    }

    static int leadingDays(Calendar month) {
        return (month.get(Calendar.DAY_OF_WEEK) + 5) % 7;
    }

    static String key(Calendar day) {
        return String.format(Locale.ROOT, "%04d-%02d-%02d",
            day.get(Calendar.YEAR), day.get(Calendar.MONTH) + 1, day.get(Calendar.DAY_OF_MONTH));
    }

    static String label(Calendar day, String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, MEXICO);
        format.setTimeZone(UTC);
        return format.format(day.getTime());
    }

    static List<Puzzle> pool(List<Puzzle> catalog) {
        List<Puzzle> pool = new ArrayList<>();
        for (Puzzle puzzle : catalog) {
            if (puzzle.size >= 4 && puzzle.size <= 6 && !puzzle.family.equals("archivo")) pool.add(puzzle);
        }
        Collections.sort(pool, (left, right) -> {
            int sizeOrder = Integer.compare(left.size, right.size);
            return sizeOrder != 0 ? sizeOrder : left.id.compareTo(right.id);
        });
        return pool;
    }

    static Puzzle boardFor(Calendar day, List<Puzzle> pool) {
        long index = day.getTimeInMillis() / DAY_MS % pool.size();
        return pool.get((int) ((index + pool.size()) % pool.size()));
    }
}

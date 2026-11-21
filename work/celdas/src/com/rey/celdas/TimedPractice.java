package com.rey.celdas;

final class TimedPractice {
    final String boardId;
    final char operation;
    final int minutes;
    private final long elapsedDeadline, wallDeadline;

    TimedPractice(String boardId, char operation, int minutes, long elapsed, long wall) {
        this.boardId = boardId;
        this.operation = operation;
        this.minutes = minutes;
        long duration = minutes * 60_000L;
        elapsedDeadline = elapsed + duration;
        wallDeadline = wall + duration;
    }

    long remaining(long elapsed, long wall) {
        return Math.max(0, Math.min(minutes * 60_000L, Math.min(elapsedDeadline - elapsed, wallDeadline - wall)));
    }

    TimedPractice rebased(long elapsed, long wall) {
        long duration = minutes * 60_000L;
        long remaining = Math.max(0, Math.min(duration, wallDeadline - wall));
        return new TimedPractice(boardId, operation, minutes, elapsed - duration + remaining, wallDeadline - duration);
    }

    String encode() { return boardId + ":" + operation + ":" + minutes + ":" + elapsedDeadline + ":" + wallDeadline; }

    static TimedPractice decode(String encoded) {
        String[] fields = encoded.split(":", -1);
        if (fields.length != 5 || fields[0].isEmpty() || fields[1].length() != 1 || "+-*/".indexOf(fields[1].charAt(0)) < 0) return null;
        try {
            int minutes = Integer.parseInt(fields[2]);
            if (minutes != 5 && minutes != 10 && minutes != 15) return null;
            long duration = minutes * 60_000L;
            return new TimedPractice(fields[0], fields[1].charAt(0), minutes,
                Long.parseLong(fields[3]) - duration, Long.parseLong(fields[4]) - duration);
        } catch (NumberFormatException invalid) {
            return null;
        }
    }
}

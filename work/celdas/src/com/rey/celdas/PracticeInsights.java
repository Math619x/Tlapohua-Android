package com.rey.celdas;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class PracticeInsights {
    static final class OperationProgress {
        final char operation;
        int correct, errors, incomplete;

        OperationProgress(char operation) { this.operation = operation; }
        int total() { return correct + errors + incomplete; }
    }
    static final class Focus {
        final int size;
        final char operation;
        int regions;

        Focus(int size, char operation) { this.size = size; this.operation = operation; }
        String key() { return size + "-" + operation; }
    }

    static String operationName(char operation) {
        switch (operation) {
            case '+': return "Sumas";
            case '-': return "Restas";
            case '*': return "Multiplicaciones";
            case '/': return "Divisiones";
            default: throw new IllegalArgumentException("Operación inválida");
        }
    }

    static List<Focus> recent(List<ContestRun> records) {
        Map<String, Focus> counts = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(3, records.size()); i++) {
            ContestRun run = records.get(i);
            for (int round = 0; round <= run.round; round++) addRound(counts, run, round);
        }
        List<Focus> result = new ArrayList<>(counts.values());
        java.util.Collections.sort(result, (a, b) -> Integer.compare(b.regions, a.regions));
        return result;
    }

    static List<OperationProgress> progress(List<ContestRun> records) {
        List<OperationProgress> result = new ArrayList<>();
        for (char operation : new char[]{'+', '-', '*', '/'}) {
            OperationProgress summary = new OperationProgress(operation);
            for (ContestRun run : records) {
                for (int round = 0; round <= run.round; round++) {
                    if (run.usedTime(round) < 0) continue;
                    int start = round * ContestRun.PER_ROUND;
                    for (int i = start; i < start + run.boardCount(round); i++) {
                        Puzzle board = run.boards[i];
                        for (int group = 0; group < board.first.length; group++) {
                            if (board.operationAt(board.first[group]) != operation) continue;
                            boolean error = false, empty = false;
                            for (int cell = 0; cell < board.cage.length; cell++) {
                                if (board.cage[cell] != group) continue;
                                int value = run.values[i][cell];
                                empty |= value == 0;
                                error |= value != 0 && value != board.solution[cell];
                            }
                            if (error) summary.errors++;
                            else if (empty) summary.incomplete++;
                            else summary.correct++;
                        }
                    }
                }
            }
            result.add(summary);
        }
        return result;
    }

    private static void addRound(Map<String, Focus> counts, ContestRun run, int round) {
        if (run.usedTime(round) < 0) return;
        int first = round * ContestRun.PER_ROUND;
        for (int i = first; i < first + run.boardCount(round); i++) {
            Puzzle puzzle = run.boards[i];
            boolean[] counted = new boolean[puzzle.first.length];
            for (int cell = 0; cell < puzzle.cage.length; cell++) {
                int group = puzzle.cage[cell], value = run.values[i][cell];
                char operation = puzzle.operationAt(cell);
                if (value == 0 || value == puzzle.solution[cell] || operation == '=' || counted[group]) continue;
                counted[group] = true;
                Focus focus = new Focus(puzzle.size, operation);
                Focus previous = counts.get(focus.key());
                if (previous == null) {
                    previous = focus;
                    counts.put(focus.key(), focus);
                }
                previous.regions++;
            }
        }
    }

    static String report(List<ContestRun> records) {
        StringBuilder csv = new StringBuilder("\ufeff");
        append(csv, "Fecha", "Modo", "División", "Ronda", "Tableros correctos", "Tableros de la ronda",
            "Casillas correctas", "Casillas de la ronda", "Segundos", "Puntos", "Operaciones para repasar");
        SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        for (ContestRun run : records) {
            for (int round = 0; round <= run.round; round++) {
                if (run.usedTime(round) < 0) continue;
                int first = round * ContestRun.PER_ROUND;
                int cells = 0, correct = 0;
                for (int i = first; i < first + run.boardCount(round); i++) {
                    cells += run.boards[i].cage.length;
                    correct += run.correctCells(i);
                }
                Map<String, Focus> focus = new LinkedHashMap<>();
                addRound(focus, run, round);
                StringBuilder summary = new StringBuilder();
                for (Focus item : focus.values()) {
                    if (summary.length() > 0) summary.append(" · ");
                    summary.append(operationName(item.operation)).append(' ').append(item.size)
                        .append('×').append(item.size).append(" (").append(item.regions).append(')');
                }
                append(csv, date.format(new Date(run.id)), run.ranked ? "Cumbre de Turquesa" : run.pro ? "Entrenamiento Pro" : "Entrenamiento Estándar",
                    ContestRun.LEVELS[run.level], run.roundTitle(round), run.solved(round), run.boardCount(round), correct,
                    cells, run.usedTime(round) / 1000L, run.score(round), summary);
            }
        }
        return csv.toString();
    }

    private static void append(StringBuilder csv, Object... fields) {
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) csv.append(',');
            csv.append('"').append(fields[i].toString().replace("\"", "\"\"")).append('"');
        }
        csv.append("\r\n");
    }
}

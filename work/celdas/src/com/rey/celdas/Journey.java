package com.rey.celdas;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class Journey {
    static final int[] SIZES = {3, 4, 5, 6, 7, 8, 9};
    static final String[] PLACES = {"Entrada del bosque", "Claro de la ceiba", "Cruce del arroyo",
        "Sendero entre piedras", "Mirador del bosque", "Terrazas de obsidiana", "Cumbre de jade"};
    static final int ATTEMPTS = 3;
    static final int CLEAN_WINS = 3;
    static final int TOTAL_WINS = 5;
    private final Set<String> completed = new LinkedHashSet<>();
    int stage, mistakes, wins, streak;
    boolean finished;
    private boolean boardHadError;
    String active = "";

    Journey(String saved) {
        String[] fields = saved.split("\\|", -1);
        try {
            if (fields.length == 9 && fields[0].equals("v2")) {
                stage = Math.max(0, Math.min(SIZES.length - 1, Integer.parseInt(fields[1])));
                mistakes = Integer.parseInt(fields[2]);
                finished = fields[3].equals("1");
                wins = Integer.parseInt(fields[4]);
                streak = Integer.parseInt(fields[5]);
                boardHadError = fields[6].equals("1");
                active = fields[7];
                for (String id : fields[8].split(",")) if (!id.isEmpty()) completed.add(id);
            } else if (fields.length == 8) {
                boolean oldFinished = fields[2].equals("1");
                stage = oldFinished ? 5 : Math.max(0, Math.min(4, Integer.parseInt(fields[0])));
                for (int i = 0; i < stage; i++) if (!fields[3 + i].isEmpty()) completed.add(fields[3 + i]);
                if (!oldFinished) {
                    active = fields[3 + stage];
                    mistakes = Math.max(0, Math.min(ATTEMPTS - 1, Integer.parseInt(fields[1])));
                    boardHadError = mistakes > 0;
                }
            }
        } catch (NumberFormatException ignored) {
            restart();
        }
    }

    String encode() {
        StringBuilder result = new StringBuilder("v2|").append(stage).append('|').append(mistakes).append('|')
            .append(finished ? 1 : 0).append('|').append(wins).append('|').append(streak).append('|')
            .append(boardHadError ? 1 : 0).append('|').append(active).append('|');
        boolean first = true;
        for (String id : completed) {
            if (!first) result.append(',');
            result.append(id);
            first = false;
        }
        return result.toString();
    }

    List<Puzzle> choices(List<Puzzle> catalog, ForestBoards generated) {
        List<Puzzle> options = new ArrayList<>();
        if (finished) return options;
        int size = SIZES[stage];
        int limit = stage % 2 == 0 ? 3 : 2;
        for (Puzzle puzzle : catalog) {
            if (puzzle.size != size || puzzle.family.equals("archivo") || completed.contains(puzzle.id)) continue;
            if (stage < 2 && !puzzle.hasMixedOperations()) continue;
            options.add(puzzle);
            if (options.size() == limit) return options;
        }
        generated.fillOptions(size, limit, catalog, completed, options);
        return options;
    }

    boolean recordWin() {
        if (active.isEmpty() || !completed.add(active)) return false;
        wins++;
        if (!boardHadError) streak++;
        return true;
    }

    boolean qualified() {
        return streak >= CLEAN_WINS || wins >= TOTAL_WINS;
    }

    void continueAfterWin() {
        if (active.isEmpty() || !completed.contains(active)) return;
        active = "";
        boardHadError = false;
        if (!qualified()) return;
        wins = streak = mistakes = 0;
        if (stage == SIZES.length - 1) finished = true;
        else stage++;
    }

    boolean fail() {
        return fail(false);
    }

    boolean fail(boolean casual) {
        boardHadError = true;
        streak = 0;
        mistakes++;
        if (mistakes < ATTEMPTS) return false;
        if (casual) {
            mistakes = 0;
            active = "";
        } else resetLevel();
        return true;
    }

    void restart() {
        stage = 0;
        finished = false;
        resetLevel();
    }

    private void resetLevel() {
        wins = streak = mistakes = 0;
        active = "";
        boardHadError = false;
    }
}

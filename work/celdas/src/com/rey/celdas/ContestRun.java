package com.rey.celdas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

final class ContestRun {
    static final String[] LEVELS = {"Inicial", "Intermedio", "Avanzado"};
    static final int ROUNDS = 3, PER_ROUND = 3;
    static final int FINAL_MINUTES = 20, QUALIFY = 7;
    static final int RANK_BOARD_POINTS = 50, RANK_FINAL_POINTS = 100;
    static final int RANK_LOSS_PER_MISSING = 10, RANK_MAX_LOSS = 30;
    private static final String[] RANKS = {"Madera", "Cobre", "Obsidiana", "Jade", "Turquesa"};
    private static final int[] THRESHOLDS = {0, 300, 750, 1400, 2200};
    private static final int[][][] SIZES = {
        {{4, 4, 4}, {4, 4, 5}, {4, 5, 5}},
        {{4, 4, 5}, {4, 5, 5}, {5, 6, 6}},
        {{5, 5, 6}, {5, 6, 6}, {6, 7, 7}}
    };
    private static final int[] STANDARD = {18, 22, 25};
    private static final int[] PRO = {15, 20, 22};

    final boolean pro;
    final boolean ranked;
    final int level;
    final long id;
    final Puzzle[] boards;
    final int[][] values;
    final int[][] notes;
    private final long[] spent;
    int round, board;
    private long endElapsed, endWall;

    private ContestRun(boolean pro, int level, long id, Puzzle[] boards) {
        this.pro = pro;
        ranked = boards.length > ROUNDS * PER_ROUND;
        this.level = level;
        this.id = id;
        this.boards = boards;
        values = new int[boards.length][];
        notes = new int[boards.length][];
        spent = new long[ranked ? ROUNDS + 1 : ROUNDS];
        Arrays.fill(spent, -1);
        for (int i = 0; i < boards.length; i++) {
            values[i] = new int[boards[i].cage.length];
            notes[i] = new int[boards[i].cage.length];
        }
    }

    static ContestRun create(boolean pro, int level, long id, List<Puzzle> catalog, Set<String> postponed, Random random) {
        return new ContestRun(pro, level, id, selectBoards(level, catalog, postponed, random));
    }

    static ContestRun createRanked(int level, long id, List<Puzzle> catalog, Set<String> postponed, Random random) {
        Puzzle[] qualifiers = selectBoards(level, catalog, postponed, random);
        List<Puzzle> finals = new ArrayList<>();
        for (Puzzle puzzle : catalog) {
            if (puzzle.size == 8 && !postponed.contains(puzzle.id)) finals.add(puzzle);
        }
        if (finals.isEmpty()) throw new IllegalArgumentException("Faltan tableros para la final.");
        Puzzle[] boards = Arrays.copyOf(qualifiers, qualifiers.length + 1);
        boards[qualifiers.length] = finals.get(random.nextInt(finals.size()));
        return new ContestRun(false, level, id, boards);
    }

    private static Puzzle[] selectBoards(int level, List<Puzzle> catalog, Set<String> postponed, Random random) {
        List<Puzzle> available = new ArrayList<>(catalog);
        Collections.shuffle(available, random);
        Set<String> used = new HashSet<>();
        Puzzle[] boards = new Puzzle[ROUNDS * PER_ROUND];
        for (int round = 0; round < ROUNDS; round++) {
            for (int board = 0; board < PER_ROUND; board++) {
                int size = SIZES[level][round][board];
                int slot = round * PER_ROUND + board;
                for (Puzzle entry : available) {
                    if (entry.size == size && (level != 0 || entry.basicOperations())
                            && !postponed.contains(entry.id) && used.add(entry.id)) {
                        boards[slot] = entry;
                        break;
                    }
                }
                if (boards[slot] == null) throw new IllegalArgumentException("Faltan tableros de concurso.");
            }
        }
        return boards;
    }

    static int minutes(boolean pro, int round) { return (pro ? PRO : STANDARD)[round]; }

    int roundMinutes(int index) { return index == ROUNDS ? FINAL_MINUTES : minutes(pro, index); }

    private long limit(int index) { return roundMinutes(index) * 60000L; }

    String label() { return (ranked ? "División" : pro ? "Pro ·" : "Estándar ·") + " " + LEVELS[level]; }

    String roundTitle(int index) { return index == ROUNDS ? "Final de la Cumbre" : "Ronda " + (index + 1) + " de " + ROUNDS; }

    int boardCount(int index) { return index == ROUNDS ? 1 : PER_ROUND; }

    int index(int slot) { return round * PER_ROUND + slot; }

    boolean roundFinished() { return spent[round] >= 0; }

    boolean finished() {
        return roundFinished() && (round == ROUNDS || (round == ROUNDS - 1 && (!ranked || !qualified())));
    }

    int qualifierSolved() {
        int count = 0;
        for (int i = 0; i < ROUNDS; i++) if (spent[i] >= 0) count += solved(i);
        return count;
    }

    boolean qualified() { return ranked && qualifierSolved() >= QUALIFY; }

    int rankPoints() {
        if (!ranked || !finished()) return 0;
        return qualifierSolved() * RANK_BOARD_POINTS + (round == ROUNDS ? solved(ROUNDS) * RANK_FINAL_POINTS : 0);
    }

    int rankChange(int previousPoints) {
        if (!ranked || !finished()) return 0;
        if (qualified()) return rankPoints();
        int penalty = (QUALIFY - qualifierSolved()) * RANK_LOSS_PER_MISSING;
        return -Math.min(previousPoints, Math.min(penalty, RANK_MAX_LOSS));
    }

    static int rankIndex(int points) {
        int index = 0;
        while (index + 1 < THRESHOLDS.length && points >= THRESHOLDS[index + 1]) index++;
        return index;
    }

    static String rankName(int index) { return RANKS[index]; }

    static int rankThreshold(int index) { return THRESHOLDS[index]; }

    static boolean hasNextRank(int index) { return index + 1 < RANKS.length; }

    boolean running() { return endWall > 0 && !roundFinished(); }

    void start(long elapsed, long wall) {
        if (running() || roundFinished()) return;
        endElapsed = elapsed + limit(round);
        endWall = wall + limit(round);
    }

    long remaining(long elapsed, long wall) {
        if (!running()) return roundFinished() ? limit(round) - spent[round] : limit(round);
        return Math.max(0, Math.min(limit(round), Math.min(endElapsed - elapsed, endWall - wall)));
    }

    void rebaseClock(long elapsed, long wall) {
        if (running()) endElapsed = elapsed + Math.max(0, Math.min(limit(round), endWall - wall));
    }

    void finishRound(long elapsed, long wall) {
        if (running()) spent[round] = limit(round) - remaining(elapsed, wall);
    }

    void nextRound() {
        if (!roundFinished() || finished()) return;
        round++;
        board = 0;
        endElapsed = endWall = 0;
    }

    long usedTime(int index) { return spent[index]; }

    int solved(int round) {
        int count = 0;
        int start = round * PER_ROUND;
        for (int i = start; i < start + boardCount(round); i++)
            if (boards[i].solved(values[i])) count++;
        return count;
    }

    int correctCells(int index) {
        int count = 0;
        for (int i = 0; i < values[index].length; i++)
            if (values[index][i] == boards[index].solution[i]) count++;
        return count;
    }

    int score(int round) {
        if (spent[round] < 0) return 0;
        int count = solved(round);
        int bonus = count == boardCount(round) ? (int) ((limit(round) - spent[round]) / 10000L) * 5 : 0;
        return 1000 * count + bonus;
    }

    int score() {
        int score = 0;
        for (int i = 0; i < spent.length; i++) score += score(i);
        return score;
    }

    String encode() {
        StringBuilder out = new StringBuilder(ranked ? "2|" : "1|").append(pro ? 1 : 0).append('|').append(level).append('|')
            .append(id).append('|').append(round).append('|').append(board).append('|').append(endElapsed).append('|')
            .append(endWall);
        for (long time : spent) out.append('|').append(time);
        for (int i = 0; i < boards.length; i++) {
            out.append('|').append(boards[i].id).append(':');
            for (int cell : values[i]) out.append(cell);
            out.append(':');
            for (int cell : notes[i]) out.append(Integer.toString(cell, 36)).append(',');
        }
        return out.toString();
    }

    static ContestRun decode(String encoded, Map<String, Puzzle> catalog) {
        String[] fields = encoded.split("\\|", -1);
        boolean ranked = fields[0].equals("2");
        int rounds = ranked ? ROUNDS + 1 : ROUNDS;
        int boardCount = ROUNDS * PER_ROUND + (ranked ? 1 : 0);
        if (fields.length != 8 + rounds + boardCount || (!ranked && !fields[0].equals("1"))) return null;
        try {
            int level = Integer.parseInt(fields[2]), round = Integer.parseInt(fields[4]), board = Integer.parseInt(fields[5]);
            if (level < 0 || level >= LEVELS.length || round < 0 || round >= rounds
                    || board < 0 || board >= (round == ROUNDS ? 1 : PER_ROUND))
                return null;
            Puzzle[] puzzles = new Puzzle[boardCount];
            String[][] cells = new String[puzzles.length][];
            for (int i = 0; i < puzzles.length; i++) {
                cells[i] = fields[8 + rounds + i].split(":", -1);
                if (cells[i].length != 3) return null;
                puzzles[i] = catalog.get(cells[i][0]);
                if (puzzles[i] == null || cells[i][1].length() != puzzles[i].cage.length) return null;
            }
            ContestRun run = new ContestRun(fields[1].equals("1"), level, Long.parseLong(fields[3]), puzzles);
            run.round = round;
            run.board = board;
            run.endElapsed = Long.parseLong(fields[6]);
            run.endWall = Long.parseLong(fields[7]);
            for (int i = 0; i < rounds; i++) run.spent[i] = Long.parseLong(fields[8 + i]);
            for (int i = 0; i < puzzles.length; i++) {
                String[] hints = cells[i][2].split(",");
                if (hints.length != run.notes[i].length) return null;
                for (int j = 0; j < run.values[i].length; j++) {
                    int value = cells[i][1].charAt(j) - '0';
                    int note = Integer.parseInt(hints[j], 36);
                    if (value < 0 || value > puzzles[i].size || note < 0
                            || note >= ((puzzles[i].size + 1) << puzzles[i].size)) return null;
                    run.values[i][j] = value;
                    run.notes[i][j] = note;
                }
            }
            return run;
        } catch (NumberFormatException invalid) {
            return null;
        }
    }
}

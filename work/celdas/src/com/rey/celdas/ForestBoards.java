package com.rey.celdas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

final class ForestBoards {
    static final String PREFIX = "ruta-v1-";
    private final Map<String, Puzzle> cache = new HashMap<>();

    Puzzle find(String id) {
        if (!id.startsWith(PREFIX)) return null;
        Puzzle cached = cache.get(id);
        if (cached != null) return cached;
        String[] parts = id.substring(PREFIX.length()).split("-", -1);
        if (parts.length != 2) return null;
        try {
            int size = Integer.parseInt(parts[0]), number = Integer.parseInt(parts[1]);
            if (size < 3 || size > 9 || number < 1) return null;
            Puzzle generated = generate(id, size, number);
            cache.put(id, generated);
            return generated;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    void fillOptions(int size, int limit, List<Puzzle> catalog, Set<String> completed, List<Puzzle> options) {
        Set<String> seen = new HashSet<>();
        for (Puzzle entry : catalog) if (entry.size == size) seen.add(signature(entry));
        String prefix = PREFIX + size + "-";
        int number = 1;
        for (String id : completed) {
            if (!id.startsWith(prefix)) continue;
            Puzzle entry = find(id);
            if (entry == null) continue;
            seen.add(signature(entry));
            number = Math.max(number, Integer.parseInt(id.substring(prefix.length())) + 1);
        }
        while (options.size() < limit) {
            Puzzle entry = find(prefix + number++);
            if (entry.hasMixedOperations() && seen.add(signature(entry))) options.add(entry);
        }
    }

    private static String signature(Puzzle puzzle) {
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < puzzle.cage.length; i++) {
            int group = puzzle.cage[i];
            key.append(puzzle.first[group]).append(':');
            if (i == puzzle.first[group]) key.append(puzzle.clue(group));
            key.append(',');
        }
        return key.toString();
    }

    private static Puzzle generate(String id, int size, int number) {
        // Saved IDs reproduce these boards, including after an update or a route reset.
        Random random = new Random(48371L + size * 97003L + number * 15485863L);
        List<Integer> rows = permutation(size, random);
        List<Integer> columns = permutation(size, random);
        List<Integer> digits = permutation(size, random);
        int[] solution = new int[size * size];
        for (int i = 0; i < solution.length; i++) {
            solution[i] = digits.get((rows.get(i / size) + columns.get(i % size)) % size) + 1;
        }
        List<Region> regions = new ArrayList<>();
        boolean[] assigned = new boolean[solution.length];
        for (int start = 0; start < assigned.length; start++) {
            if (assigned[start]) continue;
            List<Integer> cells = new ArrayList<>();
            cells.add(start);
            assigned[start] = true;
            int targetSize = 2 + (random.nextInt(4) == 0 ? 1 : 0);
            while (cells.size() < targetSize) {
                List<Integer> neighbors = new ArrayList<>();
                for (int cell : cells) {
                    int column = cell % size;
                    if (column > 0) addNeighbor(neighbors, cells, assigned, solution, cell - 1);
                    if (column + 1 < size) addNeighbor(neighbors, cells, assigned, solution, cell + 1);
                    if (cell >= size) addNeighbor(neighbors, cells, assigned, solution, cell - size);
                    if (cell + size < assigned.length) addNeighbor(neighbors, cells, assigned, solution, cell + size);
                }
                if (neighbors.isEmpty()) break;
                int next = neighbors.get(random.nextInt(neighbors.size()));
                cells.add(next);
                assigned[next] = true;
            }
            Collections.sort(cells);
            regions.add(new Region(cells, operation(cells, solution, random)));
        }
        Puzzle puzzle = board(id, size, number, solution, regions);
        List<Region> splittable = new ArrayList<>();
        for (Region region : regions) if (region.cells.size() > 1) splittable.add(region);
        Collections.shuffle(splittable, random);
        for (Region region : splittable) {
            if (puzzle.countSolutions() == 1) return puzzle;
            regions.remove(region);
            for (int cell : region.cells) regions.add(new Region(Collections.singletonList(cell), '='));
            puzzle = board(id, size, number, solution, regions);
        }
        return puzzle;
    }

    private static List<Integer> permutation(int size, Random random) {
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < size; i++) values.add(i);
        Collections.shuffle(values, random);
        return values;
    }

    private static void addNeighbor(List<Integer> neighbors, List<Integer> cells, boolean[] assigned, int[] solution, int next) {
        if (assigned[next] || neighbors.contains(next)) return;
        for (int cell : cells) if (solution[cell] == solution[next]) return;
        neighbors.add(next);
    }

    private static char operation(List<Integer> cells, int[] solution, Random random) {
        if (cells.size() == 1) return '=';
        String choices = "+*";
        if (cells.size() == 2) {
            int first = solution[cells.get(0)], second = solution[cells.get(1)];
            if (first > second) choices = first % second == 0 ? "+*--//" : "+*--";
        }
        return choices.charAt(random.nextInt(choices.length()));
    }

    private static Puzzle board(String id, int size, int number, int[] solution, List<Region> regions) {
        int[] cage = new int[solution.length];
        int[] targets = new int[regions.size()];
        StringBuilder operations = new StringBuilder();
        StringBuilder answer = new StringBuilder();
        for (int value : solution) answer.append(value);
        for (int i = 0; i < regions.size(); i++) {
            Region region = regions.get(i);
            int sum = 0, product = 1;
            for (int cell : region.cells) {
                cage[cell] = i;
                sum += solution[cell];
                product *= solution[cell];
            }
            int first = solution[region.cells.get(0)];
            char op = region.operation;
            if (op == '-') targets[i] = first - solution[region.cells.get(1)];
            else if (op == '/') targets[i] = first / solution[region.cells.get(1)];
            else targets[i] = op == '*' ? product : sum;
            operations.append(op);
        }
        String family = size <= 4 ? "patio" : size <= 6 ? "plaza" : "senderos";
        return new Puzzle(id, family, "Mapa del bosque " + size + "×" + size + " · " + number, size,
            join(cage), join(targets), operations.toString(), answer.toString());
    }

    private static String join(int[] values) {
        StringBuilder output = new StringBuilder();
        for (int value : values) {
            if (output.length() > 0) output.append(',');
            output.append(value);
        }
        return output.toString();
    }

    private static final class Region {
        final List<Integer> cells;
        final char operation;

        Region(List<Integer> cells, char operation) {
            this.cells = cells;
            this.operation = operation;
        }
    }
}

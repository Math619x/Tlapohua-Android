package com.rey.celdas;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

final class ContestCheck {
    public static void main(String[] args) throws java.io.IOException {
        List<String> audit = new ArrayList<>();
        Map<String, Puzzle> eligible = new LinkedHashMap<>();
        audit.add("id\tfamily\ttitle\tsize\tregions\ttargets\toperations\tsolution");
        Set<String> classicIds = new HashSet<>(Files.readAllLines(Paths.get(args[2]), StandardCharsets.UTF_8));
        Set<String> layouts = new HashSet<>();
        int[] counts = new int[10];
        int added = 0;
        Puzzle division = null, repeated = null;
        for (int input = 0; input < 2; input++) {
            List<String> lines = Files.readAllLines(Paths.get(args[input]), StandardCharsets.UTF_8);
            for (int row = 1; row < lines.size(); row++) {
                String[] f = lines.get(row).split("\t", -1);
                int size = Integer.parseInt(f[3]);
                if (size < 4 || size > 7 || f[1].equals("archivo")) continue;
                if (input == 0 && !classicIds.remove(f[0])) continue;
                Puzzle puzzle = new Puzzle(f[0], f[1], f[2], size, f[4], f[5], f[6], f[7]).forContest();
                if (!puzzle.solved(puzzle.solution) || puzzle.countSolutions() != 1) {
                    throw new AssertionError("Tablero de concurso inválido: " + puzzle.id);
                }
                if (eligible.put(puzzle.id, puzzle) != null) throw new AssertionError("ID duplicado");
                StringBuilder signature = new StringBuilder().append(size).append(':');
                for (int cell = 0; cell < puzzle.cage.length; cell++) {
                    int group = puzzle.cage[cell], first = puzzle.first[group];
                    signature.append(first).append(':');
                    if (cell == first) signature.append(puzzle.clue(group));
                    signature.append(',');
                }
                if (!layouts.add(signature.toString())) throw new AssertionError("Cuadrícula repetida: " + puzzle.id);
                counts[size]++;
                audit.add(lines.get(row));
                if (input == 1) added++;
                if (puzzle.id.equals("reto")) division = puzzle;
                if (puzzle.id.equals("concurso-mixto-4-01")) repeated = puzzle;
            }
        }
        if (!classicIds.isEmpty()) throw new AssertionError("Faltan tableros del catálogo clásico: " + classicIds);
        for (int size = 4; size <= 7; size++) {
            if (counts[size] != 50) throw new AssertionError("El concurso requiere 50 tableros de " + size + "×" + size);
        }
        if (division == null || repeated == null) throw new AssertionError("Faltan tableros de control");
        int[] pair = new int[25];
        pair[0] = 1;
        pair[1] = 4;
        if (!division.cageComplete(pair, 0)) throw new AssertionError("La división de concurso acepta ambas orientaciones");
        boolean repeatedWithinCage = false;
        for (int i = 0; i < repeated.cage.length; i++) {
            for (int j = i + 1; j < repeated.cage.length; j++) {
                if (repeated.cage[i] == repeated.cage[j] && repeated.solution[i] == repeated.solution[j]) {
                    if (repeated.related(i, j)) throw new AssertionError("Se bloqueó un dígito en diagonal");
                    repeatedWithinCage = true;
                }
            }
        }
        if (!repeatedWithinCage) throw new AssertionError("Falta un ejemplo de repetición válida");
        if (ContestRun.minutes(false, 0) != 18 || ContestRun.minutes(false, 1) != 22 || ContestRun.minutes(false, 2) != 25
                || ContestRun.minutes(true, 0) != 15 || ContestRun.minutes(true, 1) != 20 || ContestRun.minutes(true, 2) != 22)
            throw new AssertionError("Duraciones incorrectas");
        for (int level = 0; level < ContestRun.LEVELS.length; level++) {
            ContestRun run = ContestRun.create(true, level, 42, new ArrayList<>(eligible.values()), Collections.emptySet(), new Random(12));
            if (Arrays.stream(run.boards).map(p -> p.id).distinct().count() != 9) throw new AssertionError("Tablero repetido");
            if (level == 0 && Arrays.stream(run.boards).anyMatch(p -> !p.basicOperations())) throw new AssertionError("Perfil inicial incorrecto");
            int minSize = level == 2 ? 5 : 4, maxSize = level == 0 ? 5 : level == 1 ? 6 : 7;
            for (Puzzle selected : run.boards)
                if (selected.size < minSize || selected.size > maxSize)
                    throw new AssertionError("Un tablero quedó fuera del perfil de tamaño de su división.");
            run.start(1000, 2000);
            run.notes[1][0] = 2;
            run.notes[0][0] = run.boards[0].size << run.boards[0].size;
            run.values[2][0] = 1;
            run.board = 2;
            run = ContestRun.decode(run.encode(), eligible);
            if (run == null || run.board != 2 || run.notes[1][0] != 2 || run.values[2][0] != 1
                    || run.notes[0][0] != run.boards[0].size << run.boards[0].size || run.values[0][0] != 0
                    || run.remaining(11000, 12000) != 890000) throw new AssertionError("No se recuperó la ronda");
            if (run.remaining(500, 12000) != 890000) throw new AssertionError("Un reinicio no puede alargar el reloj");
            for (int i = 0; i < 3; i++) run.values[i] = run.boards[i].solution.clone();
            run.finishRound(11000, 12000);
            if (run.solved(0) != 3 || run.score(0) != 3445) throw new AssertionError("Bonificación incorrecta");
            run.nextRound();
            run.start(12000, 13000);
            run.finishRound(1500000, 1500000);
            if (run.usedTime(1) != 1200000 || run.score(1) != 0) throw new AssertionError("Vencimiento incorrecto");
        }
        checkCooldown(eligible);
        checkPractice(eligible);
        Files.write(Paths.get(args[3]), audit, StandardCharsets.UTF_8);
        System.out.println("Concurso: " + eligible.size() + " tableros únicos auditados (" + added + " propios). Reglas, reloj, puntuación y espera verificados.");
        checkRanked(eligible, Paths.get(args[4]));
    }

    private static void checkPractice(Map<String, Puzzle> eligible) {
        ContestRun run = ContestRun.create(false, 2, 54, new ArrayList<>(eligible.values()), Collections.emptySet(), new Random(18));
        run.start(10, 100);
        int board = 0, cell = 0;
        search:
        for (; board < run.boardCount(0); board++) {
            for (int i = 0; i < run.boards[board].cage.length; i++) {
                if (run.boards[board].operationAt(i) == '/') { cell = i; break search; }
            }
        }
        if (board == run.boardCount(0)) throw new AssertionError("La prueba requiere una división");
        Puzzle puzzle = run.boards[board];
        for (int i = 0; i < puzzle.cage.length; i++) {
            if (puzzle.cage[i] == puzzle.cage[cell]) run.values[board][i] = puzzle.solution[i] % puzzle.size + 1;
        }
        if (!PracticeInsights.recent(Collections.singletonList(run)).isEmpty())
            throw new AssertionError("No se deben analizar rondas abiertas");
        run.finishRound(1010, 1100);
        List<PracticeInsights.Focus> focus = PracticeInsights.recent(Collections.singletonList(run));
        if (focus.size() != 1 || focus.get(0).regions != 1 || focus.get(0).operation != '/' || focus.get(0).size != puzzle.size)
            throw new AssertionError("Las casillas vacías o dos errores de la misma región alteraron la sugerencia");
        List<PracticeInsights.OperationProgress> summary = PracticeInsights.progress(Collections.singletonList(run));
        PracticeInsights.OperationProgress division = summary.get(3);
        if (summary.size() != 4 || division.operation != '/' || division.errors != 1 || division.correct != 0
                || division.total() < division.errors || summary.stream().mapToInt(s -> s.incomplete).sum() == 0)
            throw new AssertionError("El resumen debe distinguir errores e incompletos por operación.");
        String csv = PracticeInsights.report(Collections.singletonList(run));
        if (!csv.startsWith("\ufeff\"Fecha\"") || !csv.contains("Divisiones " + puzzle.size + "×" + puzzle.size + " (1)")
                || csv.split("\r\n").length != 2)
            throw new AssertionError("El CSV debe reflejar las rondas entregadas y la operación a repasar");
        TimedPractice timed = TimedPractice.decode(new TimedPractice(puzzle.id, '/', 5, 1000, 2000).encode());
        if (timed == null || !timed.boardId.equals(puzzle.id) || timed.operation != '/'
                || timed.remaining(61000, 62000) != 240000
                || timed.remaining(1000, 302000) != 0 || timed.remaining(301000, 2000) != 0)
            throw new AssertionError("La práctica debe recuperar el plazo y expirar en ambos relojes");
        System.out.println("Práctica: sugerencias por región, CSV y cronómetro persistente verificados.");
    }

    private static void checkRanked(Map<String, Puzzle> eligible, java.nio.file.Path finalists) throws java.io.IOException {
        List<String> lines = Files.readAllLines(finalists, StandardCharsets.UTF_8);
        Set<String> layouts = new HashSet<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] f = line.split("\t", -1);
            Puzzle board = new Puzzle(f[0], f[1], f[2], Integer.parseInt(f[3]), f[4], f[5], f[6], f[7]).forContest();
            if (board.size != 8 || !board.solved(board.solution) || board.countSolutions() != 1
                    || eligible.put(board.id, board) != null || !layouts.add(f[4] + ":" + f[5] + ":" + f[6]))
                throw new AssertionError("Final inválida o repetida: " + board.id);
        }
        if (layouts.size() != 50) throw new AssertionError("Se necesitan 50 tableros de final");
        List<Puzzle> pool = new ArrayList<>(eligible.values());
        for (int level = 0; level < ContestRun.LEVELS.length; level++) {
            for (int solved : new int[]{6, 7, 9}) {
                ContestRun run = ContestRun.createRanked(level, 54, pool, Collections.emptySet(), new Random(18));
                for (int round = 0; round < ContestRun.ROUNDS; round++) {
                    run.start(10, 100);
                    for (int slot = 0; slot < run.boardCount(round); slot++) {
                        int index = run.index(slot);
                        if (index < solved) run.values[index] = run.boards[index].solution.clone();
                    }
                    run.finishRound(10010, 10100);
                    if (round + 1 < ContestRun.ROUNDS) run.nextRound();
                }
                boolean qualified = solved >= ContestRun.QUALIFY;
                if (run.qualified() != qualified || run.finished() == qualified || run.qualifierSolved() != solved)
                    throw new AssertionError("Clasificación incorrecta");
                run = ContestRun.decode(run.encode(), eligible);
                if (run == null || !run.ranked || run.pro || run.qualified() != qualified)
                    throw new AssertionError("No se recuperó el torneo");
                if (!qualified) {
                    run.nextRound();
                    int change = run.rankChange(305);
                    if (run.round != 2 || run.rankPoints() != solved * 50 || change != -10
                            || run.rankChange(3) != -3 || run.rankChange(0) != 0
                            || ContestRun.rankIndex(305 + change) != 0)
                        throw new AssertionError("Resultado sin final incorrecto");
                    continue;
                }
                if (run.rankChange(500) != 0) throw new AssertionError("Se otorgaron puntos antes de terminar");
                run.nextRound();
                run.start(10, 100);
                int finalIndex = run.index(0);
                run.notes[finalIndex][0] = 8 << 8;
                run = ContestRun.decode(run.encode(), eligible);
                if (run == null || run.round != 3 || run.boardCount(run.round) != 1 || run.boards[finalIndex].size != 8
                        || run.remaining(10, 100) != 1200000 || run.notes[finalIndex][0] != 8 << 8)
                    throw new AssertionError("No se recuperó la final");
                if (solved == 9) run.values[finalIndex] = run.boards[finalIndex].solution.clone();
                long delivered = solved == 9 ? 10010 : 1200010;
                run.finishRound(delivered, delivered + 90);
                if (!run.finished() || run.rankChange(0) != solved * 50 + (solved == 9 ? 100 : 0)
                        || ContestRun.rankIndex(0 + run.rankChange(0)) != 1
                        || run.score(run.round) != (solved == 9 ? 1595 : 0))
                    throw new AssertionError("Puntuación de la final incorrecta");
                ContestCooldown cooldown = new ContestCooldown("");
                cooldown.record(run, run.round);
                if (cooldown.postponed().contains(run.boards[finalIndex].id) != (solved == 7))
                    throw new AssertionError("Reserva de final incorrecta");
            }
        }
        ContestRun loss = ContestRun.createRanked(0, 55, pool, Collections.emptySet(), new Random(18));
        for (int round = 0; round < ContestRun.ROUNDS; round++) {
            loss.start(10, 100);
            loss.finishRound(10010, 10100);
            loss.nextRound();
        }
        if (!loss.finished() || loss.rankChange(400) != -30) throw new AssertionError("Se excedió el castigo máximo");
        int[] thresholds = {0, 300, 750, 1400, 2200};
        for (int rank = 1; rank < thresholds.length; rank++) {
            if (ContestRun.rankIndex(thresholds[rank] - 1) != rank - 1 || ContestRun.rankIndex(thresholds[rank]) != rank)
                throw new AssertionError("Umbral de rango incorrecto");
        }
        System.out.println("Torneo: 50 finales únicas de 8×8 auditadas. Clasificación, guardado, reloj y rangos verificados.");
    }

    private static void checkCooldown(Map<String, Puzzle> eligible) {
        List<Puzzle> catalog = new ArrayList<>(eligible.values());
        Random random = new Random(184);
        ContestCooldown cooldown = new ContestCooldown("");
        ContestRun failed = ContestRun.create(false, 0, 1, catalog, cooldown.postponed(), random);
        failed.start(10, 100);
        failed.values[0] = failed.boards[0].solution.clone();
        failed.values[1] = failed.boards[1].solution.clone();
        failed.finishRound(20, 110);
        cooldown.record(failed, 0);
        String postponed = failed.boards[2].id;
        if (!cooldown.postponed().equals(Collections.singleton(postponed))) throw new AssertionError("Se reservó un tablero resuelto");
        for (int i = 0; i < ContestCooldown.WAIT_ROUNDS; i++) {
            cooldown = new ContestCooldown(cooldown.encode());
            if (!cooldown.postponed().contains(postponed)) throw new AssertionError("Se liberó un tablero antes de tiempo");
            ContestRun later = ContestRun.create(i % 2 == 0, 0, i + 2, catalog, cooldown.postponed(), random);
            if (Arrays.stream(later.boards).anyMatch(p -> p.id.equals(postponed)))
                throw new AssertionError("Se reutilizó un tablero reservado");
            later.start(10, 100);
            for (int j = 0; j < ContestRun.PER_ROUND; j++) later.values[j] = later.boards[j].solution.clone();
            later.finishRound(20, 110);
            cooldown.record(later, 0);
        }
        if (cooldown.postponed().contains(postponed)) throw new AssertionError("El tablero no volvió al catálogo");
        for (int level = 0; level < ContestRun.LEVELS.length; level++) {
            for (int size = 4; size <= 7; size++) {
                Set<String> blocked = new HashSet<>();
                for (Puzzle board : catalog) {
                    if (board.size == size && (level != 0 || board.basicOperations())) blocked.add(board.id);
                    if (blocked.size() == ContestCooldown.WAIT_ROUNDS * ContestRun.PER_ROUND) break;
                }
                ContestRun run = ContestRun.create(true, level, 5, catalog, blocked, random);
                if (Arrays.stream(run.boards).anyMatch(p -> blocked.contains(p.id))) throw new AssertionError("La reserva se omitió");
            }
        }
    }
}

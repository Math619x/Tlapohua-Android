package com.rey.celdas;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class PuzzleCheck {
    public static void main(String[] args) throws java.io.IOException {
        List<String> lines = Files.readAllLines(Paths.get(args[0]), StandardCharsets.UTF_8);
        int[] activeSizes = new int[10];
        Set<String> layouts = new HashSet<>();
        List<Puzzle> catalog = new ArrayList<>();
        Puzzle challenge = null;
        for (int row = 1; row < lines.size(); row++) {
            String[] columns = lines.get(row).split("\t", -1);
            Puzzle puzzle = new Puzzle(columns[0], columns[1], columns[2], Integer.parseInt(columns[3]),
                columns[4], columns[5], columns[6], columns[7]);
            catalog.add(puzzle);
            if (!puzzle.family.equals("archivo")) {
                activeSizes[puzzle.size]++;
                String expected = puzzle.size <= 4 ? "patio" : puzzle.size <= 6 ? "plaza" : "senderos";
                if (!expected.equals(puzzle.family)) throw new AssertionError("Zona incorrecta: " + puzzle.id);
            }
            StringBuilder signature = new StringBuilder().append(puzzle.size).append(':');
            for (int cell = 0; cell < puzzle.cage.length; cell++) {
                int group = puzzle.cage[cell];
                signature.append(puzzle.first[group]).append(':');
                if (puzzle.first[group] == cell) signature.append(puzzle.clue(group));
                signature.append(',');
            }
            if (!layouts.add(signature.toString())) throw new AssertionError("Tablero duplicado: " + puzzle.id);
            if (!puzzle.solved(puzzle.solution) || puzzle.countSolutions() != 1)
                throw new AssertionError("Tablero inválido o sin solución única: " + puzzle.id);
            int[] board = new int[puzzle.cage.length];
            for (int group = 0; group < puzzle.first.length; group++)
                if (!puzzle.cagePossible(board, group)) throw new AssertionError("Región imposible");
            for (int value : board) if (value != 0) throw new AssertionError("La revisión modificó el tablero");
            if (puzzle.id.equals("reto")) challenge = puzzle;
        }
        for (int size = 2; size <= 9; size++) {
            int expected = size == 2 ? 40 : 50;
            if (activeSizes[size] != expected) throw new AssertionError("Se esperaban " + expected + " tableros de " + size + "×" + size);
        }
        if (challenge == null) throw new AssertionError("Falta el tablero de la versión anterior");
        int[] nearSolved = challenge.solution.clone();
        nearSolved[0] = 0;
        int[] beforeHint = nearSolved.clone();
        if (!java.util.Arrays.equals(challenge.candidates(nearSolved, 0), new int[]{challenge.solution[0]})
                || !java.util.Arrays.equals(beforeHint, nearSolved) || !challenge.hintRule(0).contains("Divide"))
            throw new AssertionError("La pista debe dar candidatos válidos sin modificar la partida.");
        int[] board = new int[25];
        String notesDescription = challenge.cellDescription(board, 5, 0, false, false);
        if (!notesDescription.contains("Fila 1, columna 1") || !notesDescription.contains("candidatos anotados 1 3")
                || !notesDescription.contains("división") || !notesDescription.contains("vacía"))
            throw new AssertionError("Las notas y la pista deben poder leerse con el lector de pantalla.");
        if (!challenge.cellDescription(board, 4 << challenge.size, 0, false, false).contains("número provisional 4"))
            throw new AssertionError("El lector debe distinguir el número provisional.");
        board[0] = 2;
        if (challenge.cagePossible(board, 0)) throw new AssertionError("2 no cabe en 4÷");
        board[0] = 4;
        if (!challenge.cellDescription(board, 0, 0, true, true).contains("confirmada, región completa"))
            throw new AssertionError("La casilla confirmada debe anunciar su estado.");
        if (!challenge.cagePossible(board, 0)) throw new AssertionError("4 sí cabe en 4÷");
        if (challenge.cageComplete(board, 0)) throw new AssertionError("Una región parcial aparece completa");
        board[1] = 1;
        if (!challenge.cageComplete(board, 0)) throw new AssertionError("4 y 1 completan 4÷");
        board[0] = 1;
        board[1] = 4;
        if (challenge.cageComplete(board, 0)) throw new AssertionError("Aceptó la división horizontal invertida");
        if (!challenge.hasOperation('/') || challenge.operationAt(0) != '/'
                || !challenge.reviewCell(board, 0).contains("incumplen la pista")
                || !challenge.forContest().reviewCell(board, 0).contains("cumple su pista"))
            throw new AssertionError("La corrección debe respetar la regla del modo");
        board[5] = 1;
        if (!challenge.reviewCell(board, 0).contains("se repite en la columna"))
            throw new AssertionError("La corrección debe explicar la columna repetida");
        board[5] = 0;
        if (!challenge.reviewCell(board, 5).contains("vacía"))
            throw new AssertionError("La corrección debe distinguir una casilla vacía");
        board[0] = 4;
        board[1] = 1;
        board[2] = 3;
        board[3] = 5;
        if (challenge.cageComplete(board, 1)) throw new AssertionError("Aceptó la resta invertida");
        board[2] = 0;
        board[3] = 0;
        board[6] = 1;
        if (challenge.cageComplete(board, 0)) throw new AssertionError("Una columna repetida invalida la marca de región");
        board[6] = 0;
        board[1] = 2;
        if (challenge.cageComplete(board, 0)) throw new AssertionError("4 y 2 no completan 4÷");
        board[4] = 3;
        if (challenge.permits(board, 8, 3)) throw new AssertionError("Aceptó un número repetido dentro de la región");
        int[] solved = challenge.solution.clone();
        if (challenge.digitCounts(solved)[4] != 5) throw new AssertionError("El 4 debe estar agotado");
        solved[0] = 0;
        if (challenge.digitCounts(solved)[4] != 4) throw new AssertionError("Al borrar un 4 debe quedar disponible");
        Puzzle vertical = catalog.stream().filter(p -> p.id.equals("patio-3-mixto")).findFirst().get();
        int[] pair = new int[9];
        pair[0] = 2;
        pair[3] = 1;
        if (!vertical.cageComplete(pair, 0)) throw new AssertionError("2 arriba de 1 completan 2÷");
        pair[0] = 1;
        pair[3] = 2;
        if (vertical.cageComplete(pair, 0)) throw new AssertionError("Aceptó la división vertical invertida");
        checkJourney(catalog);
        checkGeneratedBoards();
        checkLessons(catalog);
        checkAttempt();
        int daily = checkDaily(catalog);
        System.out.println("Catálogo verificado: " + catalog.size() + " tableros con solución única, "
            + daily + " para el reto diario; calendario, ruta, lecciones y repetición verificados.");
    }

    private static void checkGeneratedBoards() {
        ForestBoards generated = new ForestBoards();
        for (int size = 3; size <= 9; size++) {
            for (int serial = 1; serial <= 50; serial++) {
                String id = ForestBoards.PREFIX + size + "-" + serial;
                Puzzle board = generated.find(id);
                if (board == null || board.size != size || !board.solved(board.solution) || board.countSolutions() != 1)
                    throw new AssertionError("Tablero generado inválido: " + id);
            }
        }
        System.out.println("Ruta: 350 tableros generados de 3×3 a 9×9 auditados con solución única.");
    }

    private static int checkDaily(List<Puzzle> catalog) {
        List<Puzzle> pool = DailyDates.pool(catalog);
        if (pool.size() != 150) throw new AssertionError("El reto diario requiere 150 tableros");
        Calendar leap = DailyDates.parse("2028-02-29");
        if (leap == null || DailyDates.parse("2027-02-29") != null
                || DailyDates.leadingDays(DailyDates.month(leap)) != 1)
            throw new AssertionError("El mes bisiesto debe empezar en martes");
        leap.add(Calendar.DAY_OF_MONTH, 1);
        if (!DailyDates.key(leap).equals("2028-03-01")) throw new AssertionError("El calendario no avanzó de mes");
        Calendar day = DailyDates.parse("2026-12-31");
        List<Puzzle> reversed = new ArrayList<>(catalog);
        Collections.reverse(reversed);
        List<Puzzle> reordered = DailyDates.pool(reversed);
        Set<String> assigned = new HashSet<>();
        for (int i = 0; i < pool.size(); i++) {
            String id = DailyDates.boardFor(day, pool).id;
            if (!assigned.add(id) || !DailyDates.boardFor(day, reordered).id.equals(id))
                throw new AssertionError("El ciclo diario debe ser estable y usar el catálogo sin repeticiones");
            day.add(Calendar.DAY_OF_MONTH, 1);
            if (i == 0 && !DailyDates.key(day).equals("2027-01-01")) throw new AssertionError("El calendario no avanzó de año");
        }
        return pool.size();
    }

    private static void checkAttempt() {
        int[] solution = {1, 2, 2, 1};
        int[] values = new int[4];
        BoardAttempt attempt = new BoardAttempt(values, "");
        int[] order = {3, 1, 0, 2};
        for (int cell : order) {
            values[cell] = cell == 0 ? 2 : solution[cell];
            attempt.set(cell, values[cell], false);
        }
        attempt = new BoardAttempt(values, attempt.encode());
        BoardAttempt.Review first = attempt.review(values, solution);
        if (first.solved || first.frames.size() != 3 || first.frames.get(0).cell != 3
                || first.frames.get(0).points != 5 || first.frames.get(1).points != 10 || first.frames.get(2).points != -1
                || values[0] != 0 || values[2] != 0 || !attempt.locked(3) || !attempt.locked(1)
                || attempt.total() != 15) throw new AssertionError("No se conservó el prefijo correcto");
        attempt = new BoardAttempt(values, attempt.encode());
        values[2] = solution[2];
        attempt.set(2, values[2], true);
        values[0] = solution[0];
        attempt.set(0, values[0], false);
        BoardAttempt.Review retry = attempt.review(values, solution);
        if (!retry.solved || retry.frames.get(2).points != 0 || retry.frames.get(3).points != 5 || attempt.total() != 20)
            throw new AssertionError("El reintento debe empezar una racha nueva y conservar los puntos");
    }

    private static void checkJourney(List<Puzzle> catalog) {
        ForestBoards generated = new ForestBoards();
        Journey path = new Journey("");
        Journey casual = new Journey("");
        casual.active = "casual-completo";
        casual.recordWin();
        casual.continueAfterWin();
        casual.active = "casual-intento";
        if (casual.fail(true) || casual.fail(true) || !casual.fail(true) || casual.wins != 1 || casual.stage != 0
                || !casual.active.isEmpty() || casual.mistakes != 0 || casual.streak != 0)
            throw new AssertionError("La ruta casual debe conservar victorias al reiniciar el tablero.");
        if (new Journey(casual.encode()).wins != 1) throw new AssertionError("El punto de control debe persistir.");
        List<Puzzle> start = path.choices(catalog, generated);
        if (start.size() != 3 || start.stream().anyMatch(p -> p.size != 3 || !p.hasMixedOperations())
                || start.stream().noneMatch(p -> p.operationSymbols().equals("+  −  ×  ÷")))
            throw new AssertionError("La entrada requiere tres caminos y un tablero con las cuatro operaciones");
        Set<String> won = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            path.active = chooseNew(path, catalog, generated, won);
            if (!path.recordWin() || path.recordWin() || path.qualified() != (i == 2))
                throw new AssertionError("Se requieren tres victorias limpias únicas");
            path.continueAfterWin();
        }
        if (path.stage != 1 || path.wins != 0 || path.streak != 0 || path.mistakes != 0)
            throw new AssertionError("El siguiente nivel debe empezar con sus propios contadores");
        for (int i = 0; i < 5; i++) {
            path.active = chooseNew(path, catalog, generated, won);
            if ((i == 0 || i == 2) && path.fail()) throw new AssertionError("Dos errores están permitidos");
            path.recordWin();
            if (path.qualified() != (i == 4)) throw new AssertionError("Deben contar cinco victorias con dos errores");
            path.continueAfterWin();
        }
        if (path.stage != 2) throw new AssertionError("El reto de cinco victorias debe permitir avanzar");
        path.active = chooseNew(path, catalog, generated, won);
        path.recordWin();
        path.continueAfterWin();
        path.active = chooseNew(path, catalog, generated, new HashSet<>(won));
        if (path.fail() || path.fail()) throw new AssertionError("El nivel se reinició antes del tercer error");
        path = new Journey(path.encode());
        if (!path.fail() || path.stage != 2 || path.wins != 0 || path.streak != 0 || path.mistakes != 0 || !path.active.isEmpty())
            throw new AssertionError("Debe reiniciarse solo el nivel actual");
        chooseNew(path, catalog, generated, won);
        Journey former = new Journey("4|0|1|patio-3|tutorial|reto|plaza-6|senderos-7");
        if (former.stage != 5 || former.finished || !former.active.isEmpty())
            throw new AssertionError("Una ruta anterior completa debe continuar en 8×8");
        Set<String> larger = new HashSet<>();
        for (int size = 8; size <= 9; size++) {
            for (int i = 0; i < 3; i++) {
                former.active = chooseNew(former, catalog, generated, larger);
                if (former.active.startsWith(ForestBoards.PREFIX)) {
                    Puzzle board = generated.find(former.active);
                    Puzzle restored = new ForestBoards().find(former.active);
                    if (board.size != size || !board.solved(board.solution) || board.countSolutions() != 1
                            || !java.util.Arrays.equals(board.solution, restored.solution)
                            || !java.util.Arrays.equals(board.cage, restored.cage))
                        throw new AssertionError("Tablero nuevo inválido o inestable");
                }
                former.recordWin();
                former.continueAfterWin();
            }
        }
        if (!former.finished || !former.choices(catalog, generated).isEmpty()) throw new AssertionError("Falta cerrar la ruta");
    }

    private static String chooseNew(Journey journey, List<Puzzle> catalog, ForestBoards generated, Set<String> used) {
        List<Puzzle> choices = journey.choices(catalog, generated);
        if (choices.size() < 2 || choices.stream().anyMatch(p -> p.size != Journey.SIZES[journey.stage] || used.contains(p.id)))
            throw new AssertionError("Las opciones deben ser mapas nuevos del nivel actual");
        String chosen = choices.get(0).id;
        used.add(chosen);
        return chosen;
    }

    private static void checkLessons(List<Puzzle> catalog) {
        for (Lesson lesson : Lesson.ALL) {
            Puzzle puzzle = catalog.stream().filter(p -> p.id.equals(lesson.puzzleId)).findFirst().get();
            int[] values = new int[puzzle.cage.length];
            for (int given : lesson.givens) values[given] = puzzle.solution[given];
            int candidates = 0;
            for (int value = 1; value <= puzzle.size; value++) {
                if (!puzzle.permits(values, lesson.cell, value)) continue;
                values[lesson.cell] = value;
                if (puzzle.cagePossible(values, puzzle.cage[lesson.cell])) candidates++;
                values[lesson.cell] = 0;
            }
            if (candidates != 1) throw new AssertionError("Lección ambigua: " + lesson.title);
        }
    }
}

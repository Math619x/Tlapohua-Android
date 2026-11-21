package com.rey.celdas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class Puzzle {
    final String id, family, title;
    final int size;
    final int[] cage, first, solution;
    private final int[] targets;
    private final String operations;
    final boolean contest;

    Puzzle(String id, String family, String title, int size, String regions, String targets, String operations,
            String solution) {
        this.id = id;
        this.family = family;
        this.title = title;
        this.size = size;
        this.targets = numbers(targets);
        this.operations = operations;
        contest = false;
        this.cage = numbers(regions);
        this.solution = new int[cage.length];
        first = new int[this.targets.length];
        Arrays.fill(first, -1);
        if (size < 2 || size > 9 || cage.length != size * size || solution.length() != cage.length
                || first.length != operations.length()) throw new IllegalArgumentException("Tablero inválido: " + id);
        for (int i = 0; i < cage.length; i++) {
            int group = cage[i];
            if (group < 0 || group >= first.length) throw new IllegalArgumentException("Región inválida: " + id);
            this.solution[i] = solution.charAt(i) - '0';
            if (first[group] == -1) first[group] = i;
        }
        for (int position : first) if (position < 0) throw new IllegalArgumentException("Región vacía: " + id);
    }

    private Puzzle(Puzzle original) {
        id = original.id;
        family = original.family;
        title = original.title;
        size = original.size;
        cage = original.cage;
        first = original.first;
        solution = original.solution;
        targets = original.targets;
        operations = original.operations;
        contest = true;
    }

    Puzzle forContest() { return new Puzzle(this); }

    boolean basicOperations() { return operations.indexOf('*') < 0 && operations.indexOf('/') < 0; }

    private static int[] numbers(String list) {
        String[] parts = list.split(",");
        int[] values = new int[parts.length];
        for (int i = 0; i < parts.length; i++) values[i] = Integer.parseInt(parts[i]);
        return values;
    }

    String clue(int group) {
        return targets[group] + operationSymbol(operations.charAt(group));
    }

    char operationAt(int cell) { return operations.charAt(cage[cell]); }

    String cellDescription(int[] values, int note, int cell, boolean confirmed, boolean complete) {
        int provisional = note >> size;
        String value = values[cell] > 0 ? "respuesta " + values[cell]
            : provisional > 0 ? "número provisional " + provisional : "vacía";
        StringBuilder description = new StringBuilder("Fila ").append(cell / size + 1)
            .append(", columna ").append(cell % size + 1).append(", ").append(value);
        if (values[cell] == 0 && provisional == 0 && note != 0) {
            description.append(", candidatos anotados");
            for (int i = 1; i <= size; i++) if ((note & (1 << (i - 1))) != 0) description.append(' ').append(i);
        }
        int group = cage[cell];
        String operator;
        switch (operationAt(cell)) {
            case '+': operator = "suma"; break;
            case '-': operator = "resta"; break;
            case '*': operator = "multiplicación"; break;
            case '/': operator = "división"; break;
            default: operator = "valor fijo";
        }
        description.append(", región ").append(targets[group]).append(", ").append(operator);
        if (confirmed) description.append(", confirmada");
        if (complete) description.append(", región completa");
        return description.toString();
    }

    boolean hasOperation(char operation) { return operations.indexOf(operation) >= 0; }

    String hintRule(int cell) {
        String rule;
        switch (operationAt(cell)) {
            case '+': rule = "Los números de la región deben sumar la pista."; break;
            case '*': rule = "Multiplica los números de la región para obtener la pista."; break;
            case '-': rule = contest ? "La diferencia entre los dos números debe dar la pista."
                : "Resta el número derecho o inferior al izquierdo o superior."; break;
            case '/': rule = contest ? "Divide el mayor entre el menor para obtener la pista."
                : "Divide el número izquierdo o superior entre el derecho o inferior."; break;
            default: rule = "La pista indica el valor de esta casilla.";
        }
        return "Región " + clue(cage[cell]) + ". " + rule + " Revisa también los números de la fila y columna.";
    }

    int[] candidates(int[] values, int cell) {
        int[] trial = values.clone(), possible = new int[size];
        trial[cell] = 0;
        int count = 0;
        for (int value = 1; value <= size; value++) {
            if (!permits(trial, cell, value)) continue;
            trial[cell] = value;
            if (cagePossible(trial, cage[cell])) possible[count++] = value;
            trial[cell] = 0;
        }
        return Arrays.copyOf(possible, count);
    }

    String reviewCell(int[] submitted, int cell) {
        int value = submitted[cell];
        if (value == 0) return "Dejaste esta casilla vacía.";
        boolean row = false, column = false;
        for (int i = 0; i < submitted.length; i++) {
            if (i == cell || submitted[i] != value) continue;
            row |= i / size == cell / size;
            column |= i % size == cell % size;
        }
        String detail = row && column ? "El número se repite en la fila y en la columna."
            : row ? "El número se repite en la fila." : column ? "El número se repite en la columna." : "";
        for (int i = 0; i < submitted.length; i++) {
            if (cage[i] == cage[cell] && submitted[i] == 0)
                return (detail.isEmpty() ? "" : detail + " ") + "La región está incompleta.";
        }
        if (!cageMatches(submitted, cage[cell]))
            return (detail.isEmpty() ? "" : detail + " ") + "Los números de la región incumplen la pista " + clue(cage[cell]) + ".";
        if (!detail.isEmpty()) return detail;
        return value == solution[cell] ? "La casilla coincide con la solución."
            : "Esta región cumple su pista. La diferencia aparece al resolver el tablero completo.";
    }

    String operationSymbols() {
        StringBuilder symbols = new StringBuilder();
        for (char operation : new char[]{'+', '-', '*', '/'}) {
            if (operations.indexOf(operation) < 0) continue;
            if (symbols.length() > 0) symbols.append("  ");
            symbols.append(operationSymbol(operation));
        }
        return symbols.toString();
    }

    boolean hasMixedOperations() {
        return operationSymbols().length() > 1;
    }

    private String operationSymbol(char operation) {
        switch (operation) {
            case '*': return "×";
            case '/': return "÷";
            case '-': return "−";
            case '+': return "+";
            default: return "";
        }
    }

    boolean related(int a, int b) {
        return a / size == b / size || a % size == b % size || (!contest && cage[a] == cage[b]);
    }

    boolean permits(int[] board, int cell, int value) {
        for (int i = 0; i < board.length; i++) {
            if (i != cell && board[i] == value && related(i, cell)) return false;
        }
        return true;
    }

    boolean cagePossible(int[] board, int group) {
        return completeCage(board, group, first[group]);
    }

    boolean cageComplete(int[] board, int group) {
        if (!cageMatches(board, group)) return false;
        for (int cell = first[group]; cell < board.length; cell++) {
            if (cage[cell] == group && !permits(board, cell, board[cell])) return false;
        }
        return true;
    }

    int[] digitCounts(int[] board) {
        int[] counts = new int[size + 1];
        for (int value : board) if (value >= 1 && value <= size) counts[value]++;
        return counts;
    }

    private boolean completeCage(int[] board, int group, int from) {
        for (int cell = from; cell < board.length; cell++) {
            if (cage[cell] != group || board[cell] != 0) continue;
            for (int value = 1; value <= size; value++) {
                if (!permits(board, cell, value)) continue;
                board[cell] = value;
                boolean possible = completeCage(board, group, cell + 1);
                board[cell] = 0;
                if (possible) return true;
            }
            return false;
        }
        return cageMatches(board, group);
    }

    private boolean cageMatches(int[] board, int group) {
        int seen = 0, sum = 0, product = 1, last = 0, count = 0;
        for (int i = first[group]; i < board.length; i++) {
            if (cage[i] != group) continue;
            int value = board[i];
            if (value < 1 || value > size || (!contest && (seen & (1 << value)) != 0)) return false;
            seen |= 1 << value;
            sum += value;
            product *= value;
            last = value;
            count++;
        }
        int target = targets[group];
        switch (operations.charAt(group)) {
            case '=': return count == 1 && sum == target;
            case '+': return sum == target;
            case '*': return product == target;
            case '-':
                int difference = board[first[group]] - last;
                return count == 2 && (contest ? Math.abs(difference) : difference) == target;
            case '/':
                int head = board[first[group]];
                return count == 2 && (head == last * target || (contest && last == head * target));
            default: throw new IllegalStateException("Operación desconocida");
        }
    }

    boolean solved(int[] board) {
        for (int i = 0; i < board.length; i++) {
            if (board[i] < 1 || board[i] > size || !permits(board, i, board[i])) return false;
        }
        for (int group = 0; group < first.length; group++) if (!cageMatches(board, group)) return false;
        return true;
    }

    int countSolutions() {
        List<int[]> cellsByCage = new ArrayList<>();
        List<List<int[]>> optionsByCage = new ArrayList<>();
        int[] board = new int[cage.length];
        for (int group = 0; group < first.length; group++) {
            int[] cells = new int[size];
            int length = 0;
            for (int i = first[group]; i < cage.length; i++) {
                if (cage[i] != group) continue;
                if (length == size) return 0;
                cells[length++] = i;
            }
            cells = Arrays.copyOf(cells, length);
            List<int[]> options = new ArrayList<>();
            collectOptions(board, group, cells, 0, 0, options);
            cellsByCage.add(cells);
            optionsByCage.add(options);
        }
        return count(board, cellsByCage, optionsByCage, 2);
    }

    private void collectOptions(int[] board, int group, int[] cells, int index, int used, List<int[]> options) {
        if (index == cells.length) {
            if (cageMatches(board, group)) {
                int[] option = new int[cells.length];
                for (int i = 0; i < cells.length; i++) option[i] = board[cells[i]];
                options.add(option);
            }
            return;
        }
        for (int digit = 1; digit <= size; digit++) {
            int bit = 1 << digit;
            if (contest ? !permits(board, cells[index], digit) : (used & bit) != 0) continue;
            board[cells[index]] = digit;
            collectOptions(board, group, cells, index + 1, used | bit, options);
        }
        board[cells[index]] = 0;
    }

    private int count(int[] board, List<int[]> cellsByCage, List<List<int[]>> optionsByCage, int limit) {
        int[] bestCells = null;
        List<int[]> bestOptions = null;
        for (int group = 0; group < first.length; group++) {
            if (board[first[group]] != 0) continue;
            int[] cells = cellsByCage.get(group);
            List<int[]> viable = new ArrayList<>();
            for (int[] option : optionsByCage.get(group)) {
                if (fits(board, cells, option)) viable.add(option);
                if (bestOptions != null && viable.size() >= bestOptions.size()) break;
            }
            if (viable.isEmpty()) return 0;
            if (bestOptions == null || viable.size() < bestOptions.size()) {
                bestCells = cells;
                bestOptions = viable;
            }
        }
        if (bestCells == null) return 1;
        int found = 0;
        for (int[] option : bestOptions) {
            for (int i = 0; i < bestCells.length; i++) board[bestCells[i]] = option[i];
            found += count(board, cellsByCage, optionsByCage, limit - found);
            if (found >= limit) break;
        }
        for (int cell : bestCells) board[cell] = 0;
        return found;
    }

    private boolean fits(int[] board, int[] cells, int[] option) {
        for (int i = 0; i < cells.length; i++) if (!permits(board, cells[i], option[i])) return false;
        return true;
    }
}

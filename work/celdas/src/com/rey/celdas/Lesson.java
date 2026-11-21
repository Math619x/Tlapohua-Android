package com.rey.celdas;

final class Lesson {
    static final Lesson[] ALL = {
        new Lesson("Suma", "patio-2", 1, new int[]{3}, "3+ significa que las dos casillas de la región suman 3. "
            + "Ya hay un 1 en la región. Completa la casilla marcada con lo que falta para llegar a 3."),
        new Lesson("Sin repetir", "patio-3", 2, new int[]{0, 1}, "En una fila de 3×3 van los números del 1 al 3. "
            + "La primera fila ya tiene 3 y 2. Coloca el número que falta en la casilla marcada."),
        new Lesson("Multiplicación", "reto", 4, new int[]{8, 9}, "La región 24× ya tiene 4 y 3. Su producto es 12. "
            + "Coloca el número que, multiplicado por 12, da 24."),
        new Lesson("Resta", "reto", 2, new int[]{1, 3}, "En la región 2−, la casilla derecha tiene un 3. "
            + "Coloca a su izquierda el número que, al restarle 3, da 2."),
        new Lesson("División", "reto", 0, new int[]{1}, "La región 4÷ tiene un 1 a la derecha. En la división usamos el mayor primero. "
            + "Coloca el número que, dividido entre 1, da 4.")
    };

    final String title, puzzleId, instruction;
    final int cell;
    final int[] givens;

    private Lesson(String title, String puzzleId, int cell, int[] givens, String instruction) {
        this.title = title;
        this.puzzleId = puzzleId;
        this.cell = cell;
        this.givens = givens;
        this.instruction = instruction;
    }
}

package com.example.buscaminas;

/** Niveles de juego. El multiplicador escala los puntos y tiempoObjetivo define el bonus por rapidez. */
public enum Dificultad {
    FACIL("Fácil", 6, 6, 8, 1, 120),
    MEDIO("Medio", 8, 8, 16, 2, 300),
    DIFICIL("Difícil", 10, 10, 20, 3, 600);

    public final String nombre;
    public final int filas;
    public final int columnas;
    public final int minas;
    public final int multiplicador;
    public final int tiempoObjetivo; // segundos

    Dificultad(String nombre, int filas, int columnas, int minas, int multiplicador, int tiempoObjetivo) {
        this.nombre = nombre;
        this.filas = filas;
        this.columnas = columnas;
        this.minas = minas;
        this.multiplicador = multiplicador;
        this.tiempoObjetivo = tiempoObjetivo;
    }
}

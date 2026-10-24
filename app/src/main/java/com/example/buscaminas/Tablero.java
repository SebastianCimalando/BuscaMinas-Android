package com.example.buscaminas;

import java.util.ArrayDeque;
import java.util.Random;

/** Lógica pura del buscaminas: no sabe nada de pantallas ni botones. */
public class Tablero {

    public enum Estado { JUGANDO, GANADO, PERDIDO }

    public final int filas;
    public final int columnas;
    public final int totalMinas;

    private final boolean[][] mina;
    private final boolean[][] abierta;
    private final boolean[][] bandera;
    private final Random azar = new Random();

    private boolean minasColocadas = false;
    private int celdasAbiertas = 0;
    private int banderas = 0;
    private Estado estado = Estado.JUGANDO;

    public Tablero(int filas, int columnas, int totalMinas) {
        this.filas = filas;
        this.columnas = columnas;
        this.totalMinas = totalMinas;
        mina = new boolean[filas][columnas];
        abierta = new boolean[filas][columnas];
        bandera = new boolean[filas][columnas];
    }

    public Estado getEstado() { return estado; }
    public boolean esMina(int f, int c) { return mina[f][c]; }
    public boolean estaAbierta(int f, int c) { return abierta[f][c]; }
    public boolean tieneBandera(int f, int c) { return bandera[f][c]; }
    public int minasRestantes() { return Math.max(0, totalMinas - banderas); }
    public int getCeldasAbiertas() { return celdasAbiertas; }

    /** true desde el primer clic (cuando ya se repartieron las minas). */
    public boolean haComenzado() { return minasColocadas; }

    /** Cantidad de minas en las 8 casillas vecinas. */
    public int vecinas(int f, int c) {
        int cuenta = 0;
        for (int df = -1; df <= 1; df++) {
            for (int dc = -1; dc <= 1; dc++) {
                int nf = f + df, nc = c + dc;
                if ((df != 0 || dc != 0) && dentro(nf, nc) && mina[nf][nc]) cuenta++;
            }
        }
        return cuenta;
    }

    /** Abre una casilla. Si no tiene minas vecinas, abre en cadena las de alrededor. */
    public void abrir(int f, int c) {
        if (estado != Estado.JUGANDO || abierta[f][c] || bandera[f][c]) return;

        // Las minas se colocan en el primer clic para que nunca pierdas de entrada.
        if (!minasColocadas) colocarMinas(f, c);

        if (mina[f][c]) {
            abierta[f][c] = true;
            estado = Estado.PERDIDO;
            return;
        }

        ArrayDeque<int[]> cola = new ArrayDeque<>();
        abierta[f][c] = true;
        celdasAbiertas++;
        cola.add(new int[]{f, c});

        while (!cola.isEmpty()) {
            int[] actual = cola.poll();
            if (vecinas(actual[0], actual[1]) != 0) continue;

            for (int df = -1; df <= 1; df++) {
                for (int dc = -1; dc <= 1; dc++) {
                    int nf = actual[0] + df, nc = actual[1] + dc;
                    if (dentro(nf, nc) && !abierta[nf][nc] && !bandera[nf][nc] && !mina[nf][nc]) {
                        abierta[nf][nc] = true;
                        celdasAbiertas++;
                        cola.add(new int[]{nf, nc});
                    }
                }
            }
        }

        comprobarVictoria();
    }

    /**
     * Alterna una bandera. Devuelve false cuando se intenta agregar una bandera
     * y ya se alcanzó el límite; quitar una bandera siempre está permitido.
     */
    public boolean alternarBandera(int f, int c) {
        if (estado != Estado.JUGANDO || abierta[f][c]) return false;

        if (bandera[f][c]) {
            bandera[f][c] = false;
            banderas--;
            return true;
        }

        if (banderas >= totalMinas) return false;

        bandera[f][c] = true;
        banderas++;
        return true;
    }

    private void comprobarVictoria() {
        if (celdasAbiertas == filas * columnas - totalMinas) {
            estado = Estado.GANADO;
        }
    }

    private boolean dentro(int f, int c) {
        return f >= 0 && f < filas && c >= 0 && c < columnas;
    }

    /** Reparte las minas al azar, dejando libre la casilla del primer clic y sus vecinas. */
    private void colocarMinas(int fSegura, int cSegura) {
        int colocadas = 0;
        while (colocadas < totalMinas) {
            int f = azar.nextInt(filas);
            int c = azar.nextInt(columnas);
            boolean cercaDelPrimerClic = Math.abs(f - fSegura) <= 1 && Math.abs(c - cSegura) <= 1;
            if (mina[f][c] || cercaDelPrimerClic) continue;
            mina[f][c] = true;
            colocadas++;
        }
        minasColocadas = true;
    }
}

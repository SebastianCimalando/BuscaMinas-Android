package com.example.buscaminas;

import android.content.Context;
import android.content.SharedPreferences;

/** Guarda en el teléfono el mejor tiempo y el mejor puntaje de cada dificultad. */
public class Records {

    private final SharedPreferences prefs;

    public Records(Context contexto) {
        prefs = contexto.getSharedPreferences("buscaminas_records", Context.MODE_PRIVATE);
    }

    /** Mejor tiempo en segundos, o 0 si todavía no hay récord en esa dificultad. */
    public int mejorTiempo(Dificultad d) {
        return prefs.getInt("tiempo_" + d.name(), 0);
    }

    public int mejorPuntos(Dificultad d) {
        return prefs.getInt("puntos_" + d.name(), 0);
    }

    /** Devuelve true si el récord registrado para esa dificultad proviene de una victoria. */
    public boolean esRecordDeVictoria(Dificultad d) {
        return prefs.getBoolean("es_victoria_tiempo_" + d.name(), false);
    }

    /** Guarda el tiempo si mejora el récord según las reglas:
     * - Si es victoria: pasa a ser récord de victoria y solo puede ser superado por otra victoria más rápida (menor tiempo).
     * - Si es derrota: solo se actualiza si aún no hay récord de victoria y el tiempo es superior al anterior (más resistencia).
     */
    public boolean registrarTiempo(Dificultad d, int segundos, boolean esVictoria) {
        int actual = mejorTiempo(d);
        boolean teniaVictoria = esRecordDeVictoria(d);

        if (esVictoria) {
            // Victoria: reemplaza cualquier tiempo de derrota previo, o mejora una victoria previa con menor tiempo
            if (!teniaVictoria || actual <= 0 || segundos < actual) {
                prefs.edit()
                        .putInt("tiempo_" + d.name(), segundos)
                        .putBoolean("es_victoria_tiempo_" + d.name(), true)
                        .apply();
                return true;
            }
        } else {
            // Derrota: solo si aún no hay victoria registrada y si aguantó más tiempo que antes
            if (!teniaVictoria) {
                if (actual <= 0 || segundos > actual) {
                    prefs.edit()
                            .putInt("tiempo_" + d.name(), segundos)
                            .putBoolean("es_victoria_tiempo_" + d.name(), false)
                            .apply();
                    return true;
                }
            }
        }
        return false;
    }

    public boolean registrarTiempo(Dificultad d, int segundos) {
        return registrarTiempo(d, segundos, true);
    }

    /** Guarda el puntaje si mejora el récord. Devuelve true si es récord nuevo. */
    public boolean registrarPuntos(Dificultad d, int puntos) {
        if (puntos > mejorPuntos(d)) {
            prefs.edit().putInt("puntos_" + d.name(), puntos).apply();
            return true;
        }
        return false;
    }
}

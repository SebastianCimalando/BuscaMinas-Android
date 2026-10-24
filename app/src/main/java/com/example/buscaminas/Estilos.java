package com.example.buscaminas;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

/** Paleta rosa y fondos redondeados reutilizables. */
public final class Estilos {

    private Estilos() {}

    public static final int ROSA = 0xFFFF5C9D;
    public static final int ROSA_OSCURO = 0xFFD6336C;
    public static final int ROSA_CLARO = 0xFFFFD6E6;
    public static final int ROSA_PALIDO = 0xFFFFF0F6;
    public static final int CIRUELA = 0xFF5A2A41;

    public static final int CERRADA_ARRIBA = 0xFFFFAFD0;
    public static final int CERRADA_ABAJO = 0xFFFF8DBA;
    public static final int ABIERTA = 0xFFFFF0F6;
    public static final int MINA = 0xFFFFB3C1;

    public static int dp(Context c, float valor) {
        return Math.round(valor * c.getResources().getDisplayMetrics().density);
    }

    public static GradientDrawable redondeado(int color, float radioPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radioPx);
        return g;
    }

    public static GradientDrawable degradado(int arriba, int abajo, float radioPx) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{arriba, abajo});
        g.setCornerRadius(radioPx);
        return g;
    }

    /** Casilla cerrada: degradé rosa con un borde blanco translúcido que le da aspecto de burbuja. */
    public static GradientDrawable celdaCerrada(Context c, float radioPx) {
        GradientDrawable g = degradado(CERRADA_ARRIBA, CERRADA_ABAJO, radioPx);
        g.setStroke(Math.max(1, dp(c, 1.5f)), 0x99FFFFFF);
        return g;
    }

    /** Botoncito de dificultad: relleno rosa si está elegido, blanco con borde suave si no. */
    public static GradientDrawable chip(Context c, boolean seleccionado) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(c, 20));
        if (seleccionado) {
            g.setColor(ROSA);
        } else {
            g.setColor(Color.WHITE);
            g.setStroke(dp(c, 2), ROSA_CLARO);
        }
        return g;
    }
}

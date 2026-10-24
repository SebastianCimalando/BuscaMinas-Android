package com.example.buscaminas;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // Colores de los números 1..8
    private static final int[] COLORES_NUMEROS = {
            0, 0xFF3B82F6, 0xFF16A34A, 0xFFE11D48, 0xFF7C3AED,
            0xFFB45309, 0xFF0E7490, 0xFF111827, 0xFF6B7280
    };
    private static final String BANDERA = "🇦🇷";

    // Tamaño de las casillas: se agrandan lo más posible; si el tablero no entra a lo ancho, se desplaza.
    private static final int LADO_MINIMO_DP = 28;
    private static final int LADO_MAXIMO_DP = 56;
    private static final float REDONDEO = 0.40f; // radio = 40% del lado (casi circular)

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tic = new Runnable() {
        @Override
        public void run() {
            actualizarTiempo();
            if (corriendo) handler.postDelayed(this, 250);
        }
    };

    private Records records;
    private Dificultad dificultad = Dificultad.FACIL;
    private Tablero tablero;
    private TextView[][] celdas;
    private float radioCelda;
    private final TextView[] chips = new TextView[Dificultad.values().length];

    private GridLayout grilla;
    private TextView txtTiempo, txtBanderas, txtPuntos, txtRecord;

    // Cronómetro
    private boolean corriendo = false;
    private boolean pausadoPorSistema = false;
    private long inicioMs = 0;
    private long acumuladoMs = 0;

    // Puntos
    private int bonusTiempo = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        records = new Records(this);

        // Respeta las barras del sistema (barra de estado y de navegación).
        View raiz = findViewById(R.id.raiz);
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        grilla = findViewById(R.id.grilla);
        txtTiempo = findViewById(R.id.txtTiempo);
        txtBanderas = findViewById(R.id.txtBanderas);
        txtPuntos = findViewById(R.id.txtPuntos);
        txtRecord = findViewById(R.id.txtRecord);
        findViewById(R.id.btnNuevo).setOnClickListener(v -> nuevoJuego());

        construirChips();
        construirGrilla();
        nuevoJuego();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (corriendo) {
            detenerReloj();
            pausadoPorSistema = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pausadoPorSistema && tablero.getEstado() == Tablero.Estado.JUGANDO) iniciarReloj();
        pausadoPorSistema = false;
    }

    // ---------------------------------------------------------------- Interfaz

    private void construirChips() {
        LinearLayout fila = findViewById(R.id.filaDificultad);
        Dificultad[] todas = Dificultad.values();

        for (int i = 0; i < todas.length; i++) {
            final Dificultad d = todas[i];
            TextView chip = new TextView(this);
            chip.setText(d.nombre);
            chip.setGravity(Gravity.CENTER);
            chip.setTypeface(Typeface.DEFAULT_BOLD);
            chip.setTextSize(15);
            chip.setPadding(0, Estilos.dp(this, 10), 0, Estilos.dp(this, 10));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(Estilos.dp(this, 4), 0, Estilos.dp(this, 4), 0);
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> cambiarDificultad(d));

            chips[i] = chip;
            fila.addView(chip);
        }
        actualizarChips();
    }

    private void actualizarChips() {
        Dificultad[] todas = Dificultad.values();
        for (int i = 0; i < todas.length; i++) {
            boolean elegido = todas[i] == dificultad;
            chips[i].setBackground(Estilos.chip(this, elegido));
            chips[i].setTextColor(elegido ? Color.WHITE : Estilos.ROSA_OSCURO);
        }
    }

    private void construirGrilla() {
        grilla.removeAllViews();
        grilla.setColumnCount(dificultad.columnas);
        celdas = new TextView[dificultad.filas][dificultad.columnas];

        int margen = Math.max(1, Estilos.dp(this, 1.5f));
        int anchoDisponible = getResources().getDisplayMetrics().widthPixels - Estilos.dp(this, 34);
        int lado = anchoDisponible / dificultad.columnas - 2 * margen;
        lado = Math.max(Estilos.dp(this, LADO_MINIMO_DP), Math.min(lado, Estilos.dp(this, LADO_MAXIMO_DP)));
        radioCelda = lado * REDONDEO;

        View scroll = findViewById(R.id.scrollTablero);
        if (scroll != null) scroll.scrollTo(0, 0);

        for (int f = 0; f < dificultad.filas; f++) {
            for (int c = 0; c < dificultad.columnas; c++) {
                final int fila = f, columna = c;

                TextView celda = new TextView(this);
                celda.setGravity(Gravity.CENTER);
                celda.setTypeface(Typeface.DEFAULT_BOLD);
                celda.setTextSize(TypedValue.COMPLEX_UNIT_PX, lado * 0.52f);
                celda.setIncludeFontPadding(false);

                GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                lp.width = lado;
                lp.height = lado;
                lp.setMargins(margen, margen, margen, margen);
                celda.setLayoutParams(lp);

                celda.setOnClickListener(v -> {
                    if (tablero.getEstado() != Tablero.Estado.JUGANDO) return;
                    tablero.abrir(fila, columna);
                    despuesDeJugada();
                });
                celda.setOnLongClickListener(v -> {
                    if (tablero.getEstado() != Tablero.Estado.JUGANDO) return true;

                    boolean cambioRealizado = tablero.alternarBandera(fila, columna);
                    if (!cambioRealizado && !tablero.tieneBandera(fila, columna)) {
                        Toast.makeText(MainActivity.this,
                                "No quedan banderas disponibles",
                                Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    despuesDeJugada();
                    return true;
                });

                celdas[f][c] = celda;
                grilla.addView(celda);
            }
        }
    }

    /** Dibuja el estado actual del tablero. */
    private void refrescar() {
        Tablero.Estado estado = tablero.getEstado();
        boolean ganado = estado == Tablero.Estado.GANADO;
        boolean perdido = estado == Tablero.Estado.PERDIDO;
        float radio = radioCelda;

        for (int f = 0; f < tablero.filas; f++) {
            for (int c = 0; c < tablero.columnas; c++) {
                TextView celda = celdas[f][c];
                // Importante: las casillas se reutilizan entre partidas, así que el color se
                // restablece siempre (si quedaba transparente, banderas y bombas no se veían).
                celda.setTextColor(Estilos.CIRUELA);

                if (tablero.estaAbierta(f, c)) {
                    if (tablero.esMina(f, c)) {
                        celda.setBackground(Estilos.redondeado(Estilos.MINA, radio));
                        celda.setText("💣");
                    } else {
                        celda.setBackground(Estilos.redondeado(Estilos.ABIERTA, radio));
                        int n = tablero.vecinas(f, c);
                        celda.setText(n == 0 ? "" : String.valueOf(n));
                        celda.setTextColor(COLORES_NUMEROS[n]);
                    }
                } else if (ganado && tablero.esMina(f, c)) {
                    celda.setBackground(Estilos.celdaCerrada(this, radio));
                    celda.setText(BANDERA); // al ganar, las minas quedan marcadas
                } else if (perdido && tablero.esMina(f, c) && !tablero.tieneBandera(f, c)) {
                    celda.setBackground(Estilos.redondeado(Estilos.ABIERTA, radio));
                    celda.setText("💣");
                } else if (perdido && tablero.tieneBandera(f, c) && !tablero.esMina(f, c)) {
                    celda.setBackground(Estilos.redondeado(Estilos.ABIERTA, radio));
                    celda.setText("❌");
                } else {
                    celda.setBackground(Estilos.celdaCerrada(this, radio));
                    celda.setText(tablero.tieneBandera(f, c) ? BANDERA : "");
                }
            }
        }

        txtBanderas.setText(String.valueOf(ganado ? 0 : tablero.minasRestantes()));
        txtPuntos.setText(formatoPuntos(puntosTotales()));
    }

    private void refrescarRecord() {
        int tiempo = records.mejorTiempo(dificultad);
        int puntos = records.mejorPuntos(dificultad);
        txtRecord.setText("🏆 Récord " + dificultad.nombre + ": "
                + (tiempo < 0 ? "—" : formatoTiempo(tiempo))
                + "   ·   ⭐ " + (puntos == 0 ? "—" : formatoPuntos(puntos)));
    }

    // ------------------------------------------------------------------ Juego

    private void cambiarDificultad(Dificultad nueva) {
        if (nueva == dificultad) return;
        dificultad = nueva;
        actualizarChips();
        construirGrilla();
        nuevoJuego();
    }

    private void nuevoJuego() {
        detenerReloj();
        acumuladoMs = 0;
        pausadoPorSistema = false;
        bonusTiempo = 0;
        tablero = new Tablero(dificultad.filas, dificultad.columnas, dificultad.minas);
        actualizarTiempo();
        refrescar();
        refrescarRecord();
    }

    private void despuesDeJugada() {
        if (tablero.haComenzado()) iniciarReloj();
        refrescar();
        if (tablero.getEstado() != Tablero.Estado.JUGANDO) terminarPartida();
    }

    private int puntosBase() {
        return tablero.getCeldasAbiertas() * 10 * dificultad.multiplicador;
    }

    private int puntosTotales() {
        return puntosBase() + bonusTiempo;
    }

    private void terminarPartida() {
        detenerReloj();
        final int segundos = segundos();
        final boolean gano = tablero.getEstado() == Tablero.Estado.GANADO;

        boolean recordTiempo = false;
        boolean recordPuntos = false;
        if (gano) {
            // Bonus por rapidez: cuanto menos tardes respecto del tiempo objetivo, más suma.
            bonusTiempo = Math.max(0, dificultad.tiempoObjetivo - segundos) * 5 * dificultad.multiplicador;
            recordTiempo = records.registrarTiempo(dificultad, segundos);
            recordPuntos = records.registrarPuntos(dificultad, puntosTotales());
        } else if (puntosTotales() > 0) {
            // Si perdés también cuenta el mejor puntaje alcanzado (el de tiempo solo se guarda al ganar).
            recordPuntos = records.registrarPuntos(dificultad, puntosTotales());
        }

        final int puntos = puntosTotales();
        final boolean esRecordTiempo = recordTiempo;
        final boolean esRecordPuntos = recordPuntos;

        txtPuntos.setText(formatoPuntos(puntos));
        refrescarRecord();

        // Pequeña pausa para que se vea el tablero antes del cartel.
        handler.postDelayed(() -> mostrarResultado(gano, segundos, puntos, esRecordTiempo, esRecordPuntos),
                gano ? 400 : 700);
    }

    private void mostrarResultado(boolean gano, int segundos, int puntos, boolean recordTiempo, boolean recordPuntos) {
        if (isFinishing() || isDestroyed()) return;

        View vista = getLayoutInflater().inflate(R.layout.dialog_resultado, null);
        ImageView imgGif = vista.findViewById(R.id.imgGif);
        TextView txtTitulo = vista.findViewById(R.id.txtTitulo);
        TextView txtDetalle = vista.findViewById(R.id.txtDetalle);
        TextView txtRecordDialogo = vista.findViewById(R.id.txtRecordDialogo);
        TextView btnOtra = vista.findViewById(R.id.btnOtra);
        TextView btnCerrar = vista.findViewById(R.id.btnCerrar);

        final Dialog dialogo = new Dialog(this);
        dialogo.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogo.setContentView(vista);

        if (gano) {
            txtTitulo.setText("¡Ganaste! 🎉");
            cargarGif(imgGif);
        } else {
            txtTitulo.setText("¡Boom! 💥");
            imgGif.setVisibility(View.GONE);
        }

        txtDetalle.setText("Dificultad: " + dificultad.nombre
                + "\n⏱ Tiempo: " + formatoTiempo(segundos)
                + "\n⭐ Puntos: " + formatoPuntos(puntos));

        if (!gano) {
            txtRecordDialogo.setText(recordPuntos
                    ? "✨ ¡Nuevo récord de puntos!\n¡Probá de nuevo, vos podés! 💪"
                    : "¡Probá de nuevo, vos podés! 💪");
        } else {
            StringBuilder sb = new StringBuilder();
            if (recordTiempo) sb.append("🏆 ¡Nuevo récord de tiempo!");
            else sb.append("🏆 Tu récord: ").append(formatoTiempo(records.mejorTiempo(dificultad)));
            if (recordPuntos) sb.append("\n✨ ¡Nuevo récord de puntos!");
            else sb.append("\n✨ Récord de puntos: ").append(formatoPuntos(records.mejorPuntos(dificultad)));
            txtRecordDialogo.setText(sb.toString());
        }

        btnOtra.setOnClickListener(v -> {
            dialogo.dismiss();
            nuevoJuego();
        });
        btnCerrar.setOnClickListener(v -> dialogo.dismiss());

        Window ventana = dialogo.getWindow();
        if (ventana != null) {
            ventana.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            ventana.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.88f),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialogo.show();
    }

    /** Carga res/raw/nyan_cat.gif. Si no lo agregaste todavía, simplemente no muestra imagen. */
    private void cargarGif(ImageView destino) {
        int id = getResources().getIdentifier("nyan_cat", "raw", getPackageName());
        if (id == 0) {
            destino.setVisibility(View.GONE);
            return;
        }
        destino.setVisibility(View.VISIBLE);
        Glide.with(this).asGif().load(id).into(destino);
    }

    // --------------------------------------------------------------- Cronómetro

    private void iniciarReloj() {
        if (corriendo) return;
        corriendo = true;
        inicioMs = SystemClock.elapsedRealtime();
        handler.post(tic);
    }

    private void detenerReloj() {
        if (!corriendo) return;
        acumuladoMs += SystemClock.elapsedRealtime() - inicioMs;
        corriendo = false;
        handler.removeCallbacks(tic);
    }

    private int segundos() {
        long total = acumuladoMs + (corriendo ? SystemClock.elapsedRealtime() - inicioMs : 0);
        return (int) (total / 1000);
    }

    private void actualizarTiempo() {
        txtTiempo.setText(formatoTiempo(segundos()));
    }

    // ------------------------------------------------------------------ Formatos

    private static String formatoTiempo(int segundos) {
        return String.format(Locale.getDefault(), "%02d:%02d", segundos / 60, segundos % 60);
    }

    private static String formatoPuntos(int puntos) {
        return String.format(Locale.getDefault(), "%,d", puntos);
    }
}


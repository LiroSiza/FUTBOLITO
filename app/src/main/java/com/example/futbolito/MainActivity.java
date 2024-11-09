package com.example.futbolito;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    // Variable para la vista del círculo en movimiento, que también actúa como listener del sensor
    private Sensor movingCircleView;
    private TextView contadorTextView;
    private int contador = 0;
    private boolean enEjecucion = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
        // Código opcional para habilitar Edge-to-Edge, que permite que la interfaz ocupe toda la pantalla, incluyendo la barra de estado y navegación.
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            // Ajustar los márgenes de la vista principal para evitar que los elementos se superpongan a la barra de estado o navegación.
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        }); */

        // Activa el modo inmersivo persistente al iniciar la actividad
        enableImmersiveMode();

        // Crear e iniciar la instancia de la vista del círculo en movimiento
        movingCircleView = new Sensor(this);

        // Establecer la vista del círculo en movimiento como la vista principal de la actividad
        setContentView(movingCircleView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Registrar el listener del sensor cuando la actividad se reanuda
        if (movingCircleView != null) {
            movingCircleView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pausar el listener del sensor para evitar el uso innecesario de batería cuando la actividad se pausa
        if (movingCircleView != null) {
            movingCircleView.pause();
        }
    }

    private void enableImmersiveMode() {
        // Oculta la barra de estado y la barra de navegación
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // Reactiva el modo inmersivo cuando la actividad recupera el enfoque
        if (hasFocus) {
            enableImmersiveMode();
        }
    }
}
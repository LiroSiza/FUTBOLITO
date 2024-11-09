package com.example.futbolito;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    // Variable para la vista del círculo en movimiento, que también actúa como listener del sensor
    private Sensor movingCircleView;

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

}
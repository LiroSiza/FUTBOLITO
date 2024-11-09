package com.example.futbolito;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private Sensor movingCircleView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });*/
        // Crear e iniciar la vista del círculo en movimiento
        movingCircleView = new Sensor(this);
        setContentView(movingCircleView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Registrar el listener del sensor
        if (movingCircleView != null) {
            movingCircleView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Anular el registro del sensor para ahorrar batería
        if (movingCircleView != null) {
            movingCircleView.pause();
        }
    }
}
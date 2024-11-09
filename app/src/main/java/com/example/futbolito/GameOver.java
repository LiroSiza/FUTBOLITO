package com.example.futbolito;


import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class GameOver extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.score_gameover);

        // Obtiene el puntaje del jugador desde el Intent
        int playerScore = getIntent().getIntExtra("player_score", 0);
        int timeLimit = getIntent().getIntExtra("time_limit", 0);

        // Referencia al TextView para mostrar el puntaje
        TextView scoreTextView = findViewById(R.id.score_text_view);
        scoreTextView.setText(playerScore + " Goles Anotados en " + timeLimit + " s");

        Button btnPlay = findViewById(R.id.btnPlay);
        btnPlay.setOnClickListener(view -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        ImageButton btnExit = findViewById(R.id.btnExit);
        btnExit.setOnClickListener(view -> {
            finish();
        });
    }
}

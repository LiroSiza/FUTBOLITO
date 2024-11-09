package com.example.futbolito;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.View;

import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.List;

public class Sensor extends View implements SensorEventListener {

    private Paint paint;
    private Paint counterPaint;
    private float posX, posY;
    private float speedX = 0, speedY = 0;
    private SensorManager sensorManager;
    private android.hardware.Sensor accelerometer;
    private static final float CIRCLE_RADIUS = 25;
    private static final float BORDER_SIZE = 50;
    private static final float FRICTION = 0.9f;
    private static final int TIME_LIMIT = 3;

    // Lista de obstáculos dentro del campo
    private List<Obstacle> obstacles = new ArrayList<>();

    // Límites del campo de juego
    private float leftBoundary, rightBoundary, topBoundary, bottomBoundary;

    // Puntuación
    private int counterPlayer = 0;
    private boolean isRightTurn = true;

    // Contador
    private int counter = 0;
    private boolean isCounting = false;
    private Thread counterThread;

    public Sensor(Context context) {
        super(context);

        // Inicializa la pintura para el círculo
        paint = new Paint();
        paint.setColor(Color.BLUE);

        // Inicializa el Paint para el contador con fuente personalizada
        counterPaint = new Paint();
        counterPaint.setColor(Color.WHITE);
        counterPaint.setTextSize(50);
        // Cargar la fuente personalizada desde res/font
        Typeface typeface = ResourcesCompat.getFont(context, R.font.slackey_regular);
        counterPaint.setTypeface(typeface);  // Aplica la fuente

        // Configura el SensorManager y el acelerómetro
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);

        // Iniciar hilo para el contador
        startCounter();
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Dibuja las áreas fuera de los límites del campo con relleno de color
        Paint fillPaint = new Paint();
        fillPaint.setColor(Color.GRAY);
        fillPaint.setStyle(Paint.Style.FILL);

        // Dibujo de bordes del campo de juego
        canvas.drawRect(0, 0, leftBoundary, getHeight(), fillPaint);
        canvas.drawRect(rightBoundary, 0, getWidth(), getHeight(), fillPaint);
        canvas.drawRect(0, 0, getWidth(), topBoundary, fillPaint);
        canvas.drawRect(0, bottomBoundary, getWidth(), getHeight(), fillPaint);

        // Dibuja la circunferencia
        canvas.drawCircle(posX, posY, CIRCLE_RADIUS, paint);

        // Dibuja el contador en la pantalla
        canvas.drawText("Tiempo: " + counter, (getWidth()/2)-350, 75, counterPaint);
        canvas.drawText("Goles: " + counterPlayer, (getWidth()/2) + 60, 75, counterPaint);
        // Dibuja la puntuación de cada jugador
        if(isRightTurn){
            canvas.drawText("Anota Aquí ", getWidth()-(getWidth()/5), 75, counterPaint);
        }else{
            canvas.drawText("Anota Aquí ", BORDER_SIZE*2, 75, counterPaint);
        }

        // Dibuja los obstáculos
        Paint obstaclePaint = new Paint();
        obstaclePaint.setColor(Color.RED);
        for (Obstacle obstacle : obstacles) {
            switch (obstacle.getShape()) {
                case Obstacle.SHAPE_CIRCLE:
                    canvas.drawCircle(obstacle.getX(), obstacle.getY(), obstacle.getSize(), obstaclePaint);
                    break;
                case Obstacle.SHAPE_HORIZONTAL_BAR:
                    canvas.drawRect(
                            obstacle.getX() - obstacle.getSize() / 2,
                            obstacle.getY() - 10,
                            obstacle.getX() + obstacle.getSize() / 2,
                            obstacle.getY() + 10,
                            obstaclePaint
                    );
                    break;
                case Obstacle.SHAPE_VERTICAL_BAR:
                    canvas.drawRect(
                            obstacle.getX() - 10,
                            obstacle.getY() - obstacle.getSize() / 2,
                            obstacle.getX() + 10,
                            obstacle.getY() + obstacle.getSize() / 2,
                            obstaclePaint
                    );
                    break;
            }
        }

        // Actualiza la posición con los límites de la pantalla
        posX = Math.max(CIRCLE_RADIUS, Math.min(posX, getWidth() - CIRCLE_RADIUS));
        posY = Math.max(CIRCLE_RADIUS, Math.min(posY, getHeight() - CIRCLE_RADIUS));

        // Dibuja los límites de la portería izquierda
        Paint goalPaint = new Paint();
        goalPaint.setColor(Color.GREEN);
        goalPaint.setStyle(Paint.Style.STROKE);
        goalPaint.setStrokeWidth(5);

        invalidate();  // Redibuja la vista
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Intercambiamos los valores de aceleración para que el movimiento funcione correctamente en modo horizontal
        float accelX = -event.values[1];  // Invertir el valor de X para que el movimiento sea correcto
        float accelY = event.values[0];   // El valor de Y se mantiene igual

        // Ajusta la velocidad con la fuerza del movimiento
        speedX -= accelX;
        speedY += accelY;

        // Aplica fricción
        speedX *= FRICTION;
        speedY *= FRICTION;

        // Establece un umbral mínimo para la velocidad para evitar temblores
        if (Math.abs(speedX) < 0.1f) speedX = 0;
        if (Math.abs(speedY) < 0.1f) speedY = 0;

        // Actualiza la posición del balón
        posX += speedX;
        posY += speedY;

        // Verificar colisiones con obstáculos
        checkCollisions();

        // Detección de colisiones con las líneas de delimitación

        // Límite izquierdo
        if (posX <= leftBoundary + CIRCLE_RADIUS) {
            posX = leftBoundary + CIRCLE_RADIUS;
            speedX = 0;
        }
        // Límite derecho
        else if (posX >= rightBoundary - CIRCLE_RADIUS) {
            posX = rightBoundary - CIRCLE_RADIUS;
            speedX = 0;
        }

        // Límite superior
        if (posY <= topBoundary + CIRCLE_RADIUS) {
            posY = topBoundary + CIRCLE_RADIUS;
            speedY = 0;
        }
        // Límite inferior
        else if (posY >= bottomBoundary - CIRCLE_RADIUS) {
            posY = bottomBoundary - CIRCLE_RADIUS;
            speedY = 0;
        }

        if (isGoalLeft() && !isRightTurn) { // Gol de jugador 2 en Porteria izquierda
            //resetBallPosition();  // Opcional: Reinicia la posición de la esfera al centro
            counterPlayer++;     // Incrementar el contador o registrar el gol
            postInvalidate();     // Actualizar la pantalla
            isRightTurn = true; // Turno del primer jugador
            // Puedes añadir efectos de sonido o alguna animación aquí
        }

        if (isGoalRight() && isRightTurn) { // Gol de jugador 1 en Porteria derecha
            //resetBallPosition();  // Opcional: Reinicia la posición de la esfera al centro
            counterPlayer++;     // Incrementar el contador o registrar el gol
            postInvalidate();     // Actualizar la pantalla
            isRightTurn = false; // Turno del segundo jugador
            // Puedes añadir efectos de sonido o alguna animación aquí
        }

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {  // Cuando se cambia el tamaño de la vista
        super.onSizeChanged(w, h, oldw, oldh);
        // Inicializa la posición al centro de la pantalla
        posX = w / 2;
        posY = (h / 2) + (BORDER_SIZE / 2);

        // Definir las líneas de delimitación del campo
        leftBoundary = BORDER_SIZE;
        rightBoundary = w - BORDER_SIZE;
        topBoundary = 2 * BORDER_SIZE;
        bottomBoundary = h - BORDER_SIZE;

        // Generar obstáculos aleatoriamente
        generateObstacles();
    }

    @Override
    public void onAccuracyChanged(android.hardware.Sensor sensor, int i) {
    }

    public void unregisterSensor() {
        sensorManager.unregisterListener(this);
    }

    public void resume() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    public void pause() {
        sensorManager.unregisterListener(this);
        stopCounter();
    }

    // Método para iniciar el hilo del contador
    private void startCounter() {
        isCounting = true;
        counterThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isCounting) {
                    try {
                        Thread.sleep(1000);  // Espera 1 segundo
                        counter++;  // Incrementa el contador
                        postInvalidate();  // Actualiza la vista
                        if (counter == TIME_LIMIT) { // Terminar juego
                            isCounting = false;

                            // Aquí se lanza la nueva actividad final con los datos extra
                            Intent intent = new Intent(getContext(), GameOver.class);
                            intent.putExtra("player_score", counterPlayer);  // Puntos del jugador
                            intent.putExtra("time_limit", TIME_LIMIT);  // Puntos del jugador
                            getContext().startActivity(intent);  // Inicia la actividad

                            // Termina la actividad actual
                            ((Activity) getContext()).finish();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        counterThread.start();
    }

    // Método para detener el hilo del contador
    private void stopCounter() {
        isCounting = false;
        if (counterThread != null) {
            counterThread.interrupt();
        }
    }

    private void generateObstacles() {
        obstacles.clear();

        // Jugadores del equipo izquierdo (defensores y delanteros)
        float playerSize = 30; // Tamaño de los jugadores
        float gapX = (rightBoundary - leftBoundary) / 6; // Espacio entre los jugadores en el eje X
        float gapY = (bottomBoundary - topBoundary) / 4; // Espacio entre los jugadores en el eje Y

        // Defensores del equipo izquierdo
        for (int i = 1; i <= 3; i++) {
            obstacles.add(new Obstacle(Obstacle.SHAPE_CIRCLE, leftBoundary + gapX, topBoundary + i * gapY, playerSize));
        }

        // Delanteros del equipo izquierdo
        for (int i = 1; i <= 3; i++) {
            obstacles.add(new Obstacle(Obstacle.SHAPE_CIRCLE, leftBoundary + 2 * gapX, topBoundary + i * gapY, playerSize));
        }

        // Jugadores del equipo derecho (defensores y delanteros)
        // Defensores del equipo derecho
        for (int i = 1; i <= 3; i++) {
            obstacles.add(new Obstacle(Obstacle.SHAPE_CIRCLE, rightBoundary - gapX, topBoundary + i * gapY, playerSize));
        }

        // Delanteros del equipo derecho
        for (int i = 1; i <= 3; i++) {
            obstacles.add(new Obstacle(Obstacle.SHAPE_CIRCLE, rightBoundary - 2 * gapX, topBoundary + i * gapY, playerSize));
        }

        // Barras horizontales para delimitar áreas
        float barSize = 110;
        obstacles.add(new Obstacle(Obstacle.SHAPE_HORIZONTAL_BAR, leftBoundary + (barSize*2), topBoundary + 2 * gapY, barSize));
        obstacles.add(new Obstacle(Obstacle.SHAPE_HORIZONTAL_BAR, rightBoundary - (barSize*2), bottomBoundary - 2 * gapY, barSize));

        // Barras verticales para crear aperturas en el centro
        float verticalBarSize = (bottomBoundary - topBoundary) / 5;
        obstacles.add(new Obstacle(Obstacle.SHAPE_VERTICAL_BAR, (rightBoundary + leftBoundary) / 2, topBoundary + verticalBarSize, verticalBarSize));
        obstacles.add(new Obstacle(Obstacle.SHAPE_VERTICAL_BAR, (rightBoundary + leftBoundary) / 2, bottomBoundary - verticalBarSize, verticalBarSize));

        // PORTERIAS
        obstacles.add(new Obstacle(Obstacle.SHAPE_VERTICAL_BAR, BORDER_SIZE + 100, topBoundary + (getHeight()/2) - 10, (verticalBarSize/3)));
        obstacles.add(new Obstacle(Obstacle.SHAPE_VERTICAL_BAR, BORDER_SIZE + 100, bottomBoundary - (getHeight()/2) + 10, (verticalBarSize/3)));
        obstacles.add(new Obstacle(Obstacle.SHAPE_HORIZONTAL_BAR, BORDER_SIZE * 2, topBoundary + (getHeight()/2) + 11, 100));
        obstacles.add(new Obstacle(Obstacle.SHAPE_HORIZONTAL_BAR, BORDER_SIZE * 2, bottomBoundary - (getHeight()/2) - 11, 100));

        obstacles.add(new Obstacle(Obstacle.SHAPE_VERTICAL_BAR,  getWidth()- BORDER_SIZE - 100, topBoundary + (getHeight()/2) - 10, (verticalBarSize/3)));
        obstacles.add(new Obstacle(Obstacle.SHAPE_VERTICAL_BAR, getWidth()- BORDER_SIZE - 100, bottomBoundary - (getHeight()/2) + 10, (verticalBarSize/3)));
        obstacles.add(new Obstacle(Obstacle.SHAPE_HORIZONTAL_BAR,  getWidth() - (BORDER_SIZE * 2), topBoundary + (getHeight()/2) + 11, 100));
        obstacles.add(new Obstacle(Obstacle.SHAPE_HORIZONTAL_BAR, getWidth() - (BORDER_SIZE * 2), bottomBoundary - (getHeight()/2) - 11, 100));
    }


    private void checkCollisions() {
        for (Obstacle obstacle : obstacles) {
            switch (obstacle.getShape()) {
                case Obstacle.SHAPE_CIRCLE:
                    handleCircleCollision(obstacle);
                    break;
                case Obstacle.SHAPE_HORIZONTAL_BAR:
                    handleHorizontalBarCollision(obstacle);
                    break;
                case Obstacle.SHAPE_VERTICAL_BAR:
                    handleVerticalBarCollision(obstacle);
                    break;
            }
        }
    }

    private void handleCircleCollision(Obstacle obstacle) {
        float dx = posX - obstacle.getX();
        float dy = posY - obstacle.getY();
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        float minDistance = CIRCLE_RADIUS + obstacle.getSize();

        // Si hay colisión
        if (distance < minDistance) {
            // Reposicionar la pelota fuera del obstáculo
            float overlap = minDistance - distance;
            posX += (dx / distance) * overlap;
            posY += (dy / distance) * overlap;

            // Invertir la velocidad
            speedX = -speedX * 0.5f;  // Reducir la velocidad al chocar
            speedY = -speedY * 0.5f;
        }
    }

    private void handleHorizontalBarCollision(Obstacle obstacle) {
        float left = obstacle.getX() - obstacle.getSize() / 2;
        float right = obstacle.getX() + obstacle.getSize() / 2;
        float top = obstacle.getY() - 10;
        float bottom = obstacle.getY() + 10;

        // Comprobar si la pelota está colisionando con la barra
        if (posX + CIRCLE_RADIUS > left && posX - CIRCLE_RADIUS < right &&
                posY + CIRCLE_RADIUS > top && posY - CIRCLE_RADIUS < bottom) {

            // Ajustar la posición fuera de la barra
            if (posY < obstacle.getY()) {
                posY = top - CIRCLE_RADIUS;
            } else {
                posY = bottom + CIRCLE_RADIUS;
            }

            // Invertir la velocidad vertical
            speedY = -speedY * 0.5f;
        }
    }

    private void handleVerticalBarCollision(Obstacle obstacle) {
        float left = obstacle.getX() - 10;
        float right = obstacle.getX() + 10;
        float top = obstacle.getY() - obstacle.getSize() / 2;
        float bottom = obstacle.getY() + obstacle.getSize() / 2;

        // Comprobar si la pelota está colisionando con la barra
        if (posX + CIRCLE_RADIUS > left && posX - CIRCLE_RADIUS < right &&
                posY + CIRCLE_RADIUS > top && posY - CIRCLE_RADIUS < bottom) {

            // Ajustar la posición fuera de la barra
            if (posX < obstacle.getX()) {
                posX = left - CIRCLE_RADIUS;
            } else {
                posX = right + CIRCLE_RADIUS;
            }

            // Invertir la velocidad horizontal
            speedX = -speedX * 0.5f;
        }
    }

    private boolean isGoalLeft() {
        // Verificar si la esfera está dentro del área de la portería izquierda
        return (posX <= BORDER_SIZE + 50) && (posY >= bottomBoundary - (getHeight()/2) - 11) && (posY <= topBoundary + (getHeight()/2) + 11);
    }

    private boolean isGoalRight() {
        // Verificar si la esfera está dentro del área de la portería derecha
        return (posX >= getWidth() - BORDER_SIZE - 50) && ( posY >= bottomBoundary - (getHeight()/2) - 11) && (posY <= topBoundary + (getHeight()/2) + 11);
    }


    // En caso de querer posicionar la esfera como el inicio
    private void resetBallPosition() {
        posX = getWidth() / 2;
        posY = getHeight() / 2;
        speedX = 0;
        speedY = 0;
    }


}

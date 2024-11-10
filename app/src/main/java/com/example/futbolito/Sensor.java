package com.example.futbolito;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
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
    private static final int TIME_LIMIT = 60;

    // Lista de obstáculos dentro del campo
    private List<Obstacle> obstacles = new ArrayList<>();

    // Límites del campo de juego
    private float leftBoundary, rightBoundary, topBoundary, bottomBoundary;

    // Puntuación
    private int counterPlayer = 0;
    private boolean isRightTurn = true;

    // Contador
    private int counter = 60;
    private boolean isCounting = false;
    private Thread counterThread;

    public Sensor(Context context) {
        super(context);

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
//****************************** CANCHA ************************
        // Dibuja el fondo verde (cancha de fútbol)
        Paint fieldPaint = new Paint();
        fieldPaint.setColor(Color.parseColor("#0FCF40"));  // Establece el color verde para el fondo
        canvas.drawRect(0, 0, getWidth(), getHeight(), fieldPaint);  // Dibuja el rectángulo que cubre toda la pantalla

        // Dibuja las líneas blancas (líneas de la cancha)
        Paint linePaint = new Paint();
        linePaint.setColor(Color.WHITE);
        linePaint.setStrokeWidth(5);  // Establece el grosor de las líneas

        // Areas de porteria
        canvas.drawLine(getWidth()/8, getHeight()/3, getWidth()/8, ((getHeight()/3)*2)+BORDER_SIZE, linePaint);
        canvas.drawLine(BORDER_SIZE, getHeight()/3, getWidth()/8, getHeight()/3, linePaint);
        canvas.drawLine(BORDER_SIZE, ((getHeight()/3)*2)+BORDER_SIZE, getWidth()/8, ((getHeight()/3)*2)+BORDER_SIZE, linePaint);

        canvas.drawLine((getWidth()/8)*7+6, getHeight()/3, (getWidth()/8)*7+6, ((getHeight()/3)*2)+BORDER_SIZE, linePaint);
        canvas.drawLine(getWidth() - BORDER_SIZE, getHeight()/3, getWidth()-(getWidth()/8), getHeight()/3, linePaint);
        canvas.drawLine(getWidth() - BORDER_SIZE, ((getHeight()/3)*2)+BORDER_SIZE, getWidth()-(getWidth()/8), ((getHeight()/3)*2)+BORDER_SIZE, linePaint);

        // Esquinas
        // Definir el rectángulo que limita el arco
        Paint paintArc = new Paint();
        paintArc.setColor(Color.WHITE);
        paintArc.setStrokeWidth(5);
        paintArc.setStyle(Paint.Style.STROKE);

        // Primer arco
        RectF rectF = new RectF(0, BORDER_SIZE, BORDER_SIZE * 2, getHeight() - (getHeight() - BORDER_SIZE * 3));
        canvas.drawArc(rectF, 0, 90, false, paintArc);
        // Segundo arco (esquina inferior izquierda)
        RectF rectF2 = new RectF(0, getHeight() - BORDER_SIZE * 2, BORDER_SIZE * 2, getHeight());
        canvas.drawArc(rectF2, 270, 90, false, paintArc);
        // Tercer arco (esquina superior derecha)
        RectF rectF3 = new RectF(getWidth() - BORDER_SIZE * 2, BORDER_SIZE, getWidth(), BORDER_SIZE * 3);
        canvas.drawArc(rectF3, 90, 90, false, paintArc);
        // Cuarto arco (esquina inferior derecha)
        RectF rectF4 = new RectF(getWidth() - BORDER_SIZE * 2, getHeight() - BORDER_SIZE * 2, getWidth(), getHeight());
        canvas.drawArc(rectF4, 180, 90, false, paintArc);

        RectF rectF5 = new RectF(getWidth()/12-2, getHeight()/3+100, (getWidth()/12) + 200, getHeight()/3+300);
        canvas.drawArc(rectF5, 270, 180, false, paintArc);
        RectF rectF6 = new RectF(getWidth()-(getWidth()/12)-200, getHeight()/3+100, getWidth()-(getWidth()/12), getHeight()/3+300);
        canvas.drawArc(rectF6, 90, 180, false, paintArc);

        // Pintura para el círculo central
        Paint centerCirclePaint = new Paint();
        centerCirclePaint.setColor(Color.WHITE);
        centerCirclePaint.setStyle(Paint.Style.STROKE);
        centerCirclePaint.setStrokeWidth(5);

        // Coordenadas del centro del campo
        float centerX = (leftBoundary + rightBoundary) / 2;
        float centerY = (topBoundary + bottomBoundary) / 2;
        float centerCircleRadius = 150;
        canvas.drawCircle(centerX, centerY, centerCircleRadius, centerCirclePaint);

        // Línea central vertical
        centerX = (leftBoundary + rightBoundary) / 2;
        canvas.drawLine(centerX, topBoundary, centerX, bottomBoundary, linePaint);

        // Pintura para el contorno del campo
        Paint boundaryPaint = new Paint();
        boundaryPaint.setColor(Color.WHITE);
        boundaryPaint.setStyle(Paint.Style.STROKE);
        boundaryPaint.setStrokeWidth(10); // Grosor del contorno

        // Dibuja el contorno del campo
        canvas.drawRect(leftBoundary, topBoundary, rightBoundary, bottomBoundary, boundaryPaint);

//****************************** MARCO ************************
        // Dibuja las áreas fuera de los límites del campo con relleno de color
        Paint fillPaint = new Paint();
        fillPaint.setColor(Color.GRAY);
        fillPaint.setStyle(Paint.Style.FILL);

        // Dibujo de bordes del campo de juego
        canvas.drawRect(0, 0, leftBoundary, getHeight(), fillPaint);
        canvas.drawRect(rightBoundary, 0, getWidth(), getHeight(), fillPaint);
        canvas.drawRect(0, 0, getWidth(), topBoundary, fillPaint);
        canvas.drawRect(0, bottomBoundary, getWidth(), getHeight(), fillPaint);

//****************************** ESFERA ************************
        // Inicializa la pintura para el círculo
        paint = new Paint();
        paint.setColor(Color.parseColor("#4A4A4A"));
        // Dibuja la circunferencia
        canvas.drawCircle(posX, posY, CIRCLE_RADIUS, paint);

//****************************** TEXTOS PUNTAJE Y TIEMPO ************************
        // Dibuja el contador en la pantalla
        canvas.drawText("Tiempo: " + counter, (getWidth()/2)-350, 75, counterPaint);
        canvas.drawText("Goles: " + counterPlayer, (getWidth()/2) + 60, 75, counterPaint);
        if(isRightTurn){
            canvas.drawText("Anota Aquí ", getWidth()-(getWidth()/5), 75, counterPaint);
        }else{
            canvas.drawText("Anota Aquí ", BORDER_SIZE*2, 75, counterPaint);
        }

//****************************** OBSTACULOS ************************
        Paint obstaclePaint = new Paint();
        obstaclePaint.setColor(Color.parseColor("#EBEBEB"));
        for (Obstacle obstacle : obstacles) {
            obstaclePaint.setColor(Color.parseColor("#EBEBEB"));
            switch (obstacle.getShape()) {
                case Obstacle.SHAPE_CIRCLE:
                    if(obstacle.getX() > getWidth()/2){
                        obstaclePaint.setColor(Color.parseColor("#0023F5"));
                    }else{
                        obstaclePaint.setColor(Color.parseColor("#FFFD55"));
                    }
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

        invalidate();  // Redibuja la vista
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float accelX = -event.values[1];
        float accelY = event.values[0];

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

        if (isGoalLeft() && !isRightTurn) {
            //resetBallPosition();  // Opcional: Reinicia la posición de la esfera al centro
            counterPlayer++;      // Incrementar el contador o registrar el gol
            postInvalidate();     // Actualizar la pantalla
            isRightTurn = true;
        }

        if (isGoalRight() && isRightTurn) {
            //resetBallPosition();  // Opcional: Reinicia la posición de la esfera al centro
            counterPlayer++;      // Incrementar el contador o registrar el gol
            postInvalidate();     // Actualizar la pantalla
            isRightTurn = false;
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

        // Generar obstáculos
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
                        counter--;  // Incrementa el contador
                        postInvalidate();  // Actualiza la vista
                        if (counter == 0) { // Terminar juego
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
        float playerSize = 30;

        // Espacio entre los jugadores en el eje X y Y
        float gapX = (rightBoundary - leftBoundary) / 6;
        float gapY = (bottomBoundary - topBoundary) / 6;

        for (int i = 1; i < 3; i++) {
            // Defensores en diagonal para el equipo izquierdo
            obstacles.add(new Obstacle(Obstacle.SHAPE_CIRCLE, leftBoundary + i * gapX, topBoundary + i * gapY, playerSize));
        }
        for (int i = 1; i < 3; i++) {
            // Delanteros en diagonal para el equipo izquierdo
            obstacles.add(new Obstacle(Obstacle.SHAPE_CIRCLE, leftBoundary + (i + 3) * gapX, topBoundary + (i + 3) * gapY, playerSize));
        }
        for (int i = 1; i < 3; i++) {
            // Defensores en diagonal para el equipo derecho
            obstacles.add(new Obstacle(Obstacle.SHAPE_CIRCLE, rightBoundary - i * gapX, topBoundary + i * gapY, playerSize));
        }
        for (int i = 1; i < 3; i++) {
            // Delanteros en diagonal para el equipo derecho
            obstacles.add(new Obstacle(Obstacle.SHAPE_CIRCLE, rightBoundary - (i + 3) * gapX, topBoundary + (i + 3) * gapY, playerSize));
        }

        gapX = (rightBoundary - leftBoundary) / 12; // Reducir el espacio entre los jugadores (12 jugadores)
        gapY = (bottomBoundary - topBoundary) / 2;  // Alineación vertical

        for (int i = 1; i < 5; i++) {
            obstacles.add(new Obstacle(Obstacle.SHAPE_CIRCLE, leftBoundary + i * gapX + 150, topBoundary + gapY, playerSize));
        }
        for (int i = 1; i < 5; i++) {
            obstacles.add(new Obstacle(Obstacle.SHAPE_CIRCLE, rightBoundary - i * gapX - 150, topBoundary + gapY, playerSize));
        }

        gapY = (bottomBoundary - topBoundary) / 4;  // Alineación vertical

        obstacles.add(new Obstacle(Obstacle.SHAPE_CIRCLE, (getWidth()/2) - 200, topBoundary + gapY, playerSize));
        obstacles.add(new Obstacle(Obstacle.SHAPE_CIRCLE, (getWidth()/2) + 200, topBoundary + gapY, playerSize));
        obstacles.add(new Obstacle(Obstacle.SHAPE_CIRCLE, (getWidth()/2) - 200, (getHeight()/2) + 250, playerSize));
        obstacles.add(new Obstacle(Obstacle.SHAPE_CIRCLE, (getWidth()/2) + 200, (getHeight()/2) + 250, playerSize));

        obstacles.add(new Obstacle(Obstacle.SHAPE_CIRCLE, (getWidth()/2) - 400, (getHeight()/4), playerSize));
        obstacles.add(new Obstacle(Obstacle.SHAPE_CIRCLE, (getWidth()/2) + 400, (getHeight()/4), playerSize));
        obstacles.add(new Obstacle(Obstacle.SHAPE_CIRCLE, (getWidth()/2) - 400, (getHeight()/4)*3 + 50, playerSize));
        obstacles.add(new Obstacle(Obstacle.SHAPE_CIRCLE, (getWidth()/2) + 400, (getHeight()/4) *3 + 50, playerSize));

        gapY = (bottomBoundary - topBoundary) / 4; // Espacio entre los jugadores en el eje Y

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

        obstacles.add(new Obstacle(Obstacle.SHAPE_VERTICAL_BAR,  getWidth()/10 ,getHeight()-120, 130));
        obstacles.add(new Obstacle(Obstacle.SHAPE_VERTICAL_BAR,  (getWidth()/10)*4 ,getHeight()-120, 130));
        obstacles.add(new Obstacle(Obstacle.SHAPE_VERTICAL_BAR,  (getWidth()/10)*2 ,getHeight()-90, 80));
        obstacles.add(new Obstacle(Obstacle.SHAPE_VERTICAL_BAR,  (getWidth()/10)*8 ,getHeight()-90, 80));
        obstacles.add(new Obstacle(Obstacle.SHAPE_VERTICAL_BAR,  (getWidth()/10)*6 ,getHeight()-120, 130));
        obstacles.add(new Obstacle(Obstacle.SHAPE_VERTICAL_BAR,  (getWidth()/10)*9 ,getHeight()-120, 130));

        obstacles.add(new Obstacle(Obstacle.SHAPE_VERTICAL_BAR,  getWidth()/10 ,BORDER_SIZE + 120, 130));
        obstacles.add(new Obstacle(Obstacle.SHAPE_VERTICAL_BAR,  (getWidth()/10)*4 ,BORDER_SIZE + 120, 130));
        obstacles.add(new Obstacle(Obstacle.SHAPE_VERTICAL_BAR,  (getWidth()/10)*2 ,BORDER_SIZE + 90, 80));
        obstacles.add(new Obstacle(Obstacle.SHAPE_VERTICAL_BAR,  (getWidth()/10)*8 ,BORDER_SIZE + 90, 80));
        obstacles.add(new Obstacle(Obstacle.SHAPE_VERTICAL_BAR,  (getWidth()/10)*6 ,BORDER_SIZE + 120, 130));
        obstacles.add(new Obstacle(Obstacle.SHAPE_VERTICAL_BAR,  (getWidth()/10)*9 ,BORDER_SIZE + 120, 130));

        obstacles.add(new Obstacle(Obstacle.SHAPE_HORIZONTAL_BAR, getWidth()/5, topBoundary + (getHeight()/2) + BORDER_SIZE, 200));
        obstacles.add(new Obstacle(Obstacle.SHAPE_HORIZONTAL_BAR, getWidth()/5, (getHeight()/2) - BORDER_SIZE*2 - 11, 200));
        obstacles.add(new Obstacle(Obstacle.SHAPE_HORIZONTAL_BAR, (getWidth()/5)*4, topBoundary + (getHeight()/2) + BORDER_SIZE, 200));
        obstacles.add(new Obstacle(Obstacle.SHAPE_HORIZONTAL_BAR, (getWidth()/5)*4, (getHeight()/2) - BORDER_SIZE*2 - 11, 200));

        obstacles.add(new Obstacle(Obstacle.SHAPE_VERTICAL_BAR,  (getWidth()/4)+50 ,getHeight()-250, 240));
        obstacles.add(new Obstacle(Obstacle.SHAPE_VERTICAL_BAR,  (getWidth()/4)*3-50 ,getHeight()-250, 240));
        obstacles.add(new Obstacle(Obstacle.SHAPE_VERTICAL_BAR,  (getWidth()/4)+50 ,BORDER_SIZE + 250, 240));
        obstacles.add(new Obstacle(Obstacle.SHAPE_VERTICAL_BAR,  (getWidth()/4)*3-50 ,BORDER_SIZE + 250, 240));

        obstacles.add(new Obstacle(Obstacle.SHAPE_VERTICAL_BAR,  (getWidth()/3)+(getWidth()/10)-20 ,BORDER_SIZE*5 + 320, 350));
        obstacles.add(new Obstacle(Obstacle.SHAPE_VERTICAL_BAR,  (getWidth()/3)*2-(getWidth()/10)+20 ,BORDER_SIZE*5 + 320, 350));

        obstacles.add(new Obstacle(Obstacle.SHAPE_HORIZONTAL_BAR, (getWidth()/3)+(getWidth()/10)-20, (getHeight()/2) - BORDER_SIZE*3, 200));
        obstacles.add(new Obstacle(Obstacle.SHAPE_HORIZONTAL_BAR, (getWidth()/3)*2-(getWidth()/10)+20, (getHeight()/2) - BORDER_SIZE*3, 200));

        obstacles.add(new Obstacle(Obstacle.SHAPE_HORIZONTAL_BAR, (getWidth()/3)+(getWidth()/10)-20, BORDER_SIZE*8 + 340, 200));
        obstacles.add(new Obstacle(Obstacle.SHAPE_HORIZONTAL_BAR, (getWidth()/3)*2-(getWidth()/10)+20, BORDER_SIZE*8 +340, 200));
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

package com.example.futbolito;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.View;

public class Sensor extends View implements SensorEventListener {

    private Paint paint;
    private float posX, posY;
    private float speedX = 0, speedY = 0;
    private SensorManager sensorManager;
    private android.hardware.Sensor accelerometer;
    private static final float CIRCLE_RADIUS = 25;
    private static final float BORDER_SIZE = 50;
    private static final float FRICTION = 0.9f;

    // Límites del campo de juego
    private float leftBoundary, rightBoundary, topBoundary, bottomBoundary;

    // Contador
    private int counter = 0;
    private boolean isCounting = false;
    private Thread counterThread;

    public Sensor(Context context) {
        super(context);

        // Inicializa la pintura para el círculo
        paint = new Paint();
        paint.setColor(Color.BLUE);

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
        Paint counterPaint = new Paint();
        counterPaint.setColor(Color.WHITE);
        counterPaint.setTextSize(60);
        canvas.drawText("Tiempo: " + counter, 50, 100, counterPaint);

        // Actualiza la posición con los límites de la pantalla
        posX = Math.max(CIRCLE_RADIUS, Math.min(posX, getWidth() - CIRCLE_RADIUS));
        posY = Math.max(CIRCLE_RADIUS, Math.min(posY, getHeight() - CIRCLE_RADIUS));

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
}

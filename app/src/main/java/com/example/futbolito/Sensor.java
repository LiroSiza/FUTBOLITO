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
    private static final float CIRCLE_RADIUS = 50;
    private static final float FRICTION = 0.9f;

    public Sensor(Context context) {
        super(context);

        // Inicializa la pintura para el círculo
        paint = new Paint();
        paint.setColor(Color.BLUE);

        // Configura el SensorManager y el acelerómetro
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);

        // Inicializa la posición al centro de la pantalla
        posX = getWidth() / 2;
        posY = getHeight() / 2;
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Dibuja la circunferencia
        canvas.drawCircle(posX, posY, CIRCLE_RADIUS, paint);

        // Actualiza la posición con los límites de la pantalla
        posX = Math.max(CIRCLE_RADIUS, Math.min(posX, getWidth() - CIRCLE_RADIUS));
        posY = Math.max(CIRCLE_RADIUS, Math.min(posY, getHeight() - CIRCLE_RADIUS));

        invalidate();  // Redibuja la vista
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // La aceleración en el eje x e y
        float accelX = event.values[0];
        float accelY = event.values[1];

        // Ajusta la velocidad con la fuerza del movimiento
        speedX -= accelX;
        speedY += accelY;

        // Aplica fricción
        speedX *= FRICTION;
        speedY *= FRICTION;

        // Establece un umbral mínimo para la velocidad para evitar temblores
        if (Math.abs(speedX) < 0.1f) speedX = 0;
        if (Math.abs(speedY) < 0.1f) speedY = 0;

        // Actualiza la posición
        posX += speedX;
        posY += speedY;

        // Verifica colisiones con los bordes y ajusta la posición y velocidad

        // Comprobación del límite izquierdo (borde izquierdo de la pantalla)
        if (posX <= CIRCLE_RADIUS) {
            // Si la posición x del círculo está fuera del límite izquierdo, se ajusta al borde
            posX = CIRCLE_RADIUS;
            speedX = 0;
        }

        // Comprobación del límite derecho (borde derecho de la pantalla)
        else if (posX >= getWidth() - CIRCLE_RADIUS) {
            // Si la posición x del círculo está fuera del límite derecho, se ajusta al borde
            posX = getWidth() - CIRCLE_RADIUS;
            speedX = 0;
        }

        // Comprobación del límite superior (borde superior de la pantalla)
        if (posY <= CIRCLE_RADIUS) {
            // Si la posición y del círculo está fuera del límite superior, se ajusta al borde
            posY = CIRCLE_RADIUS;
            speedY = 0;
        }

        // Comprobación del límite inferior (borde inferior de la pantalla)
        else if (posY >= getHeight() - CIRCLE_RADIUS) {
            // Si la posición y del círculo está fuera del límite inferior, se ajusta al borde
            posY = getHeight() - CIRCLE_RADIUS;
            speedY = 0;
        }
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
    }

}

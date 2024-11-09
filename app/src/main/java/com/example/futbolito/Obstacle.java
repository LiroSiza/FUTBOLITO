package com.example.futbolito;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Obstacle {

    public static final int SHAPE_CIRCLE = 0;
    public static final int SHAPE_HORIZONTAL_BAR = 1;
    public static final int SHAPE_VERTICAL_BAR = 2;

    private int shape;
    private float x, y, size;

    public Obstacle(int shape, float x, float y, float size) {
        this.shape = shape;
        this.x = x;
        this.y = y;
        this.size = size;
    }

    // Getters y otros métodos
    public int getShape() {
        return shape;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getSize() {
        return size;
    }
}

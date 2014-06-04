/*
 * Copyright (c) 2013-2014 Chris Newland. All rights reserved.
 * Licensed under https://github.com/chriswhocodes/JFXRay/blob/master/LICENSE-BSD
 * http://www.chrisnewland.com/
 */
package com.chrisnewland.javafx.jfxray;

public class Vector3f
{
    private final float x, y, z; // Vector has three float attributes.

    // Constructor
    public Vector3f(float a, float b, float c)
    {
        x = a;
        y = b;
        z = c;
    }

    // Vector add
    public Vector3f add(Vector3f r)
    {
        return new Vector3f(x + r.x, y + r.y, z + r.z);
    }

    // Vector scaling
    public Vector3f scale(float r)
    {
        return new Vector3f(x * r, y * r, z * r);
    }
    
    public Vector3f invertScale(float r)
    {
        return new Vector3f(x / r, y / r, z / r);
    }

    // Vector dot product
    public float dot(Vector3f r)
    {
        return x * r.x + y * r.y + z * r.z;
    }

    // Cross-product
    public Vector3f cross(Vector3f r)
    {
        return new Vector3f(y * r.z - z * r.y, z * r.x - x * r.z, x * r.y - y * r.x);
    }

    // Used later for normalizing the vector
    public Vector3f normalise()
    {
        float factor = (float) (1f / (float) Math.sqrt(dot(this)));

        return scale(factor);
    }

    public String toString()
    {
        return x + "  " + y + "  " + z;
    }

    public float getX()
    {
        return x;
    }

    public float getY()
    {
        return y;
    }

    public float getZ()
    {
        return z;
    }
}
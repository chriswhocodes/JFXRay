/*
 * Copyright (c) 2013 Chris Newland. All rights reserved.
 * Licensed under https://github.com/chriswhocodes/DirtyMaths/blob/master/LICENSE-BSD
 * http://www.chrisnewland.com/
 */
package com.chrisnewland.dirtymaths;

public class DirtyMaths
{
    // ================================================
    // Precalculated random floats
    // ================================================
    private static float[] randomFloat;
    private static int randomFloatPos = 0;

    public static void initRandomFloat(int count)
    {
        randomFloat = new float[count];

        for (int i = 0; i < count; i++)
        {
            randomFloat[i] = (float) Math.random();
        }
    }

    public synchronized static final float getRandomFloat()
    {
        float result = randomFloat[randomFloatPos++];

        if (randomFloatPos == randomFloat.length)
        {
            randomFloatPos = 0;
        }

        return result;
    }

    // ================================================
    // Precalculated random doubles
    // ================================================
    private static double[] randomDouble;
    private static int randomDoublePos = 0;

    public static void initRandomDouble(int count)
    {
        randomDouble = new double[count];

        for (int i = 0; i < count; i++)
        {
            randomDouble[i] = Math.random();
        }
    }

    public synchronized static final double getRandomDouble()
    {
        double result = randomDouble[randomDoublePos++];

        if (randomDoublePos == randomDouble.length)
        {
            randomDoublePos = 0;
        }

        return result;
    }

    // ================================================
    // Precalculated sine and cosine tables
    // ================================================

    private static double[] sine;
    private static double[] cosine;

    private static int sinCosMultipler;
    private static int sinCosPrecalcLength;

    public static void initSineCosine(double accuracy)
    {
        sinCosMultipler = (int) (1d / accuracy);
        sinCosPrecalcLength = 360 * sinCosMultipler;

        sine = new double[sinCosPrecalcLength];
        cosine = new double[sinCosPrecalcLength];

        double theta = 0;
        double inc = Math.toRadians(accuracy);

        for (int i = 0; i < sinCosPrecalcLength; i++)
        {
            double s = Math.sin(theta);
            double c = Math.cos(theta);

            sine[i] = s;
            cosine[i] = c;

            theta += inc;
        }
    }

    public static final double sin(double degrees)
    {
        while (degrees >= 360)
        {
            degrees -= 360;
        }

        int index = (int) (degrees * sinCosMultipler);

        return sine[index];
    }

    public static final double cos(double degrees)
    {
        while (degrees >= 360)
        {
            degrees -= 360;
        }

        int index = (int) (degrees * sinCosMultipler);

        return cosine[index];
    }

}
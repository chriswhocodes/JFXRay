package com.chrisnewland.javafx.jfxray;

public class DirtyMaths
{
    private static float[] randomFloat;
    
    private static int randomPos = 0;
    
    public static void init(int randomCount)
    {
        randomFloat = new float[randomCount];
        
        for (int i = 0; i < randomCount; i++)
        {
            randomFloat[i] = (float)Math.random();
        }
              
        System.out.println("DirtyMaths initialised");
    }
    
    public synchronized static final float getRandomFloat()
    {

        float result = randomFloat[randomPos++];
        
        if (randomPos >= randomFloat.length)
        {
            randomPos = 0;
        }
        
        return result;

    }
}
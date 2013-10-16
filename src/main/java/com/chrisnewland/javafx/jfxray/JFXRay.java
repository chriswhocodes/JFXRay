package com.chrisnewland.javafx.jfxray;

/*
 * Standing on the shoulders of giants.
 * I did not invent this raytracer, I merely converted it from C to Java 
 * with the help of this web page by Fabien Sanglard:
 * http://fabiensanglard.net/rayTracing_back_of_business_card/index.php
 * The original code is by Andrew Kensler
 */
public class JFXRay
{
    // Define a vector class
    class Vector3f
    {
        float x, y, z; // Vector has three float attributes.

        // Empty constructor
        Vector3f()
        {
        }

        // Constructor
        Vector3f(float a, float b, float c)
        {
            x = a;
            y = b;
            z = c;
        }

        // Vector add
        Vector3f add(Vector3f r)
        {
            return new Vector3f(x + r.x, y + r.y, z + r.z);
        }

        // Vector scaling
        Vector3f scale(float r)
        {
            return new Vector3f(x * r, y * r, z * r);
        }

        // Vector dot product
        float dot(Vector3f r)
        {
            return x * r.x + y * r.y + z * r.z;
        }

        // Cross-product
        Vector3f cross(Vector3f r)
        {
            return new Vector3f(y * r.z - z * r.y, z * r.x - x * r.z, x * r.y - y * r.x);
        }

        // Used later for normalizing the vector
        Vector3f normalise()
        {
            float factor = (float) (1f / (float) Math.sqrt((float) dot(this)));

            return scale(factor);
        }

        public String toString()
        {
            return x + "  " + y + "  " + z;
        }
    };
    
    private byte[] imageData;

    private static boolean[][] data;
    private int rows;
    private int cols;

    private void init(String[] lines)
    {
        cols = lines[0].length();
        rows = lines.length;

        data = new boolean[rows][cols];

        for (int r = 0; r < rows; r++)
        {
            for (int c = 0; c < cols; c++)
            {
                char ch = lines[r].charAt(c);

                data[rows - 1 - r][cols - 1 - c] = ch == '1';
            }
        }
    }

    // Random generator, return a float within range [0-1]
    float randomFloat2()
    {
        return (float) Math.random();
    }

    // The intersection test for line [o,v].
    // Return 2 if a hit was found (and also return distance t and bouncing ray
    // n).
    // Return 0 if no hit was found but ray goes upward
    // Return 1 if no hit was found but ray goes downward

    // Returns object[] 0 = int (m), 1 = float (t), 2 = Vector3f n
    Object[] test(Vector3f o, Vector3f d, Vector3f n)
    {
        float t = 1e9f;

        int m = 0;

        float p2 = -o.z / d.z;

        if (.01 < p2)
        {
            t = p2;
            n = new Vector3f(0, 0, 1);
            m = 1;
        }

        for (int col = 0; col < cols; col++)
        {
            for (int row = 0; row < rows; row++)
            {
                // For this row and column is there a sphere?
                if (data[row][col])
                {
                    // There is a sphere but does the ray hit it ?

                    Vector3f p = o.add(new Vector3f(-col, 0, -row - 4));

                    float b = p.dot(d);
                    float c = p.dot(p) - 1;
                    float q = b * b - c;

                    // Does the ray hit the sphere ?
                    if (q > 0)
                    {

                        float s = -b - (float) Math.sqrt(q);

                        if (s < t && s > .01)
                        { // So far this is the minimum distance, save
                          // it. And // also // compute the bouncing ray
                          // vector into 'n'
                            t = s;
                            n = (p.add(d.scale(t))).normalise();
                            m = 2;

                        }
                    }
                }
            }
        }
        return new Object[] { m, t, n };
    }

    // sample the world and return the pixel color for
    // a ray passing by point o (Origin) and d (Direction)
    Vector3f sample(Vector3f origin, Vector3f direction)
    {
        Vector3f n = new Vector3f();

        // Search for an intersection ray Vs World.
        Object[] result = test(origin, direction, n);

        int m = (int) result[0];
        float t = (float) result[1];
        n = (Vector3f) result[2];

        if (m == 0)
        {
            // No sphere found and the ray goes upward: Generate a sky color
            return new Vector3f(.7f, .6f, 1f).scale((float) Math.pow(1 - direction.z, 4));
        }

        // A sphere was maybe hit.

        // h = intersection coordinate
        Vector3f h = origin.add(direction.scale(t));

        // 'l' = direction to light (with random delta for soft-shadows).
        Vector3f l = new Vector3f(9 + randomFloat2(), 9 + randomFloat2(), 16);

        l = l.add(h.scale(-1));

        l = l.normalise();

        // r = The half-vector
        Vector3f r = direction.add(n.scale(n.dot(direction.scale(-2f))));

        // Calculated the lambertian factor
        float b = l.dot(n);

        // Calculate illumination factor (lambertian coefficient > 0 or in
        // shadow)?
        if (b < 0)
        {
            b = 0;
        }
        else
        {
            result = test(h, l, n);

            int res = (int) result[0];
            t = (float) result[1];
            n = (Vector3f) result[2];

            if (res > 0)
            {
                b = 0;
            }
        }

        // Calculate the color 'p' with diffuse and specular component
        Vector3f rdash = r.scale(b > 0 ? 1 : 0);

        float p = (float) Math.pow(l.dot(rdash), 99);

        if ((m & 1) == 1)
        {
            // No sphere was hit and the ray was going downward:
            h = h.scale(0.2f);

            // Generate a floor color

            int ceil = (int) (Math.ceil(h.x) + Math.ceil(h.y));

            if ((ceil & 1) == 1)
            {
                return new Vector3f(3, 1, 1).scale(b * .2f + .1f);
            }
            else
            {
                return new Vector3f(3, 3, 3).scale(b * .2f + .1f);
            }
        }

        // m == 2 A sphere was hit.
        // Cast an ray bouncing from the sphere surface.

        // Attenuate color by 50% since it is bouncing (* .5)
        return new Vector3f(p, p, p).add(sample(h, r).scale(0.5f));
    }

    public byte[] getImageData()
    {
        return imageData;
    }
    
    public JFXRay()
    {
        
    }
    
    public void render(int ix, int iy, int rays, String[] lines)
    {
        init(lines);
        
        imageData = new byte[ix * iy * 3];

        // Camera direction
        Vector3f g = new Vector3f(-2, -12, 0).normalise();

        // Camera up vector...Seem Z is pointing up :/ WTF !
        Vector3f a = new Vector3f(0, 0, 1).cross(g).normalise().scale(.003f);

        // The right vector, obtained via traditional cross-product
        Vector3f b = g.cross(a).normalise().scale(.003f);

        // WTF ? See https://news.ycombinator.com/item?id=6425965 for more.
        Vector3f c = a.add(b).scale(-256).add(g);

        int pixel = 0;
        
        for (int y = iy - 1; y >= 0; y--)
        {
            // For each column
            for (int x= ix - 1; x >= 0; x--)
            { // For each pixel in a line

                // Reuse the vector class to store not XYZ but a RGB pixel color
                // Default pixel color is almost pitch black
                Vector3f p = new Vector3f(13, 13, 13);

                // Cast 64 rays per pixel (For blur (stochastic sampling) and
                // soft-shadows.
                for (int r = rays-1; r >= 0; r--)
                {
                    // The delta to apply to the origin of the view (For Depth
                    // of View blur).

                    // v t = a * (R() - .5) * 99 + b * (R() - .5) * 99;

                    // A little bit of delta up/down and left/right
                    Vector3f t = a.scale(randomFloat2() - 0.5f);
                    t = t.scale(99);

                    Vector3f t2 = b.scale(randomFloat2() - 0.5f);
                    t2 = t2.scale(99);

                    t = t.add(t2);

                    // Set the camera focal point v(17,16,8) and Cast the ray
                    // Accumulate the color returned in the p variable
                    // Ray Direction with random deltas for stochastic sampling

                    Vector3f dirA = a.scale(randomFloat2() + x);
                    Vector3f dirB = b.scale(randomFloat2() + y);
                    Vector3f dirC = dirA.add(dirB).add(c);

                    Vector3f dir = t.scale(-1f).add(dirC.scale(16f)).normalise();

                    // Ray Origin +p for color accumulation
                    p = sample(new Vector3f(17f, 16f, 8f).add(t), dir).scale(3.5f).add(p);

                }

                imageData[pixel++] = (byte)p.x;
                imageData[pixel++] = (byte)p.y;
                imageData[pixel++] = (byte)p.z;
            }
        }
    }
}

/*
 * Copyright (c) 2013 Chris Newland. All rights reserved.
 * Licensed under https://github.com/chriswhocodes/JFXRay/blob/master/LICENSE-BSD
 * http://www.chrisnewland.com/
 */
package com.chrisnewland.javafx.jfxray;

import java.util.concurrent.ThreadLocalRandom;

/*
 * Standing on the shoulders of giants.
 * I did not invent this raytracer, I merely converted it from C to Java 
 * with the help of this web page by Fabien Sanglard:
 * http://fabiensanglard.net/rayTracing_back_of_business_card/index.php
 * The original code is by Andrew Kensler
 */
public class JFXRay
{
	private byte[] imageData;

	private boolean[][] data;
	private int rows;
	private int cols;

	private Vector3f floorColourOdd;
	private Vector3f floorColourEven;
	private Vector3f skyColour;

	private float sphereReflectivity;;

	private long renderStart = 0;
	private long renderTime = 0;

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

				data[rows - 1 - r][cols - 1 - c] = ch == '*';
			}
		}
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

		float p2 = -o.getZ() / d.getZ();

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
		Vector3f n = new Vector3f(0, 0, 0);

		// Search for an intersection ray Vs World.
		Object[] result = test(origin, direction, n);

		int m = (int) result[0];
		float t = (float) result[1];
		n = (Vector3f) result[2];

		if (m == 0)
		{
			// No sphere found and the ray goes upward: Generate a sky color
			return skyColour.scale((float) Math.pow(1 - direction.getZ(), 4));
		}

		// A sphere was maybe hit.

		// h = intersection coordinate
		Vector3f h = origin.add(direction.scale(t));

		// 'l' = direction to light (with random delta for soft-shadows).
		Vector3f l = new Vector3f(9 + getRandomFloat(), 9 + getRandomFloat(), 16);

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

		float p = (float) Math.pow(l.dot(rdash), 64);

		if (m == 1)
		{
			// No sphere was hit and the ray was going downward:
			h = h.invertScale(4);

			// Generate a floor color
			int ceil = (int) (Math.ceil(h.getX()) + Math.ceil(h.getY()));

			if ((ceil & 1) == 1)
			{
				return floorColourOdd.scale(b / 4 + .1f);
			}
			else
			{
				return floorColourEven.scale(b / 4 + .1f);
			}
		}

		// m == 2 A sphere was hit.
		// Cast an ray bouncing from the sphere surface.

		// Attenuate color since it is bouncing
		return new Vector3f(p, p, p).add(sample(h, r).scale(sphereReflectivity));
	}

	public byte[] getImageData()
	{
		renderTime = System.currentTimeMillis() - renderStart;
		return imageData;
	}

	public JFXRay()
	{

	}

	public void render(final RenderConfig config)
	{
		renderStart = System.currentTimeMillis();

		this.floorColourOdd = config.getOddColour();
		this.floorColourEven = config.getEvenColour();
		this.skyColour = config.getSkyColour();
		this.sphereReflectivity = config.getSphereReflectivity();

		init(config.getLines());

		imageData = new byte[config.getImageWidth() * config.getImageHeight() * 3];

		// Camera direction
		final Vector3f g = config.getCamDirection().normalise();

		// Camera up vector...Seem Z is pointing up :/ WTF !
		final Vector3f a = new Vector3f(0, 0, 1).cross(g).normalise().scale(.003f);

		// The right vector, obtained via traditional cross-product
		final Vector3f b = g.cross(a).normalise().scale(.003f);

		// WTF ? See https://news.ycombinator.com/item?id=6425965 for more.
		final Vector3f c = a.add(b).scale(-256).add(g);

		final int linesPerThread = config.getImageHeight() / config.getThreads();

		// System.out.println("LinesPerThread: " + linesPerThread);

		Thread[] workers = new Thread[config.getThreads()];

		final Vector3f defaultPixelColour = new Vector3f(16, 16, 16);

		for (int i = 0; i < config.getThreads(); i++)
		{
			final int startingLine = config.getImageHeight() - 1 - (i * linesPerThread);
			final int pixelBufferOffset = i * linesPerThread;

			// System.out.println("Thread " + i + " plotting " + startingLine);

			Thread worker = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					int pixel = config.getImageWidth() * pixelBufferOffset * 3;

					// For each line
					for (int y = startingLine; y > startingLine - linesPerThread; y--)
					{
						// For each pixel in a line
						for (int x = config.getImageWidth() - 1; x >= 0; x--)
						{
							// Reuse the vector class to store not XYZ but an
							// RGB
							// pixel color
							// Default pixel color is almost pitch black
							Vector3f p = defaultPixelColour;

							// Cast rays per pixel (For blur (stochastic
							// sampling) and
							// soft-shadows.
							for (int r = config.getRays() - 1; r >= 0; r--)
							{
								// The delta to apply to the origin of the view
								// (For Depth of View blur).

								// A little bit of delta up/down and left/right
								Vector3f t = a.scale(getRandomFloat() - 0.5f);
								t = t.scale(64);

								Vector3f t2 = b.scale(getRandomFloat() - 0.5f);
								t2 = t2.scale(64);

								t = t.add(t2);

								// Set the camera focal point and
								// Cast the ray
								// Accumulate the color returned in the p
								// variable
								// Ray Direction with random deltas for
								// stochastic sampling

								Vector3f dirA = a.scale(getRandomFloat() + x);
								Vector3f dirB = b.scale(getRandomFloat() + y);
								Vector3f dirC = dirA.add(dirB).add(c);

								Vector3f dir = t.scale(-1).add(dirC.scale(16)).normalise();

								// Ray Origin +p for color accumulation
								p = sample(config.getRayOrigin().add(t), dir).scale(config.getBrightness()).add(p);

							}

							imageData[pixel++] = (byte) p.getX();
							imageData[pixel++] = (byte) p.getY();
							imageData[pixel++] = (byte) p.getZ();
						}
					}
				}
			});

			worker.start();
			workers[i] = worker;
		}

		for (int i = 0; i < config.getThreads(); i++)
		{
			try
			{
				workers[i].join();
			}
			catch (InterruptedException ie)
			{
				ie.printStackTrace();
			}
		}

		renderTime = System.currentTimeMillis() - renderStart;
	}

	private float getRandomFloat()
	{
		return ThreadLocalRandom.current().nextFloat();
	}

	public long getRenderTime()
	{
		return renderTime;
	}
}

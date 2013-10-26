/*
 * Copyright (c) 2013 Chris Newland. All rights reserved.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * http://www.chrisnewland.com/jitwatch
 */
package com.chrisnewland.javafx.jfxray;

import java.nio.ByteBuffer;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.TimelineBuilder;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

public class JFXRayApp extends Application
{
    private GraphicsContext gc;
    private JFXRay raytracer;

    final String[] pattern = new String[9];

    final int ix = 512;
    final int iy = 512;
    final int rays = 64;

    // Called by JFX
    public JFXRayApp()
    {

    }

    public JFXRayApp(String[] args)
    {
        launch(args);
    }

    @Override
    public void start(final Stage stage)
    {
        stage.setOnCloseRequest(new EventHandler<WindowEvent>()
        {
            @Override
            public void handle(WindowEvent arg0)
            {

            }
        });

        int width = 600;
        int height = 600;

        Canvas canvas = new Canvas(width, height);
        gc = canvas.getGraphicsContext2D();

        VBox box = new VBox();
        box.getChildren().add(canvas);

        Scene scene = new Scene(box, width, height);

        stage.setTitle("JFXRay");
        stage.setScene(scene);
        stage.show();

        pattern[0] = "1111111 111111 1       1";
        pattern[1] = "   1    1       1     1 ";
        pattern[2] = "   1    1        1   1  ";
        pattern[3] = "   1    1         1 1   ";
        pattern[4] = "   1    11111      1    ";
        pattern[5] = "   1    1         1 1   ";
        pattern[6] = "   1    1        1   1  ";
        pattern[7] = "   1    1       1     1 ";
        pattern[8] = "1111    1      1       1";

        raytracer = new JFXRay();

        Thread t = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                int threads = 4;
                raytracer.render(ix, iy, rays, pattern, threads);
            }
        });

        t.start();

        int refresh = 200; // ms

        final Duration oneFrameAmt = Duration.millis(refresh);

        final KeyFrame oneFrame = new KeyFrame(oneFrameAmt, new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent arg0)
            {
                if (raytracer != null)
                {
                    byte[] imgData = raytracer.getImageData();

                    PixelWriter pixelWriter = gc.getPixelWriter();

                    PixelFormat<ByteBuffer> pixelFormat = PixelFormat.getByteRgbInstance();

                    pixelWriter.setPixels(0, 0, ix, iy, pixelFormat, imgData, 0, ix * 3);
                }

            }
        });

        TimelineBuilder.create().cycleCount(Animation.INDEFINITE).keyFrames(oneFrame).build().play();

    }
}
/*
 * Copyright (c) 2013 Chris Newland. All rights reserved.
 * Licensed under https://github.com/chriswhocodes/JFXRay/blob/master/LICENSE-BSD
 * http://www.chrisnewland.com/
 */
package com.chrisnewland.javafx.jfxray;

import java.nio.ByteBuffer;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TimelineBuilder;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

public class JFXRayApp extends Application
{
    private static final int labelWidth = 140;
    private static final int valueWidth = 45;

    class VectorInput
    {
        private HBox hb;

        private float defaultX;
        private float defaultY;
        private float defaultZ;

        private TextField tfX;
        private TextField tfY;
        private TextField tfZ;

        public VectorInput(String labelText, float defaultX, float defaultY, float defaultZ)
        {
            this.defaultX = defaultX;
            this.defaultY = defaultY;
            this.defaultZ = defaultZ;

            Label label = new Label(labelText);
            label.setPrefWidth(labelWidth);

            tfX = new TextField(Float.toString(defaultX));
            tfY = new TextField(Float.toString(defaultY));
            tfZ = new TextField(Float.toString(defaultZ));

            tfX.setPrefWidth(valueWidth);
            tfY.setPrefWidth(valueWidth);
            tfZ.setPrefWidth(valueWidth);

            hb = new HBox();
            hb.getChildren().add(label);
            hb.getChildren().add(tfX);
            hb.getChildren().add(tfY);
            hb.getChildren().add(tfZ);
        }

        public HBox getHBox()
        {
            return hb;
        }

        public Vector3f getVector3f()
        {
            Vector3f result = null;

            try
            {
                float x = Float.parseFloat(tfX.getText());
                float y = Float.parseFloat(tfY.getText());
                float z = Float.parseFloat(tfZ.getText());

                result = new Vector3f(x, y, z);
            }
            catch (NumberFormatException nfe)
            {
                result = new Vector3f(defaultX, defaultY, defaultZ);
            }

            return result;
        }
    }

    private GraphicsContext gc;
    private JFXRay raytracer;

    private Button btnRayTrace;
    private TextArea taPattern;

    private TextField tfRays;
    private TextField tfThreads;

    private VectorInput viRayOrigin;
    private VectorInput viCamDirection;
    private VectorInput viOddColour;
    private VectorInput viEvenColour;
    private VectorInput viSkyColour;

    private Timeline timeline;

    String[] pattern = new String[9];

    final int canvasWidth = 512;
    final int canvasHeight = 512;

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

        int width = 800;
        int height = canvasHeight;

        Canvas canvas = new Canvas(canvasWidth, canvasHeight);
        gc = canvas.getGraphicsContext2D();
        gc.fillRect(0, 0, canvasWidth, canvasHeight);

        pattern[0] = "******* ****** *       *";
        pattern[1] = "   *    *       *     * ";
        pattern[2] = "   *    *        *   *  ";
        pattern[3] = "   *    *         * *   ";
        pattern[4] = "   *    *****      *    ";
        pattern[5] = "   *    *         * *   ";
        pattern[6] = "   *    *        *   *  ";
        pattern[7] = "   *    *       *     * ";
        pattern[8] = "****    *      *       *";

        taPattern = new TextArea();
        taPattern.setStyle("-fx-font-family:monospace;");

        for (String line : pattern)
        {
            taPattern.appendText(line + "\n");
        }

        int cores = Runtime.getRuntime().availableProcessors();

        Label lblThreads = new Label("Rendering threads");
        lblThreads.setPrefWidth(labelWidth);

        tfThreads = new TextField(Integer.toString(cores));
        tfThreads.setPrefWidth(valueWidth);

        HBox hbThreads = new HBox();
        hbThreads.getChildren().add(lblThreads);
        hbThreads.getChildren().add(tfThreads);

        Label lblRays = new Label("Rays per pixel");
        lblRays.setPrefWidth(labelWidth);

        tfRays = new TextField("64");
        tfRays.setPrefWidth(valueWidth);

        HBox hbRays = new HBox();
        hbRays.getChildren().add(lblRays);
        hbRays.getChildren().add(tfRays);

        viRayOrigin = new VectorInput("Ray origin", 17f, 20f, 8f);
        viCamDirection = new VectorInput("Camera direction", -2f, -12f, 0f);
        viOddColour = new VectorInput("Odd Colour", 3f, 3f, 3f);
        viEvenColour = new VectorInput("Even Colour", 3f, 1f, 1f);
        viSkyColour = new VectorInput("Sky Colour", .7f, .6f, 1f);

        btnRayTrace = new Button("RayTrace!");
        btnRayTrace.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent e)
            {
                startRaytracing();
            }
        });

        VBox vBoxControls = new VBox();

        vBoxControls.getChildren().add(taPattern);
        vBoxControls.getChildren().add(hbThreads);
        vBoxControls.getChildren().add(hbRays);
        vBoxControls.getChildren().add(viRayOrigin.getHBox());
        vBoxControls.getChildren().add(viCamDirection.getHBox());
        vBoxControls.getChildren().add(viOddColour.getHBox());
        vBoxControls.getChildren().add(viEvenColour.getHBox());
        vBoxControls.getChildren().add(viSkyColour.getHBox());
        vBoxControls.getChildren().add(btnRayTrace);

        HBox box = new HBox();
        box.getChildren().add(vBoxControls);
        box.getChildren().add(canvas);

        Scene scene = new Scene(box, width, height);

        stage.setTitle("JFXRay");
        stage.setScene(scene);
        stage.show();

        int refresh = 500; // ms

        final Duration oneFrameAmt = Duration.millis(refresh);

        final KeyFrame oneFrame = new KeyFrame(oneFrameAmt, new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent arg0)
            {
                if (raytracer != null)
                {
                    updateCanvas();
                }
            }
        });

        timeline = TimelineBuilder.create().cycleCount(Animation.INDEFINITE).keyFrames(oneFrame).build();
    }

    private void startRaytracing()
    {
        btnRayTrace.setDisable(true);

        raytracer = new JFXRay();

        Thread t = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                String patternText = taPattern.getText();
                String[] lines = patternText.split("\n");

                int maxWidth = 0;

                for (String line : lines)
                {
                    if (line.length() > maxWidth)
                    {
                        maxWidth = line.length();
                    }
                }

                for (int i = 0; i < lines.length; i++)
                {
                    String line = lines[i];

                    if (line.length() < maxWidth)
                    {
                        lines[i] = padLine(line, maxWidth);
                    }
                }

                pattern = lines;

                int threads = 2;
                try
                {
                    threads = Integer.parseInt(tfThreads.getText());
                }
                catch (NumberFormatException nfe)
                {
                }

                int rays = 64;
                try
                {
                    rays = Integer.parseInt(tfRays.getText());
                }
                catch (NumberFormatException nfe)
                {
                }

                timeline.play();

                raytracer.render(canvasWidth, canvasHeight, rays, pattern, threads, viRayOrigin.getVector3f(), viCamDirection.getVector3f(),
                        viOddColour.getVector3f(), viEvenColour.getVector3f(), viSkyColour.getVector3f());

                updateCanvas();

                timeline.stop();

                btnRayTrace.setDisable(false);
            }
        });

        t.start();
    }

    private void updateCanvas()
    {
        byte[] imgData = raytracer.getImageData();

        PixelWriter pixelWriter = gc.getPixelWriter();

        PixelFormat<ByteBuffer> pixelFormat = PixelFormat.getByteRgbInstance();

        pixelWriter.setPixels(0, 0, canvasWidth, canvasHeight, pixelFormat, imgData, 0, canvasWidth * 3);
    }

    private String padLine(String line, int width)
    {
        int pad = width - line.length();

        for (int i = 0; i < pad; i++)
        {
            line += " ";
        }

        return line;
    }
}
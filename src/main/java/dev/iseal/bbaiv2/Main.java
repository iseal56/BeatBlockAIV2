package dev.iseal.bbaiv2;

import dev.iseal.bbaiv2.managers.CNNModelManager;
import dev.iseal.bbaiv2.managers.PerfManager;
import dev.iseal.bbaiv2.misc.utils.ExceptionHandler;
import dev.iseal.bbaiv2.misc.utils.ImageProcessor;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        boolean runUpdater = true;
        if (args.length != 0) {
            if (Arrays.stream(args).anyMatch(s -> s.equalsIgnoreCase("-noRunUpdater"))) {
                runUpdater = false;
            } else {
                System.out.println("Usage: java -jar bbaiv2.jar [-noRunUpdater]");
            }
        }

        System.out.println("Backend: " + Nd4j.getBackend().getClass().getSimpleName());
        MultiLayerNetwork model = CNNModelManager.loadModel(false);

        int testSeconds = 10;
        long startTime = System.currentTimeMillis();
        long endTime = startTime + testSeconds * 1000;
        int count = 0;

        System.out.println("Starting " + testSeconds + " seconds");
        ImageProcessor.getInstance().start();
        System.out.println("Settings: "+ImageProcessor.screenRect.width+" by "+ImageProcessor.screenRect.height);

        while (ImageProcessor.getProcessedScreenShot() == null) {
            System.out.println("Waiting for image...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                ExceptionHandler.error(e);
            }
        }

        while (System.currentTimeMillis() < endTime) {
            PerfManager.push();
            // Load image
            INDArray image = ImageProcessor.getProcessedScreenShot();

            // Run inference
            INDArray output = model.output(image);

            // Extract values
            double x = output.getDouble(0, 0); // X value 0 to 1
            double y = output.getDouble(0, 1); // Y value 0 to 1
            int mappedX = (int) (x * ImageProcessor.screenRect.width);
            int mappedY = (int) (y * ImageProcessor.screenRect.height);
            boolean click = output.getDouble(0, 2) > 0.5; // Click threshold

            System.out.println("Output X: " + x);
            System.out.println("Output Y: " + y);
            System.out.println("Mouse X: " + mappedX);
            System.out.println("Mouse Y: " + mappedY);
            System.out.println("Click: " + click);

            count++;
            PerfManager.pop("Main.mainLoop");
        }
        try {
            ImageProcessor.getInstance().stop();
        } catch (InterruptedException e) {
            ExceptionHandler.error(e);
        }

        double runsPerSecond = count / (double) testSeconds;
        System.out.println("Ran " + count + " times in "+ testSeconds + " seconds.");
        System.out.println("Runs per second: " + runsPerSecond);
        CNNModelManager.saveModel(model);
        PerfManager.popStack();
    }
}
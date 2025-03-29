package dev.iseal.bbaiv2.managers;

import dev.iseal.bbaiv2.misc.utils.ImageProcessor;
import dev.iseal.bbaiv2.webserver.SparkManager;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import spark.Spark;

public class ModelManager {

    private static ModelManager instance;
    public static ModelManager getInstance() {
        if (instance == null) {
            instance = new ModelManager();
        }
        return instance;
    }

    private MultiLayerNetwork model;
    private final SparkManager sparkManager = SparkManager.getInstance();

    public void startRunningModel(boolean runUpdater) {
        model = CNNModelManager.loadModel(runUpdater);

        // wait for first game to start
        while (!SparkManager.isInGame()) {
            try {
                System.out.println("SparkManager reports not in game, waiting for game to start...");
                System.out.println("(If this message persists, check if you have the mod installed.)");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void update() {
        // loop
        PerfManager.push();

        INDArray lastImage = ImageProcessor.getProcessedScreenShot();

        while (SparkManager.isInGame()) {
            // Load image
            INDArray image = ImageProcessor.getProcessedScreenShot();

            // Run inference
            INDArray output = model.output(image);

            sparkManager.waitForNewBarelies();
            sparkManager.calculateReward();

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

            // execute action
            // TODO: Implement action execution

            lastImage = image;

            PerfManager.pop("Main.mainLoop");
        }

        handleEndGame();
    }

    private void handleEndGame() {
        float grade = SparkManager.getInstance().calculateGrade();
        System.out.println("Game ended. Grade: " + grade);
    }

    public MultiLayerNetwork getModel() {
        return model;
    }
}

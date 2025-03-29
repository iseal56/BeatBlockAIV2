package dev.iseal.bbaiv2.misc.utils;

import dev.iseal.bbaiv2.managers.PerfManager;
import org.datavec.image.loader.NativeImageLoader;
import org.nd4j.linalg.api.ndarray.INDArray;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.*;

public class ImageProcessor {

    private static final ImageProcessor instance = new ImageProcessor();
    public static ImageProcessor getInstance() {
        return instance;
    }

    // Dynamic values for fps adjusting
    private static int TARGET_FPS = 30;
    private ThreadPoolExecutor executor;

    public static final int HEIGHT = 360;
    public static final int WIDTH = 640;
    public static final int CHANNELS = 1; // Grayscale

    private int lastImageHash = 0;

    private final Robot robot;
    public static final Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final NativeImageLoader loader = new NativeImageLoader(HEIGHT, WIDTH, CHANNELS);

    private INDArray latestImage;

    public ImageProcessor() {
        Robot robot1;
        try {
            robot1 = new Robot();
        } catch (AWTException e) {
            // panic will never return
            ExceptionHandler.panic(e);
            robot1 = null;
        }
        robot = robot1;
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        executor.setRejectedExecutionHandler((runnable, threadPoolExecutor) -> {
            // target is too high, reduce FPS and ignore runnable.
            TARGET_FPS -= 1;
            System.out.println("[ImageProcessor] Rejected task! Adjusted FPS: " + TARGET_FPS);
        });
    }

    public void start() {
        long period = 1000 / TARGET_FPS;
        // rebuild the executor pool based on new FPS
        int poolSize = Math.max(1, TARGET_FPS / 5);
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(poolSize);
        scheduler.scheduleAtFixedRate(
                () -> executor.execute(() -> {
                    try {
                        BufferedImage image = takeImage();
                        latestImage = loadImage(image);
                    } catch (AWTException | IOException e) {
                        ExceptionHandler.panic(e);
                    }
                }),
                0,
                period,
                TimeUnit.MILLISECONDS
        );
    }

    public void stop() throws InterruptedException {
        boolean terminated = executor.awaitTermination(1000, TimeUnit.MILLISECONDS);
        if (!terminated) {
            executor.shutdownNow();
        }
        scheduler.close();
    }

    private INDArray loadImage(BufferedImage initialImage) throws IOException {
        PerfManager.push();
        // Convert BufferedImage to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(initialImage, "png", baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        // Load image into INDArray
        INDArray image = loader.asMatrix(bais);

        // Normalize pixel values (0 to 1)
        image.divi(255.0);

        // Reshape to (batchSize, CHANNELS, HEIGHT, WIDTH)
        INDArray output = image.reshape(1, CHANNELS, HEIGHT, WIDTH);
        PerfManager.pop("ImageProcessor.loadImage");
        return output;
    }

    private BufferedImage takeImage() throws AWTException {
        PerfManager.push();
        // Take a screenshot and convert to grayscale
        BufferedImage screenshot = robot.createScreenCapture(screenRect);
        BufferedImage grayImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = grayImage.createGraphics();
        g.drawImage(screenshot, 0, 0, WIDTH, HEIGHT, null);
        g.dispose();
        PerfManager.pop("ImageProcessor.takeImage");
        return grayImage;
    }

    public static INDArray getProcessedScreenShot() {
        INDArray latestImage = getInstance().latestImage;
        if (latestImage == null) {
            System.out.println("[ImageProcessor] No image available yet. Returning null.");
            return null;
        }
        int hash = latestImage.hashCode();
        if (hash == getInstance().lastImageHash) {
            System.out.println("[ImageProcessor] Returning same image twice! Reducing target FPS and adjusting executor pool...");
            TARGET_FPS += 5;
            // close old tasks and re-schedule with new FPS
            scheduler.shutdownNow();
            getInstance().executor.shutdownNow();
            getInstance().start(); // restart scheduling with updated FPS
        }
        getInstance().lastImageHash = hash;
        return latestImage;
    }
}
package dev.iseal.bbaiv2.managers;

import dev.iseal.bbaiv2.misc.utils.ExceptionHandler;
import dev.iseal.bbaiv2.misc.utils.ImageProcessor;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;

public class CNNModelManager {

    public static MultiLayerNetwork createModel() {
        PerfManager.push();
        Nd4j.setDefaultDataTypes(DataType.FLOAT, DataType.FLOAT);

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Adam(0.001))
                .list()
                .layer(0, new ConvolutionLayer.Builder(5, 5)
                        .nIn(ImageProcessor.CHANNELS)
                        .nOut(32)
                        .stride(1, 1)
                        .activation(Activation.RELU)
                        .build())
                .layer(1, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                        .kernelSize(2, 2)
                        .stride(2, 2)
                        .build())
                .layer(2, new DenseLayer.Builder()
                        .nOut(128)
                        .dropOut(0.5)
                        .activation(Activation.RELU)
                        .build())
                .layer(3, new OutputLayer.Builder(LossFunctions.LossFunction.MSE) // Regression
                        .nOut(3) // X, Y, and Click
                        .activation(Activation.SIGMOID) // No activation for raw outputs
                        .build())
                .setInputType(InputType.convolutional(ImageProcessor.HEIGHT, ImageProcessor.WIDTH, ImageProcessor.CHANNELS))
                .build();

        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();
        PerfManager.pop("CNNModelManager.createModel");
        return model;
    }


    public static void saveModel(MultiLayerNetwork model) {
        PerfManager.push();
        File modelFile = new File(System.getProperty("user.dir")+ File.separator + "model.zip");
        try {
            model.save(modelFile, true);
        } catch (Exception e) {
            ExceptionHandler.error(e);
        }
        PerfManager.pop("CNNModelManager.saveModel");
    }

    public static MultiLayerNetwork loadModel(boolean runUpdater) {
        File modelFile = new File(System.getProperty("user.dir")+ File.separator + "model.zip");

        if (!modelFile.exists()) {
            return createModel();
        }

        try {
            PerfManager.push();
            MultiLayerNetwork model = MultiLayerNetwork.load(modelFile, runUpdater);
            System.out.println("Model loaded into ram. RAM usage (in GB): " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/1073741824.0);
            PerfManager.pop("CNNModelManager.loadModel");
            return model;
        } catch (Exception e) {
            ExceptionHandler.error(e);
            return null;
        }

    }

}

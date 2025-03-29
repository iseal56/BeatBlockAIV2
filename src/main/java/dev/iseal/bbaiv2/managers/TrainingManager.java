package dev.iseal.bbaiv2.managers;

import dev.iseal.bbaiv2.misc.holders.Experience;
import dev.iseal.bbaiv2.misc.utils.ImageProcessor;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.awt.font.MultipleMaster;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrainingManager {

    private static final double DISCOUNT_FACTOR = 0.99;
    private final List<Experience> experienceBuffer = new ArrayList<>();

    // Add experience with two ints for X, Y and a boolean for click.
    public void addExperience(INDArray currentState, int actionX, int actionY, float clickChance, double reward, INDArray nextState) {
        Experience exp = new Experience(currentState, actionX, actionY, clickChance, reward, nextState);
        experienceBuffer.add(exp);
    }

    // Train on a mini-batch of experiences
    public void train() {
        MultiLayerNetwork model = ModelManager.getInstance().getModel();
        Collections.shuffle(experienceBuffer);
        for (Experience exp : experienceBuffer) {
            INDArray currentQValues = model.output(exp.getCurrentState(), false);
            INDArray nextQValues = model.output(exp.getNextState(), false);

            // Calculate updated values using reward and discount factor
            double reward = exp.getReward();
            double futureEstimate = nextQValues.maxNumber().doubleValue();
            double updatedValue = reward + DISCOUNT_FACTOR * futureEstimate;

            // Prepare target
            INDArray target = currentQValues.dup();
            target.putScalar(0, updatedValue);

            // Fit the model
            model.fit(exp.getCurrentState(), target);
        }

        // Clear the buffer
        clearBuffer();
    }

    public void clearBuffer() {
        experienceBuffer.clear();
    }
}
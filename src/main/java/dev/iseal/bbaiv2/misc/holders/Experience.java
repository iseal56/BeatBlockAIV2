package dev.iseal.bbaiv2.misc.holders;

import org.nd4j.linalg.api.ndarray.INDArray;

public class Experience {
    private final INDArray currentState;
    private final int actionX;
    private final int actionY;
    private final float click;
    private final double reward;
    private final INDArray nextState;

    public Experience(INDArray currentState, int actionX, int actionY, float click, double reward, INDArray nextState) {
        this.currentState = currentState;
        this.actionX = actionX;
        this.actionY = actionY;
        this.click = click;
        this.reward = reward;
        this.nextState = nextState;
    }

    public INDArray getCurrentState() {
        return currentState;
    }

    public int getActionX() {
        return actionX;
    }

    public int getActionY() {
        return actionY;
    }

    public float getClick() {
        return click;
    }

    public double getReward() {
        return reward;
    }

    public INDArray getNextState() {
        return nextState;
    }
}
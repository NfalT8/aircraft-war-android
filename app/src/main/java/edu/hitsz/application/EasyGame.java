package edu.hitsz.application;

import android.content.Context;

public class EasyGame extends BaseGame {

    public EasyGame(Context context) {
        super(context);
    }

    @Override
    protected void initDifficultyParams() {
        this.difficulty = GameDifficulty.EASY;
        this.shootCycle = 25;
        this.enemyMaxNumber = 5;
        this.bossScore = Integer.MAX_VALUE;
        this.enemySpawnCycle = 35;
    }

    @Override
    protected void increaseDifficultyWithTime() {
    }

    @Override
    protected boolean shouldGenerateBoss() {
        return false;
    }

    @Override
    protected void generateBoss() {
    }

    @Override
    protected void resetDifficultyState() {
        initDifficultyParams();
    }
}

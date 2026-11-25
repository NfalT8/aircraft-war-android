package edu.hitsz.application;

import android.content.Context;

import edu.hitsz.aircraft.AbstractAircraft;

public class HardGame extends BaseGame {

    private int currentBossHp = 400;
    private int bossHpIncrement = 100;

    private int timeTick = 0;
    private int difficultyLevel = 1;

    public HardGame(Context context) {
        super(context);
    }

    @Override
    protected void initDifficultyParams() {
        this.difficulty = GameDifficulty.HARD;

        this.enemyMaxNumber = 8;
        this.probElite = 0.3;
        this.bossScore = 400;

        this.enemySpawnCycle = 25;
        this.shootCycle = 6;
        this.enemyShootCycle = 35;
    }

    @Override
    protected void increaseDifficultyWithTime() {
        timeTick++;

        //30s提升一次难度
        if (timeTick >= 750) {
            timeTick = 0;
            difficultyLevel++;

            this.enemySpawnCycle = Math.max(10, this.enemySpawnCycle - 1);

            this.enemyShootCycle = Math.max(15, this.enemyShootCycle - 2);

            this.probElite = Math.min(0.9, this.probElite + 0.02);
            this.probElitePlus = Math.min(0.9, this.probElitePlus + 0.02);
            this.probElitePro = Math.min(0.9, this.probElitePro + 0.01);

            AbstractAircraft.setHpMultiplier(AbstractAircraft.getHpMultiplier() + 0.05);
            AbstractAircraft.setSpeedMultiplier(AbstractAircraft.getSpeedMultiplier() + 0.05);
        }
    }

    @Override
    protected boolean shouldGenerateBoss() {
        return score >= bossScore && !bossExisted;
    }

    @Override
    protected void generateBoss() {
        enemyAircrafts.add(bossEnemyFactory.createEnemy(currentBossHp));

        bossExisted = true;
        currentBossHp += bossHpIncrement;
        bossScore += 400;
        onBossSpawned();
    }

    @Override
    protected void resetDifficultyState() {
        currentBossHp = 400;
        timeTick = 0;
        difficultyLevel = 1;
        initDifficultyParams();
    }
}

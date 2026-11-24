package edu.hitsz.application;

import android.content.Context;

import edu.hitsz.aircraft.AbstractAircraft;

public class NormalGame extends BaseGame {

    private int currentBossHp = 300; // 普通模式初始 Boss 血量
    private int timeTick = 0;           // 用于记录时间推移的计数器
    private int difficultyLevel = 1;    // 记录难度提升的等级

    public NormalGame(Context context) {
        super(context);
    }

    @Override
    protected void initDifficultyParams() {
        // 设置普通模式专属参数
        this.difficulty = GameDifficulty.NORMAL;
        this.enemyMaxNumber = 7;
        this.bossScore = 450;
        this.enemySpawnCycle = 30;
    }

    @Override
    protected void increaseDifficultyWithTime() {
        timeTick++;

        if (timeTick >= 750) {
            timeTick = 0;
            difficultyLevel++;

            this.enemySpawnCycle = Math.max(15, this.enemySpawnCycle - 1);

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
        bossScore += 400; // 提高下一次召唤的门槛
        onBossSpawned();
    }

    @Override
    protected void resetDifficultyState() {
        currentBossHp = 300;
        timeTick = 0;
        difficultyLevel = 1;
        initDifficultyParams();
    }
}

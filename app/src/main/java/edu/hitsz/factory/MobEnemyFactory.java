package edu.hitsz.factory;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.aircraft.MobEnemy;
import edu.hitsz.application.GameConfig;
import edu.hitsz.application.ImageManager;

public class MobEnemyFactory implements EnemyFactory {
    @Override
    public AbstractAircraft createEnemy(){
        int locationX = (int) (Math.random() * (GameConfig.WINDOW_WIDTH - ImageManager.MOB_ENEMY_IMAGE.getWidth()));
        int locationY = (int) (Math.random() * GameConfig.WINDOW_HEIGHT * 0.05);
        int speedX = 0;
        int speedY = (int) (10 * AbstractAircraft.getSpeedMultiplier());
        int hp = (int) (30 * AbstractAircraft.getHpMultiplier());
        int power = 0;
        return new MobEnemy(locationX,locationY,speedX,speedY,hp,power);
    }

    @Override
    public AbstractAircraft createEnemy(int customHp){
        int locationX = (int) (Math.random() * (GameConfig.WINDOW_WIDTH - ImageManager.MOB_ENEMY_IMAGE.getWidth()));
        int locationY = (int) (Math.random() * GameConfig.WINDOW_HEIGHT * 0.05);
        int speedX = 0;
        int speedY = (int) (10 * AbstractAircraft.getSpeedMultiplier());
        int hp = customHp;
        int power = 0;
        return new MobEnemy(locationX,locationY,speedX,speedY,hp,power);
    }
}

package edu.hitsz.factory;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.aircraft.EliteEnemy;
import edu.hitsz.application.GameConfig;
import edu.hitsz.application.ImageManager;

public class EliteEnemyFactory implements EnemyFactory{
    @Override
    public AbstractAircraft createEnemy(){
        int locationX = (int) (Math.random() * (GameConfig.WINDOW_WIDTH - ImageManager.ELITE_ENEMY_IMAGE.getWidth()));
        int locationY = (int) (Math.random() * GameConfig.WINDOW_HEIGHT * 0.05);
        int speedX = 0;
        int speedY = (int) (6 * AbstractAircraft.getSpeedMultiplier());
        int hp = (int) (60 * AbstractAircraft.getHpMultiplier());
        int power = 5;
        return new EliteEnemy(locationX,locationY,speedX,speedY,hp,power);
    }

    @Override
    public AbstractAircraft createEnemy(int customHp){
        int locationX = (int) (Math.random() * (GameConfig.WINDOW_WIDTH - ImageManager.ELITE_ENEMY_IMAGE.getWidth()));
        int locationY = (int) (Math.random() * GameConfig.WINDOW_HEIGHT * 0.05);
        int speedX = 0;
        int speedY = (int) (8 * AbstractAircraft.getSpeedMultiplier());
        int hp = (int) (customHp * AbstractAircraft.getHpMultiplier());
        int power = 5;
        return new EliteEnemy(locationX,locationY,speedX,speedY,hp,power);
    }
}

package edu.hitsz.factory;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.aircraft.ElitePlusEnemy;
import edu.hitsz.application.GameConfig;
import edu.hitsz.application.ImageManager;

public class ElitePlusEnemyFactory implements EnemyFactory{
    @Override
    public AbstractAircraft createEnemy(){
        int locationX = (int) (Math.random() * (GameConfig.WINDOW_WIDTH - ImageManager.ELITEPLUS_ENEMY_IMAGE.getWidth()));
        int locationY = (int) (Math.random() * GameConfig.WINDOW_HEIGHT * 0.05);
        int speedX = (int) ((Math.random() < 0.5 ? 2 : -2) * AbstractAircraft.getSpeedMultiplier());
        int speedY = (int) (6 * AbstractAircraft.getSpeedMultiplier());
        int hp = (int) (80 * AbstractAircraft.getHpMultiplier());
        int power = 5;
        return new ElitePlusEnemy(locationX,locationY,speedX,speedY,hp,power);
    }

    @Override
    public AbstractAircraft createEnemy(int customHp){
        int locationX = (int) (Math.random() * (GameConfig.WINDOW_WIDTH - ImageManager.ELITEPLUS_ENEMY_IMAGE.getWidth()));
        int locationY = (int) (Math.random() * GameConfig.WINDOW_HEIGHT * 0.05);
        int speedX = (int) ((Math.random() < 0.5 ? 2 : -2) * AbstractAircraft.getSpeedMultiplier());
        int speedY = (int) (7 * AbstractAircraft.getSpeedMultiplier());
        int hp = (int) (customHp * AbstractAircraft.getHpMultiplier());
        int power = 5;
        return new ElitePlusEnemy(locationX,locationY,speedX,speedY,hp,power);
    }
}

package edu.hitsz.factory;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.aircraft.BossEnemy;
import edu.hitsz.application.GameConfig;

public class BossEnemyFactory implements EnemyFactory{
    @Override
    public AbstractAircraft createEnemy(){
        int locationX = (int) GameConfig.WINDOW_WIDTH / 2;
        int locationY = 80;
        int speedX = (int) ((Math.random() <= 0.5? 1:-1) * AbstractAircraft.getSpeedMultiplier());
        int speedY = 0;
        int hp = (int) (350 * AbstractAircraft.getHpMultiplier());
        int power = 20;
        return new BossEnemy(locationX,locationY,speedX,speedY,hp,power);
    }

    @Override
    public AbstractAircraft createEnemy(int customHp){
        int locationX = (int) GameConfig.WINDOW_WIDTH / 2;
        int locationY = 80;
        int speedX = (int) ((Math.random() <= 0.5? 1:-1) * AbstractAircraft.getSpeedMultiplier());
        int speedY = 0;
        int hp = (int) (customHp * AbstractAircraft.getHpMultiplier());
        int power = 20;
        return new BossEnemy(locationX,locationY,speedX,speedY,hp,power);
    }
}

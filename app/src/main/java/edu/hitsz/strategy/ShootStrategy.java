package edu.hitsz.strategy;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.EnemyBullet;
import edu.hitsz.bullet.HeroBullet;

import java.util.ArrayList;
import java.util.List;

public interface ShootStrategy {
    void shootInto(AbstractAircraft aircraft, List<BaseBullet> out);

    default List<BaseBullet> shoot(AbstractAircraft aircraft) {
        List<BaseBullet> out = new ArrayList<>();
        shootInto(aircraft, out);
        return out;
    }
    default BaseBullet createBullet(AbstractAircraft aircraft, int x, int y, int speedX, int speedY, int power){
        int actualPower = power > 0 ? power : aircraft.getPower();
        if(aircraft instanceof HeroAircraft){
            return new HeroBullet(x,y,speedX,speedY,actualPower);
        }
        else{
            return new EnemyBullet(x,y,speedX,speedY,actualPower);
        }
    }
}

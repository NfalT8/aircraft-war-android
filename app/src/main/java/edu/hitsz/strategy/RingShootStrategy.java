package edu.hitsz.strategy;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.bullet.BaseBullet;

import java.util.List;

public class RingShootStrategy implements ShootStrategy{
    @Override
    public void shootInto(AbstractAircraft aircraft, List<BaseBullet> out){
        boolean isHero = aircraft instanceof HeroAircraft;
        int x = aircraft.getLocationX();
        int y = aircraft.getLocationY();
        int cpower = aircraft.getPower();
        int ringPower = Math.max(1, Math.round(cpower * (isHero ? 0.6f : 0.4f)));
        int bulletNum = 20;
        int speed = isHero ? 15 : 8;
        for(int i = 0; i < bulletNum; i++){
            double angle = i * 2 * Math.PI / bulletNum;
            int speedX = (int) Math.round(speed * Math.cos(angle));
            int speedY = (int) Math.round(speed * Math.sin(angle));

            out.add(createBullet(aircraft,x,y,speedX,speedY,ringPower));
        }
    }
}

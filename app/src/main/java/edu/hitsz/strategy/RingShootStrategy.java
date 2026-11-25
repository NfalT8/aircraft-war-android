package edu.hitsz.strategy;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.bullet.BaseBullet;

import java.util.LinkedList;
import java.util.List;

public class RingShootStrategy implements ShootStrategy{
    @Override
    public List<BaseBullet> shoot(AbstractAircraft aircraft){
        List<BaseBullet> res = new LinkedList<>();
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

            res.add(createBullet(aircraft,x,y,speedX,speedY,ringPower));
        }
        return res;
    }
}

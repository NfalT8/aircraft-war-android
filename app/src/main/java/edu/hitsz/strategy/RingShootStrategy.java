package edu.hitsz.strategy;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.bullet.BaseBullet;

import java.util.LinkedList;
import java.util.List;

public class RingShootStrategy implements ShootStrategy{
    public List<BaseBullet> shoot(AbstractAircraft aircraft){
        List<BaseBullet> res = new LinkedList<>();
        int x = aircraft.getLocationX();
        int y = aircraft.getLocationY();
        int cpower = aircraft.getPower();
        int RingPower = (int) (cpower * 0.4);
        int bulletNum = 20;
        int speed = 8;
        for(int i = 0; i < bulletNum; i++){
            double angle = i * 2 * Math.PI / bulletNum;
            int speedX = (int) (speed * Math.cos(angle));
            int speedY = (int) (speed * Math.sin(angle));

            res.add(createBullet(aircraft,x,y,speedX,speedY,RingPower));
        }
        return res;
    }
}

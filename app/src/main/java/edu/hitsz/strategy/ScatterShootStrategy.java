package edu.hitsz.strategy;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.EnemyBullet;

import java.util.LinkedList;
import java.util.List;

public class ScatterShootStrategy implements ShootStrategy{
    @Override
    public List<BaseBullet> shoot(AbstractAircraft aircraft) {
        List<BaseBullet> res = new LinkedList<>();
        int cpower = aircraft.getPower();
        int ScatterPower = (int) (cpower * 0.7);
        int direction = aircraft instanceof HeroAircraft? -1:1;
        int x = aircraft.getLocationX();
        int y = aircraft.getLocationY() + direction * 2;

        int speedY = aircraft.getSpeedY() + direction * 5;

        res.add(createBullet(aircraft, x, y, -2, speedY, ScatterPower));
        res.add(createBullet(aircraft, x, y, 0, speedY, ScatterPower));
        res.add(createBullet(aircraft, x, y, 2, speedY, ScatterPower));

        return res;
    }
}

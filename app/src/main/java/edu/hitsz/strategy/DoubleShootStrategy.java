package edu.hitsz.strategy;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.EnemyBullet;

import java.util.LinkedList;
import java.util.List;

public class DoubleShootStrategy implements ShootStrategy{
    @Override
    public List<BaseBullet> shoot(AbstractAircraft aircraft){
        List<BaseBullet> res = new LinkedList<>();
        boolean isHero = aircraft instanceof HeroAircraft;
        int cpower = aircraft.getPower();
        int direction = isHero ? -1 : 1;

        int speedX = 0;
        int speedY = isHero ? -20 : aircraft.getSpeedY() + direction * 5;

        int offset = 15;
        int x1 = aircraft.getLocationX() - offset;
        int x2 = aircraft.getLocationX() + offset;
        int y = isHero
                ? aircraft.getLocationY() - aircraft.getHeight() / 2 + 10
                : aircraft.getLocationY() + direction * 2;
        res.add(createBullet(aircraft, x1, y, speedX, speedY, cpower));
        res.add(createBullet(aircraft, x2, y, speedX, speedY, cpower));

        return res;
    }
}

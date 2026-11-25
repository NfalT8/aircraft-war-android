package edu.hitsz.strategy;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.HeroBullet;

import java.util.LinkedList;
import java.util.List;

public class DirectShootStrategy implements ShootStrategy{
    @Override
    public List<BaseBullet> shoot(AbstractAircraft aircraft){
        List<BaseBullet> res = new LinkedList<>();
        boolean isHero = aircraft instanceof HeroAircraft;
        int direction = isHero ? -1 : 1;
        int x = aircraft.getLocationX();
        int y = isHero
                ? aircraft.getLocationY() - aircraft.getHeight() / 2 + 6
                : aircraft.getLocationY() + direction * 2;
        int speedX = 0;
        int speedY = isHero ? -20 : aircraft.getSpeedY() + direction * 5;
        int cpower = aircraft.getPower();

        res.add(createBullet(aircraft, x, y, speedX, speedY, cpower));
        return res;
    }
}

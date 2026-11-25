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
        boolean isHero = aircraft instanceof HeroAircraft;
        int cpower = aircraft.getPower();
        int scatterPower = Math.max(1, Math.round(cpower * 0.75f));
        int direction = isHero ? -1 : 1;
        int x = aircraft.getLocationX();
        int y = isHero
                ? aircraft.getLocationY() - aircraft.getHeight() / 2 + 8
                : aircraft.getLocationY() + direction * 2;

        int speedY = isHero ? -20 : aircraft.getSpeedY() + direction * 5;
        int spreadX = isHero ? 4 : 2;

        res.add(createBullet(aircraft, x - (isHero ? 8 : 0), y, -spreadX, speedY, scatterPower));
        res.add(createBullet(aircraft, x, y, 0, speedY, scatterPower));
        res.add(createBullet(aircraft, x + (isHero ? 8 : 0), y, spreadX, speedY, scatterPower));

        return res;
    }
}

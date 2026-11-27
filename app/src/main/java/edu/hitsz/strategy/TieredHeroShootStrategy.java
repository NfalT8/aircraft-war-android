package edu.hitsz.strategy;

import java.util.List;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.bullet.BaseBullet;

/**
 * 英雄机 5 阶常驻火力策略（Lv.1 ~ Lv.5 MAX）：
 * - Lv.1 单发速射（1.0x DPS）
 * - Lv.2 双联并发（1.5x DPS）
 * - Lv.3 三向锋矢（1.9x DPS）
 * - Lv.4 四联破甲（2.3x DPS）
 * - Lv.5 五联超载（2.8x DPS）
 */
public class TieredHeroShootStrategy implements ShootStrategy {

    @Override
    public void shootInto(AbstractAircraft aircraft, List<BaseBullet> out) {
        int level = (aircraft instanceof HeroAircraft)
                ? ((HeroAircraft) aircraft).getFirepowerLevel()
                : HeroAircraft.MIN_FIREPOWER_LEVEL;

        int basePower = Math.max(1, aircraft.getPower());
        int x = aircraft.getLocationX();
        int noseY = aircraft.getLocationY() - aircraft.getHeight() / 2 + 6;

        switch (level) {
            case 2: {
                int p = Math.max(1, Math.round(basePower * 0.75f));
                out.add(createBullet(aircraft, x - 12, noseY + 4, 0, -20, p));
                out.add(createBullet(aircraft, x + 12, noseY + 4, 0, -20, p));
                break;
            }
            case 3: {
                int centerP = Math.max(1, Math.round(basePower * 0.80f));
                int sideP = Math.max(1, Math.round(basePower * 0.55f));
                out.add(createBullet(aircraft, x, noseY, 0, -20, centerP));
                out.add(createBullet(aircraft, x - 14, noseY + 6, -3, -20, sideP));
                out.add(createBullet(aircraft, x + 14, noseY + 6, 3, -20, sideP));
                break;
            }
            case 4: {
                int innerP = Math.max(1, Math.round(basePower * 0.65f));
                int outerP = Math.max(1, Math.round(basePower * 0.50f));
                out.add(createBullet(aircraft, x - 10, noseY + 2, 0, -20, innerP));
                out.add(createBullet(aircraft, x + 10, noseY + 2, 0, -20, innerP));
                out.add(createBullet(aircraft, x - 20, noseY + 10, -4, -19, outerP));
                out.add(createBullet(aircraft, x + 20, noseY + 10, 4, -19, outerP));
                break;
            }
            case 5: {
                int centerP = Math.max(1, Math.round(basePower * 0.70f));
                int innerP = Math.max(1, Math.round(basePower * 0.55f));
                int outerP = Math.max(1, Math.round(basePower * 0.50f));
                out.add(createBullet(aircraft, x, noseY, 0, -21, centerP));
                out.add(createBullet(aircraft, x - 12, noseY + 4, -1, -21, innerP));
                out.add(createBullet(aircraft, x + 12, noseY + 4, 1, -21, innerP));
                out.add(createBullet(aircraft, x - 22, noseY + 10, -4, -20, outerP));
                out.add(createBullet(aircraft, x + 22, noseY + 10, 4, -20, outerP));
                break;
            }
            case 1:
            default: {
                out.add(createBullet(aircraft, x, noseY, 0, -20, basePower));
                break;
            }
        }
    }
}

package edu.hitsz.aircraft;

import edu.hitsz.application.GameConfig;
import edu.hitsz.application.ImageManager;
import edu.hitsz.strategy.DirectShootStrategy;

/**
 * 英雄飞机，游戏玩家操控
 * @author hitsz
 */
public class HeroAircraft extends AbstractAircraft {
    private static volatile HeroAircraft instance = null;
    private volatile long effectEndTime = 0;

    public long getEffectEndTime() {
        return effectEndTime;
    }

    public void setEffectEndTime(long effectEndTime) {
        this.effectEndTime = effectEndTime;
    }

    private HeroAircraft(int locationX, int locationY, int speedX, int speedY, int hp, int power) {
        super(locationX, locationY, speedX, speedY, hp, power);
        this.strategy = new DirectShootStrategy();
    }

    public void reset() {
        this.hp = 100;
        this.maxHp = 100;
        this.energy = 0;
        this.effectEndTime = 0;
        this.isValid = true;
        this.strategy = new DirectShootStrategy();
        this.locationX = GameConfig.WINDOW_WIDTH / 2;
        this.locationY = GameConfig.WINDOW_HEIGHT - ImageManager.HERO_IMAGE.getHeight();
    }

    private int energy = 0;
    private int maxEnergy = 100;

    public void addEnergy(int value) {
        energy = Math.min(energy + value, maxEnergy);
    }

    public void resetEnergy() {
        energy = 0;
    }

    public boolean isEnergyFull() {
        return energy >= maxEnergy;
    }

    public int getEnergy() {
        return energy;
    }

    public static HeroAircraft getInstance(){
        if (instance == null) {
            synchronized (HeroAircraft.class){
                if (instance == null) {
                    int initX = GameConfig.WINDOW_WIDTH / 2;
                    int initY = GameConfig.WINDOW_HEIGHT - ImageManager.HERO_IMAGE.getHeight();
                    int initspeedX = 0;
                    int initspeedY = 0;
                    int inithp = 100;
                    int initpower = 40;
                    instance = new HeroAircraft(initX,initY,initspeedX,initspeedY,inithp, initpower);
                }
            }
        }
        return instance;
    }


    //每次射击发射子弹数量
    private int shootNum = 1;

    //子弹威力
    private int power = 30;

    //子弹射击方向 (向上发射：-1，向下发射：1)
    private int direction = -1;



    @Override
    public void forward() {
        // 英雄机由触屏控制，不通过forward函数移动
    }

}

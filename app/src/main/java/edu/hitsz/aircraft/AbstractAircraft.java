package edu.hitsz.aircraft;

import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.basic.AbstractFlyingObject;
import edu.hitsz.strategy.ShootStrategy;

import java.util.LinkedList;
import java.util.List;

/**
 * 所有种类飞机的抽象父类
 * @author hitsz
 */
public abstract class AbstractAircraft extends AbstractFlyingObject {

    //难度系数倍率（静态变量，所有敌机共享）
    protected static double hpMultiplier = 1.0;   // 血量倍率
    protected static double speedMultiplier = 1.0; // 速度倍率

    //最大生命值
    protected int maxHp;
    protected int hp;
    protected int power;
    protected boolean destroyedByBomb = false; // 默认不是被炸弹炸死的

    public boolean isDestroyedByBomb() {
        return destroyedByBomb;
    }

    public void setDestroyedByBomb(boolean destroyedByBomb) {
        this.destroyedByBomb = destroyedByBomb;
    }

    public AbstractAircraft(int locationX, int locationY, int speedX, int speedY, int hp, int power) {
        super(locationX, locationY, speedX, speedY);
        this.hp = hp;
        this.power = power;
        this.maxHp = hp;
    }

    public int getPower(){
        return this.power;
    }

    public void decreaseHp(int decrease){
        this.hp -= decrease;
        if(this.hp <= 0){
            this.hp=0;
            vanish();
        }
    }

    public void increaseHp(int increase){
        this.hp += increase;
    }

    public int getHp() {
        return hp;
    }

    public static void setHpMultiplier(double multiplier) {
        hpMultiplier = multiplier;
    }

    public static double getHpMultiplier() {
        return hpMultiplier;
    }

    public static void setSpeedMultiplier(double multiplier) {
        speedMultiplier = multiplier;
    }

    public static double getSpeedMultiplier() {
        return speedMultiplier;
    }

    protected ShootStrategy strategy;

    public void setStrategy(ShootStrategy strategy){
        this.strategy = strategy;
    }
    /**
     * 飞机射击方法
     * @return
     *  可射击对象需实现，返回子弹列表
     *  非可射击对象空实现，返回空列表
     */
    public List<BaseBullet> shoot(){
        if(this.strategy == null){
            return new LinkedList<>();
        }
        return this.strategy.shoot(this);
    }

}
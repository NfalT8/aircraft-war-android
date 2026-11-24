package edu.hitsz.factory;

import edu.hitsz.prop.*;

public class PropFactory {
    public static AbstractProp createProp(String type, int x, int y){
        int speedX = 0;
        int speedY = 4;
        switch(type){
            case "Blood":
                return new BloodProp(x, y, speedX, speedY);
            case "Bullet":
                return new BulletProp(x,y,speedX,speedY);
            case "BulletPlus":
                return new BulletPlusProp(x,y,speedX,speedY);
            case "Bomb":
                return new BombProp(x,y,speedX,speedY);
            case "Freeze":
                return new FreezeProp(x,y,speedX,speedY);
            default:
                return null;
        }
    }
}

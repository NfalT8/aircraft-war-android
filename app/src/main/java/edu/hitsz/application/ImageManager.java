package edu.hitsz.application;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import edu.hitsz.R;
import edu.hitsz.aircraft.BossEnemy;
import edu.hitsz.aircraft.EliteEnemy;
import edu.hitsz.aircraft.ElitePlusEnemy;
import edu.hitsz.aircraft.EliteProEnemy;
import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.aircraft.MobEnemy;
import edu.hitsz.bullet.EnemyBullet;
import edu.hitsz.bullet.HeroBullet;
import edu.hitsz.prop.BloodProp;
import edu.hitsz.prop.BombProp;
import edu.hitsz.prop.BulletPlusProp;
import edu.hitsz.prop.BulletProp;
import edu.hitsz.prop.FreezeProp;

import java.util.HashMap;
import java.util.Map;

/**
 * 综合管理图片的加载，访问
 * 提供图片的静态访问方法
 * @author hitsz
 */
public class ImageManager {

    private static final Map<String, Bitmap> CLASSNAME_IMAGE_MAP = new HashMap<>();

    public static Bitmap HERO_IMAGE;
    public static Bitmap HERO_BULLET_IMAGE;
    public static Bitmap ENEMY_BULLET_IMAGE;
    public static Bitmap MOB_ENEMY_IMAGE;
    public static Bitmap ELITE_ENEMY_IMAGE;
    public static Bitmap ELITEPLUS_ENEMY_IMAGE;
    public static Bitmap ELITEPRO_ENEMY_IMAGE;
    public static Bitmap BLOOD_PROP_IMAGE;
    public static Bitmap BOMB_PROP_IMAGE;
    public static Bitmap BULLET_PROP_IMAGE;
    public static Bitmap FREEZE_PROP_IMAGE;
    public static Bitmap BULLETPLUS_PROP_IMAGE;
    public static Bitmap BOSS_IMAGE;

    public static void init(Context context) {
        if (HERO_IMAGE != null) {
            return;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        // 防止系统按屏幕密度放大图片，保证逻辑坐标与素材像素一致
        options.inScaled = false;

        HERO_IMAGE = decode(context, R.drawable.hero, options);
        MOB_ENEMY_IMAGE = decode(context, R.drawable.mob, options);
        ELITE_ENEMY_IMAGE = decode(context, R.drawable.elite, options);
        HERO_BULLET_IMAGE = decode(context, R.drawable.bullet_hero, options);
        ENEMY_BULLET_IMAGE = decode(context, R.drawable.bullet_enemy, options);
        BOSS_IMAGE = decode(context, R.drawable.boss, options);
        ELITEPLUS_ENEMY_IMAGE = decode(context, R.drawable.eliteplus, options);
        ELITEPRO_ENEMY_IMAGE = decode(context, R.drawable.elitepro, options);
        BLOOD_PROP_IMAGE = decode(context, R.drawable.prop_blood, options);
        BOMB_PROP_IMAGE = decode(context, R.drawable.prop_bomb, options);
        BULLET_PROP_IMAGE = decode(context, R.drawable.prop_bullet, options);
        BULLETPLUS_PROP_IMAGE = decode(context, R.drawable.prop_bulletplus, options);
        FREEZE_PROP_IMAGE = decode(context, R.drawable.prop_freeze, options);

        CLASSNAME_IMAGE_MAP.put(HeroAircraft.class.getName(), HERO_IMAGE);
        CLASSNAME_IMAGE_MAP.put(MobEnemy.class.getName(), MOB_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(HeroBullet.class.getName(), HERO_BULLET_IMAGE);
        CLASSNAME_IMAGE_MAP.put(EnemyBullet.class.getName(), ENEMY_BULLET_IMAGE);
        CLASSNAME_IMAGE_MAP.put(EliteEnemy.class.getName(), ELITE_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(ElitePlusEnemy.class.getName(), ELITEPLUS_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(EliteProEnemy.class.getName(), ELITEPRO_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(BossEnemy.class.getName(), BOSS_IMAGE);
        CLASSNAME_IMAGE_MAP.put(BloodProp.class.getName(), BLOOD_PROP_IMAGE);
        CLASSNAME_IMAGE_MAP.put(BombProp.class.getName(), BOMB_PROP_IMAGE);
        CLASSNAME_IMAGE_MAP.put(BulletProp.class.getName(), BULLET_PROP_IMAGE);
        CLASSNAME_IMAGE_MAP.put(BulletPlusProp.class.getName(), BULLETPLUS_PROP_IMAGE);
        CLASSNAME_IMAGE_MAP.put(FreezeProp.class.getName(), FREEZE_PROP_IMAGE);
    }

    private static Bitmap decode(Context context, int resId, BitmapFactory.Options options) {
        return BitmapFactory.decodeResource(context.getResources(), resId, options);
    }

    public static Bitmap get(String className) {
        return CLASSNAME_IMAGE_MAP.get(className);
    }

    public static Bitmap get(Object obj) {
        if (obj == null) {
            return null;
        }
        return get(obj.getClass().getName());
    }
}

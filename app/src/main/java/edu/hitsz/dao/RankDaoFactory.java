package edu.hitsz.dao;

import android.content.Context;

/**
 * 排行榜 DAO 工厂：集中控制底层存储实现，当前默认采用 SQLite 数据库存储。
 */
public final class RankDaoFactory {

    private RankDaoFactory() {
    }

    public static RankDao create(Context context) {
        return new SqliteRankDaoImpl(context.getApplicationContext());
    }
}

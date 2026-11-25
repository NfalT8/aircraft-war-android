package edu.hitsz.dao;

import java.util.List;

import edu.hitsz.application.GameDifficulty;

/**
 * 排行榜数据访问接口：统一文件存储与 SQLite 数据库存储的访问契约。
 */
public interface RankDao {

    /**
     * 新增一条排行榜记录，返回新记录 ID（失败返回 -1）。
     */
    long insert(RankRecord record);

    /**
     * 批量新增排行榜记录。
     */
    void insertAll(List<RankRecord> records);

    /**
     * 查询指定难度的排行榜记录，按分数降序排列。
     */
    List<RankRecord> findByDifficulty(GameDifficulty difficulty);

    /**
     * 查询全部排行榜记录，按分数降序排列。
     */
    List<RankRecord> findAll();

    /**
     * 更新一条排行榜记录，返回受影响行数。
     */
    int update(RankRecord record);

    /**
     * 根据 ID 删除一条排行榜记录，返回受影响行数。
     */
    int deleteById(long id);
}

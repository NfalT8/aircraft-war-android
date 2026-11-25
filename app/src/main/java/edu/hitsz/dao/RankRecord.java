package edu.hitsz.dao;

import edu.hitsz.application.GameDifficulty;

/**
 * 排行榜记录实体类。
 */
public class RankRecord {

    private long id;
    private String name;
    private int score;
    private String time;
    private GameDifficulty difficulty;

    public RankRecord(long id, String name, int score, String time, GameDifficulty difficulty) {
        this.id = id;
        this.name = name;
        this.score = score;
        this.time = time;
        this.difficulty = difficulty;
    }

    public RankRecord(String name, int score, String time, GameDifficulty difficulty) {
        this(-1L, name, score, time, difficulty);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public GameDifficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(GameDifficulty difficulty) {
        this.difficulty = difficulty;
    }
}

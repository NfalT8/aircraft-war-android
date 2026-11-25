package edu.hitsz.dao;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import edu.hitsz.application.GameDifficulty;

/**
 * 基于应用沙箱私有文件的排行榜 DAO 实现（通过 context.getFilesDir() 存储）。
 * 与 {@link SqliteRankDaoImpl} 实现同一套 {@link RankDao} 接口，便于按需替换。
 */
public class FileRankDaoImpl implements RankDao {

    private static final String FILE_NAME = "rank_records.txt";
    private static final String DELIMITER = "\t";

    private final File storageFile;

    public FileRankDaoImpl(Context context) {
        this.storageFile = new File(context.getApplicationContext().getFilesDir(), FILE_NAME);
    }

    @Override
    public synchronized long insert(RankRecord record) {
        List<RankRecord> all = readAllInternal();
        long nextId = nextId(all);
        record.setId(nextId);
        all.add(record);
        writeAllInternal(all);
        return nextId;
    }

    @Override
    public synchronized void insertAll(List<RankRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<RankRecord> all = readAllInternal();
        long nextId = nextId(all);
        for (RankRecord record : records) {
            record.setId(nextId++);
            all.add(record);
        }
        writeAllInternal(all);
    }

    @Override
    public synchronized List<RankRecord> findByDifficulty(GameDifficulty difficulty) {
        GameDifficulty target = (difficulty != null) ? difficulty : GameDifficulty.NORMAL;
        List<RankRecord> filtered = new ArrayList<>();
        for (RankRecord record : readAllInternal()) {
            if (record.getDifficulty() == target) {
                filtered.add(record);
            }
        }
        sortByScoreDesc(filtered);
        return filtered;
    }

    @Override
    public synchronized List<RankRecord> findAll() {
        List<RankRecord> all = readAllInternal();
        sortByScoreDesc(all);
        return all;
    }

    @Override
    public synchronized int update(RankRecord record) {
        List<RankRecord> all = readAllInternal();
        int updated = 0;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId() == record.getId()) {
                all.set(i, record);
                updated = 1;
                break;
            }
        }
        if (updated > 0) {
            writeAllInternal(all);
        }
        return updated;
    }

    @Override
    public synchronized int deleteById(long id) {
        List<RankRecord> all = readAllInternal();
        int deleted = 0;
        Iterator<RankRecord> iterator = all.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getId() == id) {
                iterator.remove();
                deleted++;
            }
        }
        if (deleted > 0) {
            writeAllInternal(all);
        }
        return deleted;
    }

    private long nextId(List<RankRecord> all) {
        long maxId = 0L;
        for (RankRecord record : all) {
            if (record.getId() > maxId) {
                maxId = record.getId();
            }
        }
        return maxId + 1L;
    }

    private void sortByScoreDesc(List<RankRecord> list) {
        list.sort(Comparator.comparingInt(RankRecord::getScore).reversed()
                .thenComparingLong(RankRecord::getId));
    }

    private List<RankRecord> readAllInternal() {
        List<RankRecord> records = new ArrayList<>();
        if (!storageFile.exists()) {
            return records;
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(storageFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(DELIMITER, -1);
                if (parts.length < 5) {
                    continue;
                }
                try {
                    long id = Long.parseLong(parts[0]);
                    String name = parts[1];
                    int score = Integer.parseInt(parts[2]);
                    String time = parts[3];
                    GameDifficulty difficulty = GameDifficulty.valueOf(parts[4]);
                    records.add(new RankRecord(id, name, score, time, difficulty));
                } catch (IllegalArgumentException ignored) {
                    // 跳过格式异常行
                }
            }
        } catch (IOException ignored) {
            // 读取失败时返回已解析记录
        }
        return records;
    }

    private void writeAllInternal(List<RankRecord> records) {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(storageFile, false), StandardCharsets.UTF_8))) {
            for (RankRecord record : records) {
                String safeName = record.getName().replace(DELIMITER, " ").replace("\n", " ");
                GameDifficulty difficulty = record.getDifficulty() != null
                        ? record.getDifficulty()
                        : GameDifficulty.NORMAL;
                writer.write(record.getId() + DELIMITER
                        + safeName + DELIMITER
                        + record.getScore() + DELIMITER
                        + record.getTime() + DELIMITER
                        + difficulty.name());
                writer.newLine();
            }
        } catch (IOException ignored) {
            // 写入失败时忽略
        }
    }
}

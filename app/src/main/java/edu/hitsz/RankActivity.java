package edu.hitsz;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import edu.hitsz.application.GameDifficulty;
import edu.hitsz.dao.RankDao;
import edu.hitsz.dao.RankDaoFactory;
import edu.hitsz.dao.RankRecord;
import edu.hitsz.ui.RankAdapter;

/**
 * 排行榜页面：展示按难度分榜的成绩记录，支持新增与删除，数据通过 RankDao 持久化到 SQLite。
 */
public class RankActivity extends AppCompatActivity {

    public static final String EXTRA_DIFFICULTY = "extra_difficulty";

    private static final int TAB_INACTIVE_BG = 0x80152238;
    private static final int TAB_INACTIVE_TEXT = 0xFFAFC6E8;

    private RankDao rankDao;
    private RankAdapter rankAdapter;
    private GameDifficulty currentDifficulty = GameDifficulty.NORMAL;

    private MaterialButton tabEasy;
    private MaterialButton tabNormal;
    private MaterialButton tabHard;
    private View emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);

        setContentView(R.layout.activity_rank);
        applySystemBarInsets();

        rankDao = RankDaoFactory.create(this);
        currentDifficulty = readInitialDifficulty();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        emptyView = findViewById(R.id.emptyView);

        setupDifficultyTabs();
        setupRankList();
        findViewById(R.id.btnAddRecord).setOnClickListener(v -> showAddRecordDialog());

        selectDifficulty(currentDifficulty);
    }

    private GameDifficulty readInitialDifficulty() {
        String name = getIntent().getStringExtra(EXTRA_DIFFICULTY);
        if (name != null) {
            try {
                return GameDifficulty.valueOf(name);
            } catch (IllegalArgumentException ignored) {
                // 使用默认难度
            }
        }
        return GameDifficulty.NORMAL;
    }

    private void applySystemBarInsets() {
        View container = findViewById(R.id.rankContentContainer);
        final int baseLeft = container.getPaddingLeft();
        final int baseTop = container.getPaddingTop();
        final int baseRight = container.getPaddingRight();
        final int baseBottom = container.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(container, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(
                    baseLeft + bars.left,
                    baseTop + bars.top,
                    baseRight + bars.right,
                    baseBottom + bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(container);
    }

    private void setupDifficultyTabs() {
        tabEasy = findViewById(R.id.tabEasy);
        tabNormal = findViewById(R.id.tabNormal);
        tabHard = findViewById(R.id.tabHard);

        tabEasy.setOnClickListener(v -> selectDifficulty(GameDifficulty.EASY));
        tabNormal.setOnClickListener(v -> selectDifficulty(GameDifficulty.NORMAL));
        tabHard.setOnClickListener(v -> selectDifficulty(GameDifficulty.HARD));
    }

    private void setupRankList() {
        ListView lvRank = findViewById(R.id.lv_rank);
        rankAdapter = new RankAdapter(this, new ArrayList<>(), this::confirmDeleteRecord);
        lvRank.setAdapter(rankAdapter);
    }

    private void selectDifficulty(GameDifficulty difficulty) {
        currentDifficulty = difficulty;
        updateTabStyle(tabEasy, difficulty == GameDifficulty.EASY, 0xFF4CD964, 0xFF06280F);
        updateTabStyle(tabNormal, difficulty == GameDifficulty.NORMAL, 0xFF3D9BF5, 0xFFFFFFFF);
        updateTabStyle(tabHard, difficulty == GameDifficulty.HARD, 0xFFF05252, 0xFFFFFFFF);
        reloadRecords();
    }

    private void updateTabStyle(MaterialButton tab, boolean selected, int activeBg, int activeText) {
        tab.setBackgroundTintList(ColorStateList.valueOf(selected ? activeBg : TAB_INACTIVE_BG));
        tab.setTextColor(selected ? activeText : TAB_INACTIVE_TEXT);
    }

    private void reloadRecords() {
        List<RankRecord> records = rankDao.findByDifficulty(currentDifficulty);
        rankAdapter.setRecords(records);
        updateEmptyState();
    }

    private void updateEmptyState() {
        emptyView.setVisibility(rankAdapter.getCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void confirmDeleteRecord(RankRecord record, int position) {
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_AircraftWar_DarkDialog)
                .setTitle("删除记录")
                .setMessage("确定要删除「" + record.getName() + "」（" + record.getScore() + " 分）的排行记录吗？")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) -> {
                    int deleted = rankDao.deleteById(record.getId());
                    if (deleted > 0) {
                        rankAdapter.removeAt(position);
                        updateEmptyState();
                        Toast.makeText(this, "已删除该记录", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void showAddRecordDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_rank, null);
        EditText etName = dialogView.findViewById(R.id.etDialogName);
        EditText etScore = dialogView.findViewById(R.id.etDialogScore);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_AircraftWar_DarkDialog)
                .setTitle("添加记录（" + currentDifficulty.getDisplayName() + "）")
                .setView(dialogView)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String scoreStr = etScore.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                etName.setError("请输入玩家姓名");
                return;
            }
            if (TextUtils.isEmpty(scoreStr)) {
                etScore.setError("请输入得分");
                return;
            }
            int score;
            try {
                score = Integer.parseInt(scoreStr);
            } catch (NumberFormatException e) {
                etScore.setError("分数格式不正确");
                return;
            }
            if (score < 0) {
                etScore.setError("分数不能为负数");
                return;
            }

            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
            rankDao.insert(new RankRecord(name, score, now, currentDifficulty));
            reloadRecords();
            dialog.dismiss();
            Toast.makeText(this, "已添加到" + currentDifficulty.getDisplayName() + "排行榜", Toast.LENGTH_SHORT).show();
        }));

        dialog.show();
    }
}

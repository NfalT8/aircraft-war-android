package edu.hitsz;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import edu.hitsz.application.BaseGame;
import edu.hitsz.application.EasyGame;
import edu.hitsz.application.GameDifficulty;
import edu.hitsz.application.HardGame;
import edu.hitsz.application.ImageManager;
import edu.hitsz.application.NormalGame;
import edu.hitsz.dao.RankDao;
import edu.hitsz.dao.RankDaoFactory;
import edu.hitsz.dao.RankRecord;
import edu.hitsz.ui.BossHealthBarView;
import edu.hitsz.ui.BuffCountdownView;

/**
 * 游戏主界面：根据 Intent 中携带的难度创建对应的游戏场景。
 */
public class GameActivity extends AppCompatActivity {

    public static final String EXTRA_DIFFICULTY = "extra_difficulty";
    private static final String PREFS_PLAYER = "aircraft_player_prefs";
    private static final String KEY_LAST_PLAYER_NAME = "last_player_name";
    private static final String DEFAULT_PLAYER_NAME = "匿名飞行员";

    private BaseGame game;
    private GameDifficulty currentDifficulty;
    private RankDao rankDao;
    private ImageButton btnBomb;
    private ImageButton btnHeal;
    private ImageButton btnRing;
    private View skillContainer;
    private View gameOverOverlay;
    private EditText etPlayerName;
    private MaterialButton btnSaveAndRank;
    private BuffCountdownView buffCountdownView;
    private BossHealthBarView bossHealthBar;

    private boolean musicEnabled = true;
    private int lastFinalScore = 0;
    private boolean scoreSavedForRound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImageManager.init(this);
        enterImmersiveMode();

        setContentView(R.layout.activity_game);
        rankDao = RankDaoFactory.create(this);
        currentDifficulty = readDifficulty();
        FrameLayout gameContainer = findViewById(R.id.gameContainer);
        game = createGame(currentDifficulty);
        gameContainer.addView(game, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        buffCountdownView = findViewById(R.id.buffCountdownView);
        bossHealthBar = findViewById(R.id.bossHealthBar);

        setupMusicButton();
        setupSkillButtons();
        setupGameOverOverlay();
        applyCutoutInsets();

        game.setUiListener(new BaseGame.UiListener() {
            @Override
            public void onEnergyFullChanged(boolean full) {
                setSkillButtonsEnabled(full);
            }

            @Override
            public void onBuffStarted(BaseGame.BuffType type, long durationMs) {
                buffCountdownView.startBuff(type, durationMs);
            }

            @Override
            public void onBuffsCleared() {
                buffCountdownView.clearBuffs();
            }

            @Override
            public void onBossHpChanged(int hp, int maxHp) {
                bossHealthBar.updateBossHp(hp, maxHp);
            }

            @Override
            public void onGameOver(int score) {
                lastFinalScore = score;
                scoreSavedForRound = false;
                TextView finalScore = findViewById(R.id.tvFinalScore);
                finalScore.setText(currentDifficulty.getDisplayName() + " · 得分 " + score);
                btnSaveAndRank.setText("保存并查看排行榜");
                buffCountdownView.clearBuffs();
                bossHealthBar.hideImmediately();
                skillContainer.setVisibility(View.GONE);
                gameOverOverlay.setVisibility(View.VISIBLE);
            }
        });
    }

    private BaseGame createGame(GameDifficulty difficulty) {
        switch (difficulty) {
            case EASY:
                return new EasyGame(this);
            case HARD:
                return new HardGame(this);
            case NORMAL:
            default:
                return new NormalGame(this);
        }
    }

    private GameDifficulty readDifficulty() {
        String name = getIntent().getStringExtra(EXTRA_DIFFICULTY);
        if (name != null) {
            try {
                return GameDifficulty.valueOf(name);
            } catch (IllegalArgumentException ignored) {
                // 参数异常时使用默认难度
            }
        }
        return GameDifficulty.NORMAL;
    }

    private void setupMusicButton() {
        ImageButton btnMusic = findViewById(R.id.btnMusic);
        updateMusicIcon(btnMusic);
        btnMusic.setOnClickListener(v -> {
            musicEnabled = !musicEnabled;
            game.setMusicEnabled(musicEnabled);
            updateMusicIcon(btnMusic);
        });
    }

    private void updateMusicIcon(ImageButton button) {
        button.setImageResource(musicEnabled
                ? R.drawable.ic_volume_on
                : R.drawable.ic_volume_off);
    }

    private void setupSkillButtons() {
        btnBomb = findViewById(R.id.btnBomb);
        btnHeal = findViewById(R.id.btnHeal);
        btnRing = findViewById(R.id.btnRing);

        btnBomb.setOnClickListener(v -> game.requestBombUltimate());
        btnHeal.setOnClickListener(v -> game.requestHealUltimate());
        btnRing.setOnClickListener(v -> game.requestRingUltimate());
    }

    private void setupGameOverOverlay() {
        skillContainer = findViewById(R.id.skillContainer);
        gameOverOverlay = findViewById(R.id.gameOverOverlay);
        etPlayerName = findViewById(R.id.etPlayerName);
        btnSaveAndRank = findViewById(R.id.btnSaveAndRank);

        SharedPreferences prefs = getSharedPreferences(PREFS_PLAYER, MODE_PRIVATE);
        String savedName = prefs.getString(KEY_LAST_PLAYER_NAME, "");
        if (!TextUtils.isEmpty(savedName)) {
            etPlayerName.setText(savedName);
        }

        MaterialButton btnRestart = findViewById(R.id.btnRestart);
        MaterialButton btnBackToMenu = findViewById(R.id.btnBackToMenu);

        btnSaveAndRank.setOnClickListener(v -> {
            hideKeyboard();
            if (!scoreSavedForRound) {
                String inputName = etPlayerName.getText().toString().trim();
                String finalName = TextUtils.isEmpty(inputName) ? DEFAULT_PLAYER_NAME : inputName;
                if (!TextUtils.isEmpty(inputName)) {
                    prefs.edit().putString(KEY_LAST_PLAYER_NAME, inputName).apply();
                }
                String now = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        .format(new Date());
                rankDao.insert(new RankRecord(finalName, lastFinalScore, now, currentDifficulty));
                scoreSavedForRound = true;
                btnSaveAndRank.setText("已保存 · 查看排行榜");
                Toast.makeText(this, "成绩已保存到排行榜", Toast.LENGTH_SHORT).show();
            }
            Intent intent = new Intent(this, RankActivity.class);
            intent.putExtra(RankActivity.EXTRA_DIFFICULTY, currentDifficulty.name());
            startActivity(intent);
        });

        btnRestart.setOnClickListener(v -> {
            hideKeyboard();
            scoreSavedForRound = false;
            gameOverOverlay.setVisibility(View.GONE);
            skillContainer.setVisibility(View.VISIBLE);
            setSkillButtonsEnabled(false);
            game.restartGame();
        });
        btnBackToMenu.setOnClickListener(v -> {
            hideKeyboard();
            finish();
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && etPlayerName != null) {
            imm.hideSoftInputFromWindow(etPlayerName.getWindowToken(), 0);
        }
    }

    private void setSkillButtonsEnabled(boolean enabled) {
        float alpha = enabled ? 1.0f : 0.4f;
        ImageButton[] buttons = {btnBomb, btnHeal, btnRing};
        for (ImageButton button : buttons) {
            button.setEnabled(enabled);
            button.setAlpha(alpha);
        }
    }

    private void applyCutoutInsets() {
        View topRightContainer = findViewById(R.id.topRightContainer);
        FrameLayout.LayoutParams rightParams =
                (FrameLayout.LayoutParams) topRightContainer.getLayoutParams();
        final int baseRightTopMargin = rightParams.topMargin;

        FrameLayout.LayoutParams bossParams =
                (FrameLayout.LayoutParams) bossHealthBar.getLayoutParams();
        final int baseBossTopMargin = bossParams.topMargin;

        View contentView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(contentView, (v, insets) -> {
            int cutoutTop = insets.getInsets(WindowInsetsCompat.Type.displayCutout()).top;
            rightParams.topMargin = baseRightTopMargin + cutoutTop;
            topRightContainer.setLayoutParams(rightParams);
            bossParams.topMargin = baseBossTopMargin + cutoutTop;
            bossHealthBar.setLayoutParams(bossParams);
            game.setTopSafeInsetPx(cutoutTop);
            return insets;
        });
        ViewCompat.requestApplyInsets(contentView);
    }

    private void enterImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        getWindow().setAttributes(lp);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enterImmersiveMode();
        }
    }

    @Override
    protected void onDestroy() {
        if (game != null) {
            game.releaseAudio();
        }
        super.onDestroy();
    }
}

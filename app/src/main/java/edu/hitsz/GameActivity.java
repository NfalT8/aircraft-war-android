package edu.hitsz;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;

import edu.hitsz.application.BaseGame;
import edu.hitsz.application.EasyGame;
import edu.hitsz.application.GameDifficulty;
import edu.hitsz.application.HardGame;
import edu.hitsz.application.ImageManager;
import edu.hitsz.application.NormalGame;

/**
 * 游戏主界面：根据 Intent 中携带的难度创建对应的游戏场景。
 */
public class GameActivity extends AppCompatActivity {

    public static final String EXTRA_DIFFICULTY = "extra_difficulty";

    private BaseGame game;
    private ImageButton btnBomb;
    private ImageButton btnHeal;
    private ImageButton btnRing;
    private View skillContainer;
    private View gameOverOverlay;

    private boolean musicEnabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImageManager.init(this);
        enterImmersiveMode();

        setContentView(R.layout.activity_game);
        FrameLayout gameContainer = findViewById(R.id.gameContainer);
        game = createGame(readDifficulty());
        gameContainer.addView(game, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        setupMusicButton();
        setupSkillButtons();
        setupGameOverOverlay();

        game.setUiListener(new BaseGame.UiListener() {
            @Override
            public void onEnergyFullChanged(boolean full) {
                setSkillButtonsEnabled(full);
            }

            @Override
            public void onGameOver(int score) {
                TextView finalScore = findViewById(R.id.tvFinalScore);
                finalScore.setText("得分 " + score);
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

        MaterialButton btnRestart = findViewById(R.id.btnRestart);
        MaterialButton btnBackToMenu = findViewById(R.id.btnBackToMenu);

        btnRestart.setOnClickListener(v -> {
            gameOverOverlay.setVisibility(View.GONE);
            skillContainer.setVisibility(View.VISIBLE);
            setSkillButtonsEnabled(false);
            game.restartGame();
        });
        btnBackToMenu.setOnClickListener(v -> finish());
    }

    private void setSkillButtonsEnabled(boolean enabled) {
        float alpha = enabled ? 1.0f : 0.4f;
        ImageButton[] buttons = {btnBomb, btnHeal, btnRing};
        for (ImageButton button : buttons) {
            button.setEnabled(enabled);
            button.setAlpha(alpha);
        }
    }

    private void enterImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
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

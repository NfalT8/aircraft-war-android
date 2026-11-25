package edu.hitsz;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import edu.hitsz.application.GameDifficulty;

/**
 * 难度选择页：根据用户选择跳转到对应难度的游戏场景。
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);

        setContentView(R.layout.activity_main);

        findViewById(R.id.btnEasy).setOnClickListener(v -> openGame(GameDifficulty.EASY));
        findViewById(R.id.btnNormal).setOnClickListener(v -> openGame(GameDifficulty.NORMAL));
        findViewById(R.id.btnHard).setOnClickListener(v -> openGame(GameDifficulty.HARD));
        findViewById(R.id.btnRank).setOnClickListener(v -> openRank());

        View menuCard = findViewById(R.id.menuCard);
        menuCard.setAlpha(0f);
        menuCard.setTranslationY(36f);
        menuCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(360)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void openGame(GameDifficulty difficulty) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra(GameActivity.EXTRA_DIFFICULTY, difficulty.name());
        startActivity(intent);
    }

    private void openRank() {
        Intent intent = new Intent(this, RankActivity.class);
        startActivity(intent);
    }
}

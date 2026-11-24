package edu.hitsz.application;

import edu.hitsz.R;

public enum GameDifficulty {
    EASY("简单", R.drawable.bg3),
    NORMAL("普通", R.drawable.bg4),
    HARD("困难", R.drawable.bg5);

    private final String displayName;
    private final int backgroundId;

    GameDifficulty(String displayName, int backgroundId) {
        this.displayName = displayName;
        this.backgroundId = backgroundId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getBackgroundId() {
        return backgroundId;
    }
}

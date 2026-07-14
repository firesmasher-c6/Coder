package me.coder.manager;

public class EditorSession {
    public final String token;
    public final String playerName;
    public volatile String browserUser = null;
    public volatile EditorStatus status = EditorStatus.WAITING_FOR_BROWSER;

    public enum EditorStatus {
        WAITING_FOR_BROWSER,
        PENDING_TRUST,
        TRUSTED,
        REJECTED
    }

    public EditorSession(String token, String playerName) {
        this.token = token;
        this.playerName = playerName;
    }
}
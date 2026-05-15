package time;
import java.time.LocalDateTime;

/**
 * 专注记录模型
 * 表示一次专注或休息会话
 */
public class FocusRecord {
    private final LocalDateTime startTime;
    private final long durationSeconds;
    private final String type; // "FOCUS" 或 "BREAK"

    public FocusRecord(LocalDateTime startTime, long durationSeconds, String type) {
        this.startTime = startTime;
        this.durationSeconds = durationSeconds;
        this.type = type;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s %d秒", startTime.toString(), type, durationSeconds);
    }
}
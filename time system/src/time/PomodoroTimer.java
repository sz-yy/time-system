package time;
import time.FocusRecord;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * 番茄钟定时器（完全适配版）
 * - 专注/休息倒计时
 * - 暂停/恢复/停止
 * - 自动生成 FocusRecord 并通过回调通知
 * - 不依赖 FileDataManager，所有配置由构造或 setter 传入
 */
public class PomodoroTimer {

    public enum State { IDLE, FOCUS, BREAK, PAUSED }

    private State state = State.IDLE;
    private State lastActiveState = State.IDLE;
    private int remainingSeconds = 0;

    private int focusSeconds;
    private int breakSeconds;

    // 线程池与定时任务
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> timerTask;

    // 历史记录列表（包含初始加载的和运行时新增的）
    private final List<FocusRecord> records;

    // 新增记录回调
    private Consumer<FocusRecord> onRecordAdded;

    // 本次计时开始时间（用于生成记录）
    private LocalDateTime currentStartTime;
    // 本次计时类型（FOCUS / BREAK），在开始时设定
    private String currentType;

    // 可选监听器（方便外部更新UI/控制台）
    private final List<TimerListener> listeners = new CopyOnWriteArrayList<>();

    public interface TimerListener {
        void onTick(int remainingSeconds);
        void onFinished(State completedState);
        void onStateChanged(State newState);
    }

    /**
     * 构造函数
     * @param initialRecords 从文件加载的已有记录
     * @param focusMinutes  专注时长（分钟）
     * @param breakMinutes  休息时长（分钟）
     */
    public PomodoroTimer(List<FocusRecord> initialRecords, int focusMinutes, int breakMinutes) {
        this.records = new ArrayList<>(initialRecords != null ? initialRecords : Collections.emptyList());
        this.focusSeconds = focusMinutes * 60;
        this.breakSeconds = breakMinutes * 60;
    }

    // ========== 公开控制方法 ==========

    public synchronized void startFocus() {
        if (state == State.FOCUS || state == State.BREAK) {
            stop();
        }
        remainingSeconds = focusSeconds;
        state = State.FOCUS;
        lastActiveState = State.FOCUS;
        currentStartTime = LocalDateTime.now();
        currentType = "FOCUS";
        notifyStateChanged();
        startCountdown();
    }

    public synchronized void startBreak() {
        if (state == State.FOCUS || state == State.BREAK) {
            stop();
        }
        remainingSeconds = breakSeconds;
        state = State.BREAK;
        lastActiveState = State.BREAK;
        currentStartTime = LocalDateTime.now();
        currentType = "BREAK";
        notifyStateChanged();
        startCountdown();
    }

    public synchronized void pause() {
        if (state == State.FOCUS || state == State.BREAK) {
            stopTimerTask();
            state = State.PAUSED;
            notifyStateChanged();
        }
    }

    public synchronized void resume() {
        if (state == State.PAUSED && remainingSeconds > 0 && lastActiveState != State.IDLE) {
            state = lastActiveState;
            currentStartTime = LocalDateTime.now(); // 简单处理：恢复后继续，但记录时仍用最初开始时间，时长按设定算
            startCountdown();
            notifyStateChanged();
        }
    }

    public synchronized void stop() {
        if (state == State.FOCUS || state == State.BREAK) {
            // 计算实际已经过的秒数
            long elapsed = java.time.Duration.between(currentStartTime, LocalDateTime.now()).getSeconds();
            long maxDuration = (currentType.equals("FOCUS")) ? focusSeconds : breakSeconds;
            // 实际记录时长取“已过时间”和“设定时长”中的较小值，避免暂停导致虚高
            long duration = Math.min(elapsed, maxDuration);

            FocusRecord record = new FocusRecord(currentStartTime, duration, currentType);
            records.add(record);

            if (onRecordAdded != null) {
                onRecordAdded.accept(record);
            }
        }
        stopTimerTask();
        state = State.IDLE;
        lastActiveState = State.IDLE;
        remainingSeconds = 0;
        notifyStateChanged();
    }

    public boolean isRunning() {
        return state == State.FOCUS || state == State.BREAK;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public State getState() {
        return state;
    }

    public void setFocusDuration(int minutes) {
        this.focusSeconds = minutes * 60;
        if (state == State.FOCUS) {
            // 如果正在专注，可以按比例缩放剩余时间或不做改动，简单起见不动
        }
    }

    public void setBreakDuration(int minutes) {
        this.breakSeconds = minutes * 60;
        if (state == State.BREAK) {
            // 同理
        }
    }

    /**
     * 获取所有记录（包括初始加载的及运行时生成的）
     */
    public List<FocusRecord> getRecords() {
        return new ArrayList<>(records);
    }

    /**
     * 设置新增记录的回调（由 Main 绑定用于触发 FileDataManager.requestSave）
     */
    public void setOnRecordAdded(Consumer<FocusRecord> callback) {
        this.onRecordAdded = callback;
    }

    public void addTimerListener(TimerListener listener) {
        listeners.add(listener);
    }

    public void removeTimerListener(TimerListener listener) {
        listeners.remove(listener);
    }

    public void shutdown() {
        stopTimerTask();
        scheduler.shutdown();
    }

    // ========== 内部实现 ==========

    private void startCountdown() {
        stopTimerTask();
        timerTask = scheduler.scheduleAtFixedRate(() -> {
            synchronized (PomodoroTimer.this) {
                if (remainingSeconds > 0) {
                    remainingSeconds--;
                    notifyTick();
                }
                if (remainingSeconds <= 0) {
                    stopTimerTask();
                    State completedState = state;
                    // 生成记录
                    long duration = (completedState == State.FOCUS) ? focusSeconds : breakSeconds;
                    FocusRecord record = new FocusRecord(currentStartTime, duration, currentType);
                    records.add(record);
                    // 回调通知
                    if (onRecordAdded != null) {
                        onRecordAdded.accept(record);
                    }
                    // 状态归idle
                    state = State.IDLE;
                    notifyFinished(completedState);
                    notifyStateChanged();
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void stopTimerTask() {
        if (timerTask != null && !timerTask.isCancelled()) {
            timerTask.cancel(false);
            timerTask = null;
        }
    }

    private void notifyTick() {
        for (TimerListener l : listeners) {
            l.onTick(remainingSeconds);
        }
    }

    private void notifyFinished(State completedState) {
        for (TimerListener l : listeners) {
            l.onFinished(completedState);
        }
    }

    private void notifyStateChanged() {
        for (TimerListener l : listeners) {
            l.onStateChanged(state);
        }
    }
}

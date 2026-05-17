package time;
import time.FocusRecord;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class PomodoroTimer {

    public enum State { IDLE, FOCUS, BREAK, PAUSED }

    private State state = State.IDLE;
    private State lastActiveState = State.IDLE;
    private int remainingSeconds = 0;

    private int focusSeconds;
    private int breakSeconds;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> timerTask;

    private final List<FocusRecord> records;

    private Consumer<FocusRecord> onRecordAdded;

    private LocalDateTime currentStartTime;

    private String currentType;


    private final List<TimerListener> listeners = new CopyOnWriteArrayList<>();

    public interface TimerListener {
        void onTick(int remainingSeconds);
        void onFinished(State completedState);
        void onStateChanged(State newState);
    }

    public PomodoroTimer(List<FocusRecord> initialRecords, int focusMinutes, int breakMinutes) {
        this.records = new ArrayList<>(initialRecords != null ? initialRecords : Collections.emptyList());
        this.focusSeconds = focusMinutes * 60;
        this.breakSeconds = breakMinutes * 60;
    }



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
            currentStartTime = LocalDateTime.now();
            startCountdown();
            notifyStateChanged();
        }
    }

    public synchronized void stop() {
        if (state == State.FOCUS || state == State.BREAK) {

            long elapsed = java.time.Duration.between(currentStartTime, LocalDateTime.now()).getSeconds();
            long maxDuration = (currentType.equals("FOCUS")) ? focusSeconds : breakSeconds;
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
        }
    }

    public void setBreakDuration(int minutes) {
        this.breakSeconds = minutes * 60;
        if (state == State.BREAK) {
        }
    }

    public List<FocusRecord> getRecords() {
        return new ArrayList<>(records);
    }

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
                    long duration = (completedState == State.FOCUS) ? focusSeconds : breakSeconds;
                    FocusRecord record = new FocusRecord(currentStartTime, duration, currentType);
                    records.add(record);
                    if (onRecordAdded != null) {
                        onRecordAdded.accept(record);
                    }
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

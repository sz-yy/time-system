package time;
import time.FocusRecord;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学习统计管理器：计算并展示每日/每周学习时长（仅统计专注记录）
 */
public class StatisticsManager {

    public void showDailyStats(List<FocusRecord> records) {
        LocalDate today = LocalDate.now();
        long totalSeconds = records.stream()
                .filter(r -> "FOCUS".equals(r.getType()))
                .filter(r -> r.getStartTime().toLocalDate().equals(today))
                .mapToLong(FocusRecord::getDurationSeconds)
                .sum();
        System.out.println("今日学习时长: " + formatDuration(totalSeconds));
    }

    public void showWeeklyStats(List<FocusRecord> records) {
        LocalDate now = LocalDate.now();
        LocalDate weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        Map<LocalDate, Long> dailySeconds = records.stream()
                .filter(r -> "FOCUS".equals(r.getType()))
                .filter(r -> {
                    LocalDate date = r.getStartTime().toLocalDate();
                    return !date.isBefore(weekStart) && !date.isAfter(weekEnd);
                })
                .collect(Collectors.groupingBy(
                        r -> r.getStartTime().toLocalDate(),
                        Collectors.summingLong(FocusRecord::getDurationSeconds)
                ));

        long totalWeekSeconds = dailySeconds.values().stream().mapToLong(Long::longValue).sum();
        System.out.println("本周学习统计 (周一至周日):");
        System.out.println("总时长: " + formatDuration(totalWeekSeconds));
        System.out.println("每日明细:");
        for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
            long seconds = dailySeconds.getOrDefault(date, 0L);
            System.out.printf("  %s (%s): %s\n", date, getDayOfWeekChinese(date.getDayOfWeek()), formatDuration(seconds));
        }
    }

    private String formatDuration(long totalSeconds) {
        if (totalSeconds <= 0) {
            return "0分钟";
        }
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%d小时%02d分钟%02d秒", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d分钟%02d秒", minutes, seconds);
        } else {
            return String.format("%d秒", seconds);
        }
    }

    private String getDayOfWeekChinese(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "周一";
            case TUESDAY -> "周二";
            case WEDNESDAY -> "周三";
            case THURSDAY -> "周四";
            case FRIDAY -> "周五";
            case SATURDAY -> "周六";
            case SUNDAY -> "周日";
        };
    }
}
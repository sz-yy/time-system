package time;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;
public class ConsoleUI {
    private static final Scanner scanner = new Scanner(System.in);

    // ANSI 颜色码
    public static final String RESET = "\033[0m";
    public static final String RED = "\033[31m";
    public static final String GREEN = "\033[32m";
    public static final String YELLOW = "\033[33m";
    public static final String BLUE = "\033[34m";
    public static final String CYAN = "\033[36m";
    public static final String GRAY = "\033[90m";
    public static final String BOLD = "\033[1m";

    /** 清屏（打印多个空行模拟，兼容性更好） */
    public static void clearScreen() {
        for (int i = 0; i < 50; i++) {
            System.out.println();
        }
    }

    /** 打印水平分隔线 */
    public static void printSeparator() {
        System.out.println("══════════════════════════════════════");
    }

    // 获取字符的半角显示宽度
    private static int charWidth(char c) {
        // CJK 统一表意文字、扩展区、兼容区、全角标点
        if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
            return 2;
        }
        // 全角符号：FF00-FFEF, 3000-303F 等
        if (c >= 0xFF00 && c <= 0xFFEF) return 2;
        if (c >= 0x3000 && c <= 0x303F) return 2;
        // 其他非ASCII视为2（简单粗暴）
        if (c > 127) return 2;
        return 1;
    }

    // 计算整个字符串的显示宽度（忽略 ANSI 颜色码）
    private static int displayWidth(String s) {
        // 移除 ANSI 转义序列
        String plain = s.replaceAll("\033\\[[;\\d]*m", "");
        int w = 0;
        for (char c : plain.toCharArray()) {
            w += charWidth(c);
        }
        return w;
    }

    // 将字符串填充到指定显示宽度（右补空格）
    private static String padRight(String s, int targetWidth) {
        int current = displayWidth(s);
        if (current >= targetWidth) return s;
        return s + " ".repeat(targetWidth - current);
    }
    /** 绘制盒子菜单 */
    public static void printBoxMenu(String title, String[] options) {
        final int INNER_WIDTH = 28;  // 内容区半角宽度，可根据需要调整

        String top    = CYAN + "+" + "-".repeat(INNER_WIDTH) + "+" + RESET;
        String middle = CYAN + "+" + "-".repeat(INNER_WIDTH) + "+" + RESET;
        String bottom = CYAN + "+" + "-".repeat(INNER_WIDTH) + "+" + RESET;

        // 顶边
        System.out.println(top);
        // 标题行：居中
        String titlePlain = title;  // 不含颜色码
        int titleWidth = displayWidth(titlePlain);
        int leftPad = (INNER_WIDTH - titleWidth) / 2;
        int rightPad = INNER_WIDTH - titleWidth - leftPad;
        String titleLine = CYAN + "|" + RESET
                + BOLD + YELLOW
                + " ".repeat(leftPad) + titlePlain + " ".repeat(rightPad)
                + RESET + CYAN + "|" + RESET;
        System.out.println(titleLine);

        // 分隔线
        System.out.println(middle);

        // 选项行
        for (int i = 0; i < options.length; i++) {
            String color = (i == options.length - 1 && options[i].startsWith("0")) ? GRAY : GREEN;
            // 格式："  1. 番茄钟" 这类，左边留3个空格
            String content = "   " + options[i];  // 3个半角空格缩进
            String padded = padRight(content, INNER_WIDTH);
            String line = CYAN + "|" + RESET + color + padded + RESET + CYAN + "|" + RESET;
            System.out.println(line);
        }

        // 底边
        System.out.println(bottom);
        System.out.print("请选择: ");
    }

    /** 彩色待办项显示，根据截止日期标记颜色 */
    public static void printTodoItem(int index, TodoItem item) {
        String status = item.isCompleted() ? "✓" : " ";
        String prefix = String.format("%d. [%s] %s (截止: %s)", index + 1, status, item.getTitle(), item.getDueDate());
        if (item.isCompleted()) {
            System.out.println(GRAY + prefix + RESET);
        } else {
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), item.getDueDate());
            if (daysLeft < 0) {
                System.out.println(RED + prefix + " ⚠已过期" + RESET);
            } else if (daysLeft <= 3) {
                System.out.println(YELLOW + prefix + " ⏳临近" + RESET);
            } else {
                System.out.println(GREEN + prefix + RESET);
            }
        }
    }

    /** 绘制番茄钟进度条 */
    public static void printProgressBar(int remainingSeconds, int totalSeconds) {
        int percent = totalSeconds == 0 ? 0 : (100 - (remainingSeconds * 100 / totalSeconds));
        int filled = percent / 5;
        int empty = 20 - filled;
        String bar = "█".repeat(filled) + "░".repeat(empty);
        int min = remainingSeconds / 60;
        int sec = remainingSeconds % 60;
        System.out.printf("\r⏳ [%s%s%s] %02d:%02d %s",
                CYAN, bar, RESET, min, sec,
                "输入 s 停止，0 返回");
    }

    /** 读取一行输入，去除首尾空格 */
    public static String readLine() {
        return scanner.nextLine().trim();
    }

    /** 打印消息，带颜色 */
    public static void printSuccess(String msg) {
        System.out.println(GREEN + "✓ " + msg + RESET);
    }

    public static void printError(String msg) {
        System.out.println(RED + "✗ " + msg + RESET);
    }

    public static void printInfo(String msg) {
        System.out.println(BLUE + "ℹ " + msg + RESET);
    }
}

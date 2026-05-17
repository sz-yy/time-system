package time;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;
public class ConsoleUI {
    private static final Scanner scanner = new Scanner(System.in);

    public static final String RESET = "\033[0m";
    public static final String RED = "\033[31m";
    public static final String GREEN = "\033[32m";
    public static final String YELLOW = "\033[33m";
    public static final String BLUE = "\033[34m";
    public static final String CYAN = "\033[36m";
    public static final String GRAY = "\033[90m";
    public static final String BOLD = "\033[1m";

    public static void clearScreen() {
        for (int i = 0; i < 50; i++) {
            System.out.println();
        }
    }

    public static void printSeparator() {
        System.out.println("══════════════════════════════════════");
    }

    private static int charWidth(char c) {
        if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
            return 2;
        }
        if (c >= 0xFF00 && c <= 0xFFEF) return 2;
        if (c >= 0x3000 && c <= 0x303F) return 2;
        if (c > 127) return 2;
        return 1;
    }

    private static int displayWidth(String s) {

        String plain = s.replaceAll("\033\\[[;\\d]*m", "");
        int w = 0;
        for (char c : plain.toCharArray()) {
            w += charWidth(c);
        }
        return w;
    }

    private static String padRight(String s, int targetWidth) {
        int current = displayWidth(s);
        if (current >= targetWidth) return s;
        return s + " ".repeat(targetWidth - current);
    }
    public static void printBoxMenu(String title, String[] options) {
        final int INNER_WIDTH = 28;

        String top    = CYAN + "+" + "-".repeat(INNER_WIDTH) + "+" + RESET;
        String middle = CYAN + "+" + "-".repeat(INNER_WIDTH) + "+" + RESET;
        String bottom = CYAN + "+" + "-".repeat(INNER_WIDTH) + "+" + RESET;

        System.out.println(top);
        String titlePlain = title;
        int titleWidth = displayWidth(titlePlain);
        int leftPad = (INNER_WIDTH - titleWidth) / 2;
        int rightPad = INNER_WIDTH - titleWidth - leftPad;
        String titleLine = CYAN + "|" + RESET
                + BOLD + YELLOW
                + " ".repeat(leftPad) + titlePlain + " ".repeat(rightPad)
                + RESET + CYAN + "|" + RESET;
        System.out.println(titleLine);

        System.out.println(middle);

        for (int i = 0; i < options.length; i++) {
            String color = (i == options.length - 1 && options[i].startsWith("0")) ? GRAY : GREEN;
            String content = "   " + options[i];
            String padded = padRight(content, INNER_WIDTH);
            String line = CYAN + "|" + RESET + color + padded + RESET + CYAN + "|" + RESET;
            System.out.println(line);
        }

        System.out.println(bottom);
        System.out.print("请选择: ");
    }
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

    public static void printProgressBar(int remainingSeconds, int totalSeconds, String hints) {
        int percent = totalSeconds == 0 ? 0 : (100 - (remainingSeconds * 100 / totalSeconds));
        int filled = percent / 5;
        int empty = 20 - filled;
        String bar = "█".repeat(filled) + "░".repeat(empty);
        int min = remainingSeconds / 60;
        int sec = remainingSeconds % 60;
        System.out.printf("\r⏳ [%s%s%s] %02d:%02d %s",
                CYAN, bar, RESET, min, sec, hints);
    }
    public static void printProgressBar(int remainingSeconds, int totalSeconds) {
        printProgressBar(remainingSeconds, totalSeconds, "");
    }


    public static String readLine() {
        return scanner.nextLine().trim();
    }

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
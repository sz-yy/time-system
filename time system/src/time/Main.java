package time;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;
public class Main {
    private static TodoManager todoManager;
    private static PomodoroTimer pomodoroTimer;
    private static StatisticsManager statisticsManager;
    private static FileDataManager fileDataManager;
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        fileDataManager = FileDataManager.getInstance();
        List<TodoItem> loadedTodos = fileDataManager.loadTodos();
        List<FocusRecord> loadedRecords = fileDataManager.loadRecords();

        todoManager = new TodoManager(loadedTodos);
        pomodoroTimer = new PomodoroTimer(loadedRecords, 25, 5); // 默认专注25分钟，休息5分钟
        statisticsManager = new StatisticsManager();

        Runnable saveCallback = () -> {
            fileDataManager.saveTodos(todoManager.getItems());
            fileDataManager.saveRecords(pomodoroTimer.getRecords());
        };
        fileDataManager.registerSaveCallback(saveCallback);

        todoManager.setOnDataChanged(() -> fileDataManager.requestSave());
        pomodoroTimer.setOnRecordAdded(record -> fileDataManager.requestSave());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("正在保存数据...");
            saveCallback.run();
            System.out.println("数据已保存，再见！");
        }));

        boolean exit = false;
        while (!exit) {
            printStatusBar();
            printMainMenu();
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> handlePomodoroMenu();
                case "2" -> handleTodoMenu();
                case "3" -> handleStatisticsMenu();
                case "4" -> handleSettings();
                case "0" -> exit = true;
                default -> System.out.println("无效选项，请重新输入。");
            }
        }

        saveCallback.run();
        System.out.println("程序已退出。");
    }

    private static void printStatusBar() {
        ConsoleUI.clearScreen();
        System.out.println(ConsoleUI.CYAN + ConsoleUI.BOLD +
                "===== 个人学习效率小助手 =====" + ConsoleUI.RESET);
        if (pomodoroTimer.isRunning()) {
            int remaining = pomodoroTimer.getRemainingSeconds();
            int total = (pomodoroTimer.getState() == PomodoroTimer.State.FOCUS) ? 25 * 60 : 5 * 60;
            ConsoleUI.printProgressBar(remaining, total);
            System.out.println();
        } else {
            System.out.println(ConsoleUI.GRAY + "【番茄钟空闲】" + ConsoleUI.RESET);
        }
        ConsoleUI.printSeparator();
    }
    private static void printMainMenu() {
        String[] options = {
                "1. 番茄钟",
                "2. 待办清单",
                "3. 学习统计",
                "4. 设置",
                "0. 退出"
        };
        ConsoleUI.printBoxMenu("个人学习效率小助手", options);
    }

    private static void handlePomodoroMenu() {
        boolean back = false;
        while (!back) {
            if (pomodoroTimer.isRunning()) {
                int remaining = pomodoroTimer.getRemainingSeconds();
                int min = remaining / 60;
                int sec = remaining % 60;
                System.out.printf("\r⏳ 番茄钟运行中：%02d:%02d （1=专注 2=休息 s=停止 0=返回）", min, sec);
                try {
                    if (System.in.available() > 0) {
                        String input = scanner.nextLine().trim().toLowerCase();
                        switch (input) {
                            case "1":
                                pomodoroTimer.stop();
                                pomodoroTimer.startFocus();
                                break;
                            case "2":
                                pomodoroTimer.stop();
                                pomodoroTimer.startBreak();
                                break;
                            case "s":
                                pomodoroTimer.stop();
                                System.out.println("\n计时已手动停止。");
                                break;
                            case "0":
                                pomodoroTimer.stop();
                                back = true;
                                break;
                            default:
                                break;
                        }
                    }
                    Thread.sleep(1000);
                } catch (Exception e) {
                    // ignore
                }
                continue;
            }

            System.out.println("\n-- 番茄钟 --");
            System.out.println("1. 开始专注");
            System.out.println("2. 开始休息");
            System.out.println("3. 停止当前计时");
            System.out.println("0. 返回");
            System.out.print("请选择: ");
            String opt = scanner.nextLine().trim();
            switch (opt) {
                case "1" -> pomodoroTimer.startFocus();
                case "2" -> pomodoroTimer.startBreak();
                case "3" -> {
                    if (pomodoroTimer.isRunning()) pomodoroTimer.stop();
                    else System.out.println("当前没有运行中的计时。");
                }
                case "0" -> {
                    if (pomodoroTimer.isRunning()) {
                        pomodoroTimer.stop(); 
                    }
                    back = true;
                }
                default -> System.out.println("无效选项。");
            }
        }
    }
    private static void handleTodoMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n" + ConsoleUI.CYAN + "-- 待办清单 --" + ConsoleUI.RESET);
            List<TodoItem> items = todoManager.getItems();
            if (items.isEmpty()) {
                ConsoleUI.printInfo("清单为空");
            } else {
                for (int i = 0; i < items.size(); i++) {
                    TodoItem item = items.get(i);
                    ConsoleUI.printTodoItem(i, item);
                }
            }
            System.out.println();
            System.out.println("选项: " + ConsoleUI.GREEN + "1.添加" + ConsoleUI.RESET +
                    " " + ConsoleUI.YELLOW + "2.删除" + ConsoleUI.RESET +
                    " " + ConsoleUI.BLUE + "3.标记完成/未完成" + ConsoleUI.RESET +
                    " " + ConsoleUI.GRAY + "0.返回" + ConsoleUI.RESET);
            System.out.print("请选择: ");
            String opt = scanner.nextLine().trim();
            switch (opt) {
                case "1" -> {
                    System.out.print("输入待办标题: ");
                    String title = scanner.nextLine().trim();
                    if (title.isEmpty()) {
                        ConsoleUI.printError("标题不能为空。");
                        break;
                    }
                    System.out.print("输入截止日期 (yyyy-mm-dd): ");
                    String dateStr = scanner.nextLine().trim();
                    try {
                        LocalDate due = LocalDate.parse(dateStr);
                        todoManager.addItem(title, due);
                        ConsoleUI.printSuccess("已添加。");
                    } catch (Exception e) {
                        ConsoleUI.printError("日期格式错误。");
                    }
                }
                case "2" -> {
                    System.out.print("输入要删除的序号: ");
                    try {
                        int idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                        todoManager.deleteItem(idx);
                        ConsoleUI.printSuccess("已删除。");
                    } catch (Exception e) {
                        ConsoleUI.printError("无效序号。");
                    }
                }
                case "3" -> {
                    System.out.print("输入要标记的序号: ");
                    try {
                        int idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                        todoManager.toggleComplete(idx);
                        ConsoleUI.printSuccess("已切换状态。");
                    } catch (Exception e) {
                        ConsoleUI.printError("无效序号。");
                    }
                }
                case "0" -> back = true;
                default -> ConsoleUI.printError("无效选项。");
            }
        }
    }

    private static void handleStatisticsMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n-- 学习统计 --");
            System.out.println("1. 今日学习时长");
            System.out.println("2. 本周学习时长");
            System.out.println("0. 返回");
            System.out.print("请选择: ");
            String opt = scanner.nextLine().trim();
            switch (opt) {
                case "1" -> statisticsManager.showDailyStats(pomodoroTimer.getRecords());
                case "2" -> statisticsManager.showWeeklyStats(pomodoroTimer.getRecords());
                case "0" -> back = true;
                default -> System.out.println("无效选项。");
            }
        }
    }

    private static void handleSettings() {
        boolean back = false;
        while (!back) {
            System.out.println("\n-- 设置 --");
            System.out.println("1. 设置专注时长（分钟）");
            System.out.println("2. 设置休息时长（分钟）");
            System.out.println("0. 返回");
            System.out.print("请选择: ");
            String opt = scanner.nextLine().trim();
            switch (opt) {
                case "1" -> {
                    System.out.print("请输入新的专注时长: ");
                    try {
                        int mins = Integer.parseInt(scanner.nextLine().trim());
                        if (mins > 0) {
                            pomodoroTimer.setFocusDuration(mins);
                            System.out.println("已更新专注时长为 " + mins + " 分钟。");
                        } else {
                            System.out.println("时长必须为正数。");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("输入无效。");
                    }
                }
                case "2" -> {
                    System.out.print("请输入新的休息时长: ");
                    try {
                        int mins = Integer.parseInt(scanner.nextLine().trim());
                        if (mins > 0) {
                            pomodoroTimer.setBreakDuration(mins);
                            System.out.println("已更新休息时长为 " + mins + " 分钟。");
                        } else {
                            System.out.println("时长必须为正数。");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("输入无效。");
                    }
                }
                case "0" -> back = true;
                default -> System.out.println("无效选项。");
            }
        }
    }
}
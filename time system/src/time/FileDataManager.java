package time;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 数据持久化核心类 - 单例模式
 * 负责待办事项和专注记录的加载与保存
 * 文件路径固定为 data/ 目录下
 */
public class FileDataManager {

    // ========== 单例 ==========
    private static FileDataManager instance;

    // 固定文件路径
    private static final String DATA_DIR = "data";
    private static final String TODOS_FILE = DATA_DIR + "/todos.txt";
    private static final String RECORDS_FILE = DATA_DIR + "/focus_records.txt";

    /**
     * 获取单例实例
     */
    public static FileDataManager getInstance() {
        if (instance == null) {
            instance = new FileDataManager();
        }
        return instance;
    }

    // ========== 回调 ==========
    private Runnable saveCallback;

    /**
     * 注册保存回调（由 Main 或其他模块调用）
     */
    public void registerSaveCallback(Runnable callback) {
        this.saveCallback = callback;
    }

    /**
     * 执行注册的回调，不直接操作待办和记录
     */
    public void requestSave() {
        if (saveCallback != null) {
            saveCallback.run();
        } else {
            System.out.println("[FileDataManager] 警告：未注册保存回调，requestSave() 无操作。");
        }
    }

    // ========== 构造方法私有 ==========
    private FileDataManager() {
        // 不在构造时创建目录，等保存时再自动创建
    }

    // ========== 日期格式化 ==========
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // ========== 加载待办事项 ==========
    /**
     * 加载待办事项列表
     * 文件不存在时返回空列表，不创建文件也不报错
     */
    public List<TodoItem> loadTodos() {
        List<TodoItem> todos = new ArrayList<>();
        File file = new File(TODOS_FILE);
        if (!file.exists()) {
            return todos; // 文件不存在，直接返回空列表
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                // 格式：标题|截止日期|是否完成
                String[] parts = line.split("\\|");
                if (parts.length == 3) {
                    String title = parts[0].trim();
                    String deadline = parts[1].trim();
                    boolean completed = Boolean.parseBoolean(parts[2].trim());
                    LocalDate dueDate = LocalDate.parse(deadline);
                    todos.add(new TodoItem(title, dueDate, completed));
                }
            }
        } catch (IOException e) {
            System.err.println("[FileDataManager] 加载待办事项失败：" + e.getMessage());
        }
        return todos;
    }

    // ========== 保存待办事项 ==========
    /**
     * 保存待办事项列表（完全覆写）
     * 保存时自动创建 data/ 目录和文件
     */
    public void saveTodos(List<TodoItem> todos) {
        try {
            ensureDataDirExists();
        } catch (IOException e) {
            System.err.println("[FileDataManager] 创建 data 目录失败：" + e.getMessage());
            return;
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TODOS_FILE))) {
            for (TodoItem todo : todos) {
                // 格式：标题|截止日期|是否完成
                writer.write(todo.getTitle() + "|" + todo.getDueDate() + "|" + todo.isCompleted());
                writer.newLine();
            }
            writer.flush();
        } catch (IOException e) {
            System.err.println("[FileDataManager] 保存待办事项失败：" + e.getMessage());
        }
    }

    // ========== 加载专注记录 ==========
    /**
     * 加载专注记录列表
     * 文件不存在时返回空列表，不创建文件也不报错
     */
    public List<FocusRecord> loadRecords() {
        List<FocusRecord> records = new ArrayList<>();
        File file = new File(RECORDS_FILE);
        if (!file.exists()) {
            return records; // 文件不存在，直接返回空列表
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                // 格式：开始时间|持续秒数|类型
                String[] parts = line.split("\\|");
                if (parts.length == 3) {
                    LocalDateTime startTime = LocalDateTime.parse(parts[0].trim(), FORMATTER);
                    long durationSeconds = Long.parseLong(parts[1].trim());
                    String type = parts[2].trim();
                    records.add(new FocusRecord(startTime, durationSeconds, type));
                }
            }
        } catch (IOException e) {
            System.err.println("[FileDataManager] 加载专注记录失败：" + e.getMessage());
        }
        return records;
    }

    // ========== 保存专注记录 ==========
    /**
     * 保存专注记录列表（完全覆写）
     * 保存时自动创建 data/ 目录和文件
     */
    public void saveRecords(List<FocusRecord> records) {
        try {
            ensureDataDirExists();
        } catch (IOException e) {
            System.err.println("[FileDataManager] 创建 data 目录失败：" + e.getMessage());
            return;
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(RECORDS_FILE))) {
            for (FocusRecord record : records) {
                // 格式：开始时间|持续秒数|类型
                writer.write(record.getStartTime().format(FORMATTER) + "|"
                        + record.getDurationSeconds() + "|"
                        + record.getType());
                writer.newLine();
            }
            writer.flush();
        } catch (IOException e) {
            System.err.println("[FileDataManager] 保存专注记录失败：" + e.getMessage());
        }
    }

    // ========== 辅助方法 ==========
    /**
     * 确保 data/ 目录存在，不存在则自动创建
     */
    private void ensureDataDirExists() throws IOException {
        Path dirPath = Paths.get(DATA_DIR);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
    }
}
package time;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class FileDataManager {

    private static FileDataManager instance;

    private static final String DATA_DIR = "data";
    private static final String TODOS_FILE = DATA_DIR + "/todos.txt";
    private static final String RECORDS_FILE = DATA_DIR + "/focus_records.txt";

    public static FileDataManager getInstance() {
        if (instance == null) {
            instance = new FileDataManager();
        }
        return instance;
    }

    private Runnable saveCallback;

    public void registerSaveCallback(Runnable callback) {
        this.saveCallback = callback;
    }

    public void requestSave() {
        if (saveCallback != null) {
            saveCallback.run();
        } else {
            System.out.println("[FileDataManager] 警告：未注册保存回调，requestSave() 无操作。");
        }
    }

    private FileDataManager() {
    }

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;


    public List<TodoItem> loadTodos() {
        List<TodoItem> todos = new ArrayList<>();
        File file = new File(TODOS_FILE);
        if (!file.exists()) {
            return todos;
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


    public void saveTodos(List<TodoItem> todos) {
        try {
            ensureDataDirExists();
        } catch (IOException e) {
            System.err.println("[FileDataManager] 创建 data 目录失败：" + e.getMessage());
            return;
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TODOS_FILE))) {
            for (TodoItem todo : todos) {
                writer.write(todo.getTitle() + "|" + todo.getDueDate() + "|" + todo.isCompleted());
                writer.newLine();
            }
            writer.flush();
        } catch (IOException e) {
            System.err.println("[FileDataManager] 保存待办事项失败：" + e.getMessage());
        }
    }


    public List<FocusRecord> loadRecords() {
        List<FocusRecord> records = new ArrayList<>();
        File file = new File(RECORDS_FILE);
        if (!file.exists()) {
            return records;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
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


    public void saveRecords(List<FocusRecord> records) {
        try {
            ensureDataDirExists();
        } catch (IOException e) {
            System.err.println("[FileDataManager] 创建 data 目录失败：" + e.getMessage());
            return;
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(RECORDS_FILE))) {
            for (FocusRecord record : records) {
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

    private void ensureDataDirExists() throws IOException {
        Path dirPath = Paths.get(DATA_DIR);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
    }
}
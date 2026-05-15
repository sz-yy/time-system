package time;
// TodoItem.java

import java.time.LocalDate;

public class TodoItem {

    // 标题
    private String title;

    // 截止日期
    private LocalDate dueDate;

    // 是否完成
    private boolean completed;

    // 默认未完成
    public TodoItem(String title, LocalDate dueDate) {
        this.title = title;
        this.dueDate = dueDate;
        this.completed = false;
    }

    // 从文件读取时使用
    public TodoItem(String title, LocalDate dueDate, boolean completed) {
        this.title = title;
        this.dueDate = dueDate;
        this.completed = completed;
    }

    // getter
    public String getTitle() {
        return title;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public boolean isCompleted() {
        return completed;
    }

    // setter
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
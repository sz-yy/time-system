package time;
// TodoManager.java

import time.TodoItem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TodoManager {

    // 待办列表
    private List<TodoItem> items;

    // 数据变化回调
    private Runnable onDataChanged;

    // 构造函数
    public TodoManager(List<TodoItem> initialItems) {

        if (initialItems == null) {
            this.items = new ArrayList<>();
        } else {
            this.items = new ArrayList<>(initialItems);
        }

        sortItems();
    }

    // 设置数据变化回调
    public void setOnDataChanged(Runnable callback) {
        this.onDataChanged = callback;
    }

    // 添加待办
    public void addItem(String title, LocalDate dueDate) {

        items.add(new TodoItem(title, dueDate));

        sortItems();

        notifyChange();
    }

    // 删除待办
    public void deleteItem(int index) {

        if (index < 0 || index >= items.size()) {
            return;
        }

        items.remove(index);

        notifyChange();
    }

    // 切换完成状态
    public void toggleComplete(int index) {

        if (index < 0 || index >= items.size()) {
            return;
        }

        TodoItem item = items.get(index);

        item.setCompleted(!item.isCompleted());

        sortItems();

        notifyChange();
    }

    // 获取排序后的列表副本
    public List<TodoItem> getItems() {

        sortItems();

        return new ArrayList<>(items);
    }

    // 排序
    // 未完成优先
    // 同状态按截止日期升序
    private void sortItems() {

        items.sort(
                Comparator
                        .comparing(TodoItem::isCompleted)
                        .thenComparing(TodoItem::getDueDate)
        );
    }

    // 通知数据变化
    private void notifyChange() {

        if (onDataChanged != null) {
            onDataChanged.run();
        }
    }
}
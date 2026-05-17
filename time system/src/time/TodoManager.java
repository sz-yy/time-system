package time;

import time.TodoItem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TodoManager {

    private List<TodoItem> items;

    private Runnable onDataChanged;

    public TodoManager(List<TodoItem> initialItems) {

        if (initialItems == null) {
            this.items = new ArrayList<>();
        } else {
            this.items = new ArrayList<>(initialItems);
        }

        sortItems();
    }

    public void setOnDataChanged(Runnable callback) {
        this.onDataChanged = callback;
    }

    public void addItem(String title, LocalDate dueDate) {

        items.add(new TodoItem(title, dueDate));

        sortItems();

        notifyChange();
    }

    public void deleteItem(int index) {

        if (index < 0 || index >= items.size()) {
            return;
        }

        items.remove(index);

        notifyChange();
    }

    public void toggleComplete(int index) {

        if (index < 0 || index >= items.size()) {
            return;
        }

        TodoItem item = items.get(index);

        item.setCompleted(!item.isCompleted());

        sortItems();

        notifyChange();
    }

    public List<TodoItem> getItems() {

        sortItems();

        return new ArrayList<>(items);
    }

    private void sortItems() {

        items.sort(
                Comparator
                        .comparing(TodoItem::isCompleted)
                        .thenComparing(TodoItem::getDueDate)
        );
    }

    private void notifyChange() {

        if (onDataChanged != null) {
            onDataChanged.run();
        }
    }
}
package com.example.metta.optimizer;

import java.util.List;
import java.util.ArrayList;

public class TaskSuite {
    private final List<QueryTask> tasks;

    public TaskSuite() {
        this.tasks = new ArrayList<>();
    }

    public TaskSuite(List<QueryTask> initialTasks) {
        this.tasks = new ArrayList<>(initialTasks);
    }

    public void addTask(QueryTask task) {
        if (task != null) {
            this.tasks.add(task);
        }
    }

    public List<QueryTask> getTasks() {
        return tasks; // Or Collections.unmodifiableList(tasks) if preferred
    }
}

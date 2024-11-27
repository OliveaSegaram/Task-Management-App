package com.example.memomate.repos

import android.content.Context
import android.content.SharedPreferences
import com.example.memomate.model.TaskModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TaskRepository(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("task_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // Fetch all tasks stored as JSON
    fun getAllTasks(): List<TaskModel> {
        val json = sharedPreferences.getString("tasks_list", null)
        val type = object : TypeToken<List<TaskModel>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    // Save tasks to SharedPreferences
    fun saveAllTasks(tasks: List<TaskModel>) {
        val editor = sharedPreferences.edit()
        val json = gson.toJson(tasks)
        editor.putString("tasks_list", json)
        editor.apply()
    }

    // Add a new task
    fun addTask(task: TaskModel) {
        val tasks = getAllTasks().toMutableList()
        tasks.add(task)
        saveAllTasks(tasks)
    }

    // Update an existing task
    fun updateTask(updatedTask: TaskModel) {
        val tasks = getAllTasks().toMutableList()
        val taskIndex = tasks.indexOfFirst { it.id == updatedTask.id }
        if (taskIndex != -1) {
            tasks[taskIndex] = updatedTask
            saveAllTasks(tasks)
        }
    }

    // Delete a task
    fun deleteTask(task: TaskModel) {
        val tasks = getAllTasks().toMutableList()
        tasks.removeAll { it.id == task.id }
        saveAllTasks(tasks)
    }

    // Filter tasks by category
    fun getTasksByCategory(category: String): List<TaskModel> {
        return getAllTasks().filter { it.category == category }
    }

    // Filter tasks by date range
    fun getTaskWithDateFilter(startDate: String, endDate: String): List<TaskModel> {
        return getAllTasks().filter { it.taskDate in startDate..endDate }
    }
}

package com.example.memomate.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.memomate.model.TaskModel
import com.example.memomate.repos.TaskRepository

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TaskRepository(application)

    // Insert task
    fun insertTask(taskModel: TaskModel) {
        repository.addTask(taskModel)  // No need for coroutines or LiveData
    }

    // Fetch all tasks
    fun fetchAllTasks(): List<TaskModel> {
        return repository.getAllTasks()  // Return a simple list instead of LiveData
    }

    // Fetch tasks by category
    fun fetchAllTasksByCategory(category: String): List<TaskModel> {
        return repository.getTasksByCategory(category)
    }

    // Get tasks with a date filter
    fun getTaskWithDateFilter(startDate: String, endDate: String): List<TaskModel> {
        return repository.getTaskWithDateFilter(startDate, endDate)
    }

    // Update task completion status
    fun updateTask(taskModel: TaskModel) {
        taskModel.isCompleted = !taskModel.isCompleted
        repository.updateTask(taskModel)  // Directly call the repository to update the task
    }

    // Delete task
    fun deleteTask(taskModel: TaskModel) {
        repository.deleteTask(taskModel)  // Directly call the repository to delete the task
    }
}

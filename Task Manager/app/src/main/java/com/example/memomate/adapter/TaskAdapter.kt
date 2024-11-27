package com.example.memomate.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.memomate.R
import com.example.memomate.databinding.ItemTaskBinding
import com.example.memomate.model.TaskModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TaskAdapter(val actionCallback: (TaskModel, String) -> Unit) :
    ListAdapter<TaskModel, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    class TaskViewHolder(
        private val binding: ItemTaskBinding,
        val actionCallback: (TaskModel, String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(taskModel: TaskModel) {
            binding.apply {
                title.text = taskModel.tittle
                description.text = taskModel.des
                date.text = taskModel.taskDate
                time.text = taskModel.time
                checkbox.isChecked = taskModel.isCompleted

                // Handle checkbox click
                checkbox.setOnClickListener {
                    val updatedTask = taskModel.copy(isCompleted = checkbox.isChecked)
                    Log.d("TaskAdapter", "Checkbox clicked: ${checkbox.isChecked}")
                    actionCallback(updatedTask, "Update")
                }

                // Handle item click for editing
                root.setOnClickListener {
                    actionCallback(taskModel, "Edit")
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding, actionCallback)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val taskModel = getItem(position)
        holder.bind(taskModel)
    }

    fun getTaskAtPosition(position: Int): TaskModel {
        return getItem(position)
    }
}

class TaskDiffCallback : DiffUtil.ItemCallback<TaskModel>() {
    override fun areItemsTheSame(oldItem: TaskModel, newItem: TaskModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: TaskModel, newItem: TaskModel): Boolean {
        return oldItem == newItem
    }
}

class SwipeToDeleteCallback(
    private val context: Context,
    private val adapter: TaskAdapter,
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        val taskModel = adapter.getTaskAtPosition(position)

        MaterialAlertDialogBuilder(context)
            .setIcon(R.drawable.baseline_delete_24)
            .setTitle("Are you sure!")
            .setMessage("You really want to delete this task?")
            .setNegativeButton("No") { dialog, which ->
                // Reset item
                adapter.notifyItemChanged(position)
            }
            .setPositiveButton("Yes") { dialog, which ->
                // Delete item
                adapter.actionCallback(taskModel, "Delete")
            }
            .show()
    }
}

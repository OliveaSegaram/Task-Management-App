package com.example.memomate.Fragment

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkManager
import com.example.memomate.R
import com.example.memomate.adapter.SwipeToDeleteCallback
import com.example.memomate.adapter.TaskAdapter
import com.example.memomate.databinding.FragmentTaskListBinding
import com.example.memomate.model.TaskModel
import com.example.memomate.viewModel.TaskViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TaskList : Fragment() {

    private lateinit var binding: FragmentTaskListBinding
    private val taskViewModel: TaskViewModel by viewModels()
    private lateinit var adapter: TaskAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var textView: TextView

    // Stopwatch variables
    private var startTime = 0L
    private var timeInMillis = 0L
    private var timeSwapBuff = 0L
    private var updateTime = 0L
    private var handler: Handler = Handler()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTaskListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the TextView to display the user's name
        textView = binding.textView  // This is the TextView from fragment_task_list.xml

        // Retrieve SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        // Retrieve the saved user name from SharedPreferences
        val userName = sharedPreferences.getString("Name", "User")  // Default value is "User" if no name is found

        // Set the user name in the TextView
        textView.text = "Hey, $userName"

        // Initialize adapter and set up RecyclerView
        adapter = TaskAdapter(::taskAction)
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.adapter = adapter

        // Load tasks from SharedPreferences and display them
        val tasks = taskViewModel.fetchAllTasks()
        if (tasks.isEmpty()) {
            binding.emptyText.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.emptyText.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            adapter.submitList(tasks)
        }

        // Initialize DatePickers
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select start date")
            .build()

        val endDatePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select end date")
            .build()

        // Set date picker listeners
        binding.dateText.setOnClickListener {
            datePicker.show(childFragmentManager, "DATE_PICKER")
        }

        datePicker.addOnPositiveButtonClickListener { selection ->
            val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
            val formattedDate = Instant.ofEpochMilli(selection)
                .atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
            binding.dateText.setText(formattedDate)
        }

        binding.endDateText.setOnClickListener {
            endDatePicker.show(childFragmentManager, "END_DATE_PICKER")
        }

        endDatePicker.addOnPositiveButtonClickListener { selection ->
            val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
            val formattedDate = Instant.ofEpochMilli(selection)
                .atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
            binding.endDateText.setText(formattedDate)
        }

        // Swipe to delete functionality
        val swipeToDeleteCallback = SwipeToDeleteCallback(requireContext(), adapter)
        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        binding.addTask.setOnClickListener {
            findNavController().navigate(R.id.action_taskList_to_addTask)
        }

        // Handle category filter toggle
        binding.toggleButton.addOnButtonCheckedListener { toggleButton, checkedId, isChecked ->
            if (isChecked) {
                val checkedButton = toggleButton.findViewById<Button>(checkedId)
                binding.textView3.text = "Task overview ${checkedButton.text}:"

                val filteredTasks = taskViewModel.fetchAllTasksByCategory(checkedButton.text.toString())
                if (filteredTasks.isEmpty()) {
                    binding.emptyText.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    binding.emptyText.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                    adapter.submitList(filteredTasks)
                }
            }
        }

        // Handle search button click
        binding.search.setOnClickListener {
            val startDate = binding.dateText.text.toString()
            val endDate = binding.endDateText.text.toString()

            if (startDate.isBlank()) {
                Toast.makeText(requireContext(), "Please provide a valid start date", Toast.LENGTH_LONG).show()
            } else if (endDate.isBlank()) {
                Toast.makeText(requireContext(), "Please provide a valid end date", Toast.LENGTH_LONG).show()
            } else {
                val filteredTasks = taskViewModel.getTaskWithDateFilter(startDate, endDate)
                if (filteredTasks.isEmpty()) {
                    binding.emptyText.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    binding.emptyText.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                    adapter.submitList(filteredTasks)
                }
            }
        }

        // Initialize and start the stopwatch
        startStopwatch()
    }

    private fun taskAction(taskModel: TaskModel, tag: String) {
        when(tag) {
            "Edit" -> {
                val bundle = Bundle().apply {
                    putSerializable("task", taskModel)
                }
                findNavController().navigate(R.id.action_taskList_to_addTask, bundle)
            }
            "Delete" -> {
                taskViewModel.deleteTask(taskModel)
                // Optionally cancel notification using WorkManager
                WorkManager.getInstance(requireContext()).cancelUniqueWork(taskModel.tittle)
            }
        }
    }

    // Stopwatch implementation
    private fun startStopwatch() {
        startTime = SystemClock.uptimeMillis()
        handler.postDelayed(updateStopwatch, 0)
    }

    private val updateStopwatch: Runnable = object : Runnable {
        override fun run() {
            timeInMillis = SystemClock.uptimeMillis() - startTime
            updateTime = timeSwapBuff + timeInMillis

            val secs = (updateTime / 1000).toInt() % 60
            val mins = (updateTime / 1000 / 60).toInt() % 60
            val hrs = (updateTime / 1000 / 3600).toInt()

            // Format the stopwatch time as HH:MM:SS
            val timeFormatted = String.format("%02d:%02d:%02d", hrs, mins, secs)

            // Safely access stopwatchTimer with let
            binding.stopwatchTimer?.let {
                it.text = timeFormatted
            }

            // Update every 1 second
            handler.postDelayed(this, 1000)
        }
    }


    override fun onPause() {
        super.onPause()
        // Stop updating the stopwatch when the fragment is paused
        timeSwapBuff += timeInMillis
        handler.removeCallbacks(updateStopwatch)
    }

    override fun onResume() {
        super.onResume()
        // Resume the stopwatch when the fragment is resumed
        startTime = SystemClock.uptimeMillis()
        handler.postDelayed(updateStopwatch, 0)
    }
}

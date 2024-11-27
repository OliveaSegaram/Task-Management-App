package com.example.memomate.Fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.memomate.databinding.FragmentAddTaskBinding
import com.example.memomate.model.TaskModel
import com.example.memomate.utils.WorkManagerService
import com.example.memomate.viewModel.TaskViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class AddTask : Fragment() {

    private lateinit var binding: FragmentAddTaskBinding
    private val taskViewModel: TaskViewModel by viewModels()
    private var taskToUpdate: TaskModel? = null
    private var selectedText: String = "High"
    private lateinit var workManagerService: WorkManagerService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddTaskBinding.inflate(inflater, container, false)
        workManagerService = WorkManagerService(requireContext())  // Initialize WorkManagerService
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check if we're editing an existing task
        taskToUpdate = arguments?.getSerializable("task") as? TaskModel

        taskToUpdate?.let { task ->
            binding.tittleText.setText(task.tittle)
            binding.desText.setText(task.des)
            binding.dateText.setText(task.taskDate)
            binding.timeText.setText(task.time)
        }

        // Handle notification permission request
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        // Initialize DatePicker
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select task reminder date")
            .build()

        binding.dateText.setOnClickListener {
            datePicker.show(childFragmentManager, "DATE_PICKER")
        }

        datePicker.addOnPositiveButtonClickListener { selection ->
            val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
            val formattedDate = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(selection),
                ZoneId.systemDefault()
            ).format(dateFormatter)
            binding.dateText.setText(formattedDate)
        }

        // Initialize TimePicker
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setTitleText("Select time")
            .build()

        binding.timeText.setOnClickListener {
            timePicker.show(childFragmentManager, "TIME_PICKER")
        }

        timePicker.addOnPositiveButtonClickListener {
            val hour = timePicker.hour
            val minute = timePicker.minute
            val formattedTime = String.format(
                "%02d:%02d %s", if (hour == 0 || hour == 12) 12 else hour % 12, minute,
                if (hour < 12) "AM" else "PM"
            )
            binding.timeText.setText(formattedTime)
        }

        // ChipGroup for task priority
        binding.chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            for (id in checkedIds) {
                val chip = group.findViewById<Chip>(id)
                if (chip.isChecked) {
                    selectedText = chip.text.toString()
                }
            }
        }

        // Save or Update button logic
        binding.saveButton.setOnClickListener {
            val title = binding.tittleText.text.toString()
            val description = binding.desText.text.toString()
            val date = binding.dateText.text.toString()
            val time = binding.timeText.text.toString()
            val currentDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())

            if (title.isBlank()) {
                binding.tittleText.error = "Please provide a title"
            } else {
                val task = taskToUpdate?.copy(
                    tittle = title,
                    des = description,
                    taskDate = date,
                    time = time
                ) ?: TaskModel(
                    tittle = title,
                    des = description,
                    taskDate = date,
                    time = time,
                    currentDate = currentDate,
                    category = selectedText
                )

                // **Cancel the old reminder if this is an update**
                taskToUpdate?.let { existingTask ->
                    workManagerService.cancel(existingTask.tittle)  // Cancel existing work for the old task
                }

                // **Schedule new reminder**
                val delay = calculateDelayForReminder(date, time)  // Calculate delay in milliseconds
                workManagerService.schedule(title, delay)

                if (taskToUpdate != null) {
                    taskViewModel.updateTask(task)
                    Toast.makeText(requireContext(), "Note updated successfully", Toast.LENGTH_SHORT).show()
                } else {
                    taskViewModel.insertTask(task)
                    Toast.makeText(requireContext(), "Note added successfully", Toast.LENGTH_SHORT).show()
                }

                findNavController().popBackStack()
            }
        }
    }

    private fun calculateDelayForReminder(date: String, time: String): Long {
        // You need to calculate the delay here based on the selected date and time
        val formatter = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
        val selectedDateTime = formatter.parse("$date $time")?.time ?: 0L
        val currentTime = System.currentTimeMillis()
        return if (selectedDateTime > currentTime) {
            selectedDateTime - currentTime
        } else {
            0L  // If the selected time is in the past, handle this appropriately
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted
        } else {
            Toast.makeText(requireContext(), "Notification permission is required for reminders.", Toast.LENGTH_SHORT).show()
        }
    }
}

package com.example.memomate.model


import java.io.Serializable

data class TaskModel(
    var id: Long = 0,
    val tittle: String,
    var des: String,
    var taskDate: String,
    var currentDate: String,
    var time: String,
    var category: String,
    var isCompleted: Boolean = false
) : Serializable

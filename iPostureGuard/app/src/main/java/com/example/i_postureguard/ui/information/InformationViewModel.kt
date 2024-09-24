package com.example.i_postureguard.ui.exercise

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ExerciseViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is ranking Fragment"
    }
    val text: LiveData<String> = _text
}
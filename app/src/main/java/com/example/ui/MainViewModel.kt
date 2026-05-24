package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Profile
import com.example.data.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repository: ProfileRepository) : ViewModel() {

    val profile: StateFlow<Profile?> = repository.profile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _selectedDayIndex = MutableStateFlow<Long?>(null)
    val selectedDayIndex: StateFlow<Long?> = _selectedDayIndex.asStateFlow()

    private val _gridMode = MutableStateFlow("days_100") // "days_100", "days_365", "weeks"
    val gridMode: StateFlow<String> = _gridMode.asStateFlow()

    fun saveProfile(name: String, country: String, birthDate: String, lifeExpectancy: Double) {
        viewModelScope.launch {
            repository.saveProfile(
                Profile(
                    name = name,
                    country = country,
                    birthDate = birthDate,
                    lifeExpectancy = lifeExpectancy
                )
            )
            _selectedDayIndex.value = null
        }
    }

    fun deleteProfile() {
        viewModelScope.launch {
            repository.clearProfile()
            _selectedDayIndex.value = null
        }
    }

    fun selectDay(index: Long?) {
        _selectedDayIndex.value = index
    }

    fun setGridMode(mode: String) {
        _gridMode.value = mode
        _selectedDayIndex.value = null // reset selection when visual mode changes
    }
}

class MainViewModelFactory(private val repository: ProfileRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package me.kifio.kreader.android

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.kifio.kreader.android.model.Book

class SplashViewModel : ViewModel() {
    var readyState by mutableStateOf<Boolean>(false)
        private set

    fun showSplash() {
        viewModelScope.launch(context = Dispatchers.Default) {
            delay(3000L)
            withContext(context = Dispatchers.Main) {
                readyState = true
            }
        }
    }
}
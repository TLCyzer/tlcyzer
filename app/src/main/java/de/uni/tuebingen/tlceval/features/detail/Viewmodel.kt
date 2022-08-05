package de.uni.tuebingen.tlceval.features.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DetailViewModel(private val model: DetailModel) : ViewModel() {
    val captureName = model.captureName
    val imageFile = model.imagePath
    val chartData = model.references

    val chartVisible: LiveData<Boolean>
        get() = _chartVisible
    private val _chartVisible = MutableLiveData<Boolean>(false)

    suspend fun initModel(): Boolean {
        return model.initialize()
    }

    suspend fun changeNameOfCapture(name: String) {
        model.changeName(name)
    }

    fun toggleChartVisible() {
        val curState = chartVisible.value == true
        _chartVisible.value = !curState
    }
}
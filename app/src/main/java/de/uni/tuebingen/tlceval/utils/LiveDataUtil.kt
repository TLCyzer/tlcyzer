package de.uni.tuebingen.tlceval.utils

import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun <T> LiveData<T>.debounce(duration: Long = 1000L, coroutineScope: CoroutineScope) =
    MediatorLiveData<T>().also { mld ->

        val source = this
        var job: Job? = null

        mld.addSource(source) {
            job?.cancel()
            job = coroutineScope.launch {
                delay(duration)
                mld.value = source.value
            }
        }
    }

fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
    observe(lifecycleOwner, object : Observer<T> {
        override fun onChanged(t: T?) {
            observer.onChanged(t)
            removeObserver(this)
        }
    })
}

fun <T : Any?> MutableLiveData<T>.default(initialValue: T) = apply { setValue(initialValue) }
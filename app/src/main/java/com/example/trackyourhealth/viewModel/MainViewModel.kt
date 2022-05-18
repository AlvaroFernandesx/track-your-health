package com.example.trackyourhealth.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.example.trackyourhealth.model.Metric
import com.example.trackyourhealth.model.Metrics
import com.example.trackyourhealth.model.RepositoryFactory

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RepositoryFactory(application).create()

    private val _shouldRefresh = MutableLiveData(true)

    sealed class Status {

        class Failure(val e: Exception) : Status()

        class Success(val result: Result) : Status()

        object Loading : Status()
    }

    sealed class Result {
        data class MetricList(
            val value: Metrics
        ) : Result()

        data class SingleMetric(
            val value: Metric
        ) : Result()

        data class Id(
            val value: Long
        ) : Result()

        object EmptyResult : Result()
    }

    fun getMetrics() = liveData {
        try {
            emit(Status.Loading)
            emit(Status.Success(Result.MetricList(repository.getAll())))
        } catch (e: Exception) {
            emit(Status.Failure(Exception("Failed to fetch tags", e)))
        }
    }

    fun add(metric: Metric) = liveData {
        try {
            emit(Status.Loading)
            emit(Status.Success(Result.Id(repository.add(metric))))
            _shouldRefresh.postValue(true)
        } catch (e: Exception) {
            emit(Status.Failure(Exception("Failed to add element", e)))
        }
    }

    fun getById(id: Long) = liveData {
        try {
            emit(Status.Loading)
            emit(Status.Success(Result.SingleMetric(repository.getById(id))))
        } catch (e: Exception) {
            emit(Status.Failure(Exception("Failed to get element by id", e)))
        }
    }

    fun remove(id: Long) = liveData {
        try {
            emit(Status.Loading)
            repository.removeById(id)
            emit(Status.Success(Result.EmptyResult))
            _shouldRefresh.postValue(true)
        } catch (e: Exception) {
            emit(Status.Failure(Exception("Failed to remove element", e)))
        }
    }

    fun update(metric: Metric) = liveData {
        try {
            emit(Status.Loading)
            repository.update(metric)
            emit(Status.Success(Result.EmptyResult))
            _shouldRefresh.postValue(true)
        } catch (e: Exception) {
            emit(Status.Failure(Exception("Failed to update element", e)))
        }
    }

}
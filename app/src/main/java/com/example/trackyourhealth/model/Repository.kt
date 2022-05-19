package com.example.trackyourhealth.model

interface Repository {

    suspend fun getAll(): Metrics

    suspend fun getById(id: Long): Metric

    suspend fun update(metric: Metric)

    suspend fun add(metric: Metric): Long

    suspend fun removeById(id: Long)

    suspend fun removeAll()
}
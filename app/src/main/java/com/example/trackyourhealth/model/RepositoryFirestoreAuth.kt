package com.example.trackyourhealth.model

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class RepositoryFirestoreAuth(application: Application) : Repository {
    private val db: FirebaseFirestore = Firebase.firestore
    private val isConnected = AtomicBoolean(true)

    companion object {
        private const val metricsCollection = "metrics_auth"
        private const val metricIdDoc = "metricId"

        private object MetricDoc {
            const val id = "id"
            const val user = "user"
        }
    }

    private data class MetricFirestore(
        val id: Long? = null,
        val title: String? = null,
        val listQualitativeMetric: List<DailyRecords>? = null,
        val user: String? = null
    ) {
        fun toMetric() = Metric(
            id = id ?: 0,
            title = title ?: "",
            listQualitativeMetric = listQualitativeMetric ?: emptyList()
        )

        companion object {
            fun fromMetric(metric: Metric, user: String) = MetricFirestore(
                id = metric.id,
                title = metric.title,
                listQualitativeMetric = metric.listQualitativeMetric,
                user = user
            )
        }
    }

    private data class MetricId(
        val value: Long? = null
    )

    init {
        application.applicationContext.getSystemService(ConnectivityManager::class.java).apply {
            val connected = getNetworkCapabilities(activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ?: false

            isConnected.set(connected)
            registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    isConnected.set(true)
                }

                override fun onLost(network: Network) {
                    isConnected.set(false)
                }
            })
        }
    }

    private fun getSource() = if (isConnected.get()) Source.DEFAULT else Source.CACHE

    private fun getCurrentUser(): String = FirebaseAuth.getInstance().currentUser?.uid
        ?: throw Exception("No user is signed in")

    private fun getCollection() = db.collection(metricsCollection)

    override suspend fun getAll(): Metrics = getCollection()
        .whereEqualTo(MetricDoc.user, getCurrentUser())
        .get(getSource())
        .await()
        .toObjects(MetricFirestore::class.java).map { it.toMetric() }

    override suspend fun getById(id: Long): Metric = getCollection()
        .whereEqualTo(MetricDoc.user, getCurrentUser())
        .whereEqualTo(MetricDoc.id, id)
        .get(getSource())
        .await()
        .toObjects(MetricFirestore::class.java)
        .first()
        .toMetric()

    override suspend fun update(metric: Metric) {
        getCollection()
            .whereEqualTo(MetricDoc.user, getCurrentUser())
            .whereEqualTo(MetricDoc.id, metric.id)
            .get(getSource())
            .await()
            .let { querySnapshot ->
                if (querySnapshot.isEmpty)
                    throw Exception("Failed to update element with non-existing id ${metric.id}")
                querySnapshot.first().reference.set(MetricFirestore.fromMetric(metric, getCurrentUser()))
            }
    }

    override suspend fun add(metric: Metric): Long = MetricFirestore(
        id = nextId(),
        title = metric.title,
        listQualitativeMetric = metric.listQualitativeMetric,
        user = getCurrentUser()
    ).let {
        getCollection().add(it)
        it.id ?: throw Exception("Failed to add element with a valid id")
    }

    override suspend fun removeById(id: Long) {
        getCollection()
            .whereEqualTo(MetricDoc.user, getCurrentUser())
            .whereEqualTo(MetricDoc.id, id)
            .get(getSource())
            .await()
            .let { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    throw Exception("Failed to remove element with non-existing id $id")
                }
                querySnapshot.first().reference.delete()
            }
    }

    override suspend fun removeAll() {
        getCollection()
            .whereEqualTo(MetricDoc.user, getCurrentUser())
            .get(getSource())
            .await()
            .forEach { queryDocumentSnapshot ->
                queryDocumentSnapshot.reference.delete()
            }
    }

    private suspend fun nextId(): Long = getCollection()
        .document(metricIdDoc)
        .get(getSource())
        .await()
        .let { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val oldValue = documentSnapshot.toObject(MetricId::class.java)?.value
                    ?: throw Exception("Failed to retrieve previous id")
                MetricId(oldValue + 1)
            } else {
                MetricId(1)
            }.let { newMetricId ->
                documentSnapshot.reference.set(newMetricId)
                newMetricId.value ?: throw Exception("New id should not be null")
            }
        }
}
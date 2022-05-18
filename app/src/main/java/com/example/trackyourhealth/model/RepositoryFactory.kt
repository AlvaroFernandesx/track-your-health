package com.example.trackyourhealth.model

import android.app.Application

class RepositoryFactory(private val application: Application) {

    enum class Type {
        FirestoreAuth
    }

    fun create(type: Type = Type.FirestoreAuth) = when (type) {
        Type.FirestoreAuth -> RepositoryFirestoreAuth(application)
    }
}
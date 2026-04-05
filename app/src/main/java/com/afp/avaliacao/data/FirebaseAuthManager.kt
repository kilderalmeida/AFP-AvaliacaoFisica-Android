package com.afp.avaliacao.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseAuthManager(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun login(email: String, password: String): FirebaseUser {
        val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        return result.user ?: throw Exception("Falha no login")
    }

    suspend fun getUserRole(uid: String): String {
        return try {
            val document = firestore.collection("users").document(uid).get().await()
            document.getString("papel") ?: "atleta" // Default para atleta se não houver papel
        } catch (e: Exception) {
            "atleta"
        }
    }

    suspend fun getUserName(uid: String): String {
        return try {
            val document = firestore.collection("users").document(uid).get().await()
            document.getString("nome") ?: "Usuário"
        } catch (e: Exception) {
            "Usuário"
        }
    }

    fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser

    fun logout() {
        firebaseAuth.signOut()
    }
}

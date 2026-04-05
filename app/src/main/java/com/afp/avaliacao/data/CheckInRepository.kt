package com.afp.avaliacao.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.*

data class SessionData(
    val id: String = "",
    val athleteId: String = "",
    val dataCheckin: Date = Date(),
    val dataCheckout: Date? = null,
    val atividades: List<String> = emptyList(),
    val vfc: Int = 0,
    val bemEstar: Map<String, Int> = emptyMap(),
    val recuperacao: String = "",
    val dorRegioes: List<String> = emptyList(),
    val hidratacao: Int = 1,
    val pseFoster: Int? = null,
    val duracaoMin: Int? = null,
    val carga: Int? = null
)

class SessionRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun hasCheckInToday(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.time

        return try {
            val result = firestore.collection("sessaotreino")
                .whereEqualTo("athleteId", uid)
                .whereGreaterThanOrEqualTo("dataCheckin", startOfDay)
                .limit(1)
                .get()
                .await()
            !result.isEmpty
        } catch (e: Exception) {
            Log.e("SessionRepository", "Erro ao verificar check-in hoje", e)
            false
        }
    }

    suspend fun salvarCheckIn(data: SessionData): Result<Unit> {
        return try {
            if (hasCheckInToday()) throw Exception("Check-in já realizado hoje.")
            
            val uid = auth.currentUser?.uid ?: throw Exception("Usuário não autenticado")
            val checkInFinal = data.copy(athleteId = uid, dataCheckin = Date())
            
            firestore.collection("sessaotreino")
                .add(checkInFinal)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SessionRepository", "Erro ao salvar check-in", e)
            Result.failure(e)
        }
    }

    suspend fun getUltimaSessaoHoje(): SessionData? {
        val uid = auth.currentUser?.uid ?: return null
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.time

        return try {
            val result = firestore.collection("sessaotreino")
                .whereEqualTo("athleteId", uid)
                .whereGreaterThanOrEqualTo("dataCheckin", startOfDay)
                .orderBy("dataCheckin", Query.Direction.DESCENDING)
                .get()
                .await()

            result.documents.mapNotNull { doc ->
                val dataCheckout = doc.getTimestamp("dataCheckout")?.toDate()
                if (dataCheckout != null) return@mapNotNull null // Já finalizada

                try {
                    SessionData(
                        id = doc.id,
                        athleteId = doc.getString("athleteId") ?: "",
                        dataCheckin = doc.getTimestamp("dataCheckin")?.toDate() ?: Date(),
                        dataCheckout = dataCheckout,
                        atividades = doc.get("atividades") as? List<String> ?: emptyList(),
                        vfc = doc.getLong("vfc")?.toInt() ?: 0,
                        bemEstar = doc.get("bemEstar") as? Map<String, Int> ?: emptyMap(),
                        recuperacao = doc.getString("recuperacao") ?: "",
                        dorRegioes = doc.get("dorRegioes") as? List<String> ?: emptyList(),
                        hidratacao = doc.getLong("hidratacao")?.toInt() ?: 1,
                        pseFoster = doc.getLong("pseFoster")?.toInt(),
                        duracaoMin = doc.getLong("duracaoMin")?.toInt(),
                        carga = doc.getLong("carga")?.toInt()
                    )
                } catch (e: Exception) {
                    null
                }
            }.firstOrNull()
        } catch (e: Exception) {
            Log.e("SessionRepository", "Erro ao buscar última sessão", e)
            throw e
        }
    }

    suspend fun salvarCheckOut(id: String, pse: Int, duracao: Int): Result<Unit> {
        return try {
            val carga = pse * duracao
            val update = mapOf(
                "dataCheckout" to Date(),
                "pseFoster" to pse,
                "duracaoMin" to duracao,
                "carga" to carga
            )
            firestore.collection("sessaotreino").document(id).update(update).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SessionRepository", "Erro ao salvar checkout", e)
            Result.failure(e)
        }
    }
}

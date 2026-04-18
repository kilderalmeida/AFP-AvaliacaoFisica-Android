package com.afp.avaliacao.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
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
    suspend fun getSessaoAberta(): SessionData? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            // Buscamos as últimas sessões sem filtro de checkout no Firestore para evitar problemas de índice
            val result = firestore.collection("sessaotreino")
                .whereEqualTo("athleteId", uid)
                .orderBy("dataCheckin", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .await()

            // Filtramos na memória a que não tem dataCheckout e nem pseFoster
            result.documents.mapNotNull { doc ->
                val checkout = doc.get("dataCheckout")
                val pse = doc.get("pseFoster")
                
                if (checkout == null && pse == null) {
                    SessionData(
                        id = doc.id,
                        athleteId = doc.getString("athleteId") ?: "",
                        dataCheckin = doc.getTimestamp("dataCheckin")?.toDate() ?: Date(),
                        dataCheckout = null,
                        atividades = (doc.get("atividades") as? List<*>)?.map { it.toString() } ?: emptyList(),
                        vfc = doc.getLong("vfc")?.toInt() ?: 0,
                        bemEstar = (doc.get("bemEstar") as? Map<*, *>)?.map { it.key.toString() to it.value.toString().toInt() }?.toMap() ?: emptyMap(),
                        recuperacao = doc.getString("recuperacao") ?: "",
                        dorRegioes = (doc.get("dorRegioes") as? List<*>)?.map { it.toString() } ?: emptyList(),
                        hidratacao = doc.getLong("hidratacao")?.toInt() ?: 1
                    )
                } else null
            }.firstOrNull()
        } catch (e: Exception) {
            Log.e("SessionRepository", "Erro ao buscar sessão aberta", e)
            null
        }
    }

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
            // Verificamos se existe QUALQUER check-in hoje
            val result = firestore.collection("sessaotreino")
                .whereEqualTo("athleteId", uid)
                .whereGreaterThanOrEqualTo("dataCheckin", startOfDay)
                .get()
                .await()
            
            // Se houver algum check-in hoje, mas ele ainda estiver aberto (sem checkout),
            // permitimos que o usuário faça o checkout primeiro.
            // A trava de "novo check-in" só deve impedir se JÁ existir um check-in FINALIZADO hoje
            // OU se existir um check-in ABERTO (independente do dia).
            
            val temSessaoFinalizadaHoje = result.documents.any { it.get("dataCheckout") != null }
            temSessaoFinalizadaHoje
        } catch (e: Exception) {
            Log.e("SessionRepository", "Erro ao verificar check-in hoje", e)
            false
        }
    }

    suspend fun salvarCheckIn(data: SessionData): Result<Unit> {
        return try {
            val sessaoAberta = getSessaoAberta()
            if (sessaoAberta != null) {
                throw Exception("Você possui um treino em aberto (${SimpleDateFormat("dd/MM").format(sessaoAberta.dataCheckin)}). Faça o checkout antes de iniciar um novo.")
            }

            if (hasCheckInToday()) {
                throw Exception("Você já realizou um treino hoje.")
            }
            
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
        return getSessaoAberta()
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

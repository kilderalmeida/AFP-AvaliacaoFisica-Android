package com.afp.avaliacao.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.*

data class Atleta(
    val id: String, 
    val nome: String, 
    val email: String = "", 
    val ativo: Boolean = true,
    val coachId: String = "",
    val treinadorId: String? = null,
    val coachNome: String? = null,
    val coachEmail: String? = null,
    val treinadorNome: String? = null,
    val treinadorEmail: String? = null
)

data class Treinador(
    val id: String,
    val nome: String,
    val email: String = "",
    val coachId: String = ""
)

class MetricsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun getMeusAtletas(): List<Atleta> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            val asCoach = firestore.collection("users")
                .whereEqualTo("coach_id", uid)
                .whereEqualTo("papel", "atleta")
                .get()
                .await()
            
            val asTreinador = firestore.collection("users")
                .whereEqualTo("treinador_id", uid)
                .whereEqualTo("papel", "atleta")
                .get()
                .await()

            val allDocs = (asCoach.documents + asTreinador.documents).distinctBy { it.id }

            allDocs.map { doc ->
                Atleta(
                    id = doc.id,
                    nome = doc.getString("nome") ?: "Atleta Desconhecido",
                    email = doc.getString("email") ?: "",
                    ativo = doc.getBoolean("ativo") ?: true,
                    coachId = doc.getString("coach_id") ?: "",
                    treinadorId = doc.getString("treinador_id"),
                    coachNome = doc.getString("coach_nome"),
                    coachEmail = doc.getString("coach_email"),
                    treinadorNome = doc.getString("treinador_nome"),
                    treinadorEmail = doc.getString("treinador_email")
                )
            }
        } catch (e: Exception) {
            Log.e("MetricsRepository", "Erro ao buscar atletas", e)
            emptyList()
        }
    }

    suspend fun getTodosAtletasDoSistema(): List<Atleta> {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("papel", "atleta")
                .get()
                .await()
            snapshot.documents.map { doc ->
                Atleta(
                    id = doc.id,
                    nome = doc.getString("nome") ?: "Atleta Desconhecido",
                    email = doc.getString("email") ?: "",
                    ativo = doc.getBoolean("ativo") ?: true,
                    coachId = doc.getString("coach_id") ?: "",
                    treinadorId = doc.getString("treinador_id"),
                    coachNome = doc.getString("coach_nome"),
                    coachEmail = doc.getString("coach_email"),
                    treinadorNome = doc.getString("treinador_nome"),
                    treinadorEmail = doc.getString("treinador_email")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTreinadores(): List<Treinador> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("coach_id", uid)
                .whereEqualTo("papel", "treinador")
                .get()
                .await()
            snapshot.documents.map { doc ->
                Treinador(
                    id = doc.id,
                    nome = doc.getString("nome") ?: "Treinador Desconhecido",
                    email = doc.getString("email") ?: "",
                    coachId = doc.getString("coach_id") ?: ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun cadastrarTreinador(nome: String, email: String): Result<Unit> {
        val coachUid = auth.currentUser?.uid ?: return Result.failure(Exception("Não autorizado"))
        return try {
            val coachName = auth.currentUser?.displayName ?: "Coach"
            val coachEmail = auth.currentUser?.email ?: ""

            val novoTreinador = hashMapOf(
                "nome" to nome,
                "email" to email,
                "papel" to "treinador",
                "coach_id" to coachUid,
                "coach_nome" to coachName,
                "coach_email" to coachEmail,
                "limiteAtletas" to 5,
                "dataCadastro" to com.google.firebase.Timestamp.now()
            )
            firestore.collection("users").add(novoTreinador).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCoachLimit(): Int {
        val uid = auth.currentUser?.uid ?: return 0
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            doc.getLong("limiteAtletas")?.toInt() ?: 5
        } catch (e: Exception) {
            5
        }
    }

    suspend fun cadastrarAtleta(nome: String, email: String): Result<Unit> {
        val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Não logado"))
        return try {
            val coachName = auth.currentUser?.displayName ?: "Coach"
            val coachEmail = auth.currentUser?.email ?: ""
            
            val limite = getCoachLimit()
            val atletasAtuais = getMeusAtletas().count { it.ativo }
            
            if (atletasAtuais >= limite) {
                return Result.failure(Exception("Limite de atletas ativos atingido ($limite)."))
            }
            
            val novoAtleta = hashMapOf(
                "nome" to nome,
                "email" to email,
                "papel" to "atleta",
                "coach_id" to currentUid,
                "coach_nome" to coachName,
                "coach_email" to coachEmail,
                "ativo" to true,
                "dataCadastro" to com.google.firebase.Timestamp.now()
            )
            firestore.collection("users").add(novoAtleta).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleAtletaAtivo(atletaId: String, novoStatus: Boolean): Result<Unit> {
        return try {
            if (novoStatus) {
                val limite = getCoachLimit()
                val atletasAtuais = getMeusAtletas().count { it.ativo }
                if (atletasAtuais >= limite) return Result.failure(Exception("Limite atingido"))
            }
            firestore.collection("users").document(atletaId).update("ativo", novoStatus).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSessoesTreino(atletaId: String, dias: Int): List<Map<String, Any>> {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -dias)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dataInicio = calendar.time
        return try {
            val snapshot = firestore.collection("sessaotreino")
                .whereEqualTo("athleteId", atletaId)
                .whereGreaterThanOrEqualTo("dataCheckin", dataInicio)
                .orderBy("dataCheckin", Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.documents.map { it.data ?: emptyMap() }
        } catch (e: Exception) {
            filtrarManualmente(atletaId, dataInicio)
        }
    }

    private suspend fun filtrarManualmente(atletaId: String, dataInicio: Date): List<Map<String, Any>> {
        return try {
            val snapshot = firestore.collection("sessaotreino").whereEqualTo("athleteId", atletaId).get().await()
            snapshot.documents.mapNotNull { it.data }.filter { 
                val data = (it["dataCheckin"] as? com.google.firebase.Timestamp)?.toDate()
                data != null && data.after(dataInicio)
            }.sortedByDescending { (it["dataCheckin"] as? com.google.firebase.Timestamp)?.toDate() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

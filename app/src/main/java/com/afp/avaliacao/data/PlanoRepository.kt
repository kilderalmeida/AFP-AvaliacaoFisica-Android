package com.afp.avaliacao.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.*

data class PlanoTreino(
    val id: String = "",
    val coachId: String = "",
    val atletaId: String = "",
    val dataCriacao: Date = Date(),
    val periodo: Map<String, List<DiaTreino>> = emptyMap(),
    val status: String = "ativo"
)

data class DiaTreino(
    val dia: String = "",
    val modalidade: String = "",
    val cargaAlvo: Int = 0,
    val duracao: Int = 0,
    val descricao: String = ""
)

class PlanoRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun salvarPlano(plano: PlanoTreino): Result<Unit> {
        return try {
            firestore.collection("planosTreino")
                .add(plano)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUltimoPlanoAtleta(atletaId: String): PlanoTreino? {
        return try {
            val snapshot = firestore.collection("planosTreino")
                .whereEqualTo("atletaId", atletaId)
                .whereEqualTo("status", "ativo")
                .orderBy("dataCriacao", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            
            snapshot.documents.firstOrNull()?.toObject(PlanoTreino::class.java)
        } catch (e: Exception) {
            null
        }
    }
}

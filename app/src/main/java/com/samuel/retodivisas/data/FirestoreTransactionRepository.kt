package com.samuel.retodivisas.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.math.round

class FirestoreTransactionRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun observeUser(uid: String): Flow<AppUser?> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(AppUser::class.java)?.copy(uid = uid)
                if (user != null) {
                    SessionManager.currentUser = user
                }
                trySend(user)
            }
        awaitClose { listener.remove() }
    }

    fun observeTransactionsByUser(uid: String): Flow<List<TransactionModel>> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .collection("transactions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(TransactionModel::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.timestamp } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Observa todas las transacciones de todos los usuarios en tiempo real.
     * Ordena en memoria por `timestamp` descendente para evitar que Firestore
     * exija un índice compuesto manual en `collectionGroup`.
     */
    fun observeAllTransactions(): Flow<List<TransactionModel>> = callbackFlow {
        val listener = firestore.collectionGroup("transactions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(TransactionModel::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.timestamp } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun executeTransaction(
        uid: String,
        currency: String,
        amountUsd: Double,
        agreedRate: Double,
        marketRate: Double,
        type: String,
        photoPath: String? = null
    ): Result<TransactionModel> = runCatching {
        val userRef = firestore.collection("users").document(uid)
        val pair = "USD/$currency"

        val (deviation, requiresAudit) = AuditRule.evaluate(agreedRate, marketRate)

        val newTx = firestore.runTransaction { transaction ->
            val userDoc = transaction.get(userRef)
            if (!userDoc.exists()) {
                throw IllegalStateException("Usuario no encontrado")
            }

            val currentBalance = userDoc.getDouble("balanceUsd") ?: 0.0
            val email = userDoc.getString("email") ?: ""

            val newBalance = if (type == TYPE_SELL) {
                if (currentBalance < amountUsd) {
                    throw IllegalStateException("Balance insuficiente")
                }
                currentBalance - amountUsd
            } else {
                currentBalance + amountUsd
            }

            val roundedBalance = round(newBalance * 100) / 100.0
            val txRef = userRef.collection("transactions").document()

            val tx = TransactionModel(
                id = txRef.id,
                userId = uid,
                currencyPair = pair,
                amount = amountUsd,
                exchangeRate = agreedRate,
                agreedRate = agreedRate,
                marketRate = marketRate,
                transactionType = type,
                timestamp = System.currentTimeMillis(),
                requiresAudit = requiresAudit,
                deviationPercent = deviation,
                photoPath = photoPath,
                email = email
            )

            transaction.update(userRef, "balanceUsd", roundedBalance)
            transaction.set(txRef, tx)

            tx
        }.await()

        val updatedUserBalance = if (type == TYPE_SELL) {
            round(((SessionManager.currentUser?.balanceUsd ?: 0.0) - amountUsd) * 100) / 100.0
        } else {
            round(((SessionManager.currentUser?.balanceUsd ?: 0.0) + amountUsd) * 100) / 100.0
        }
        SessionManager.currentUser = SessionManager.currentUser?.copy(balanceUsd = updatedUserBalance)

        newTx
    }

    suspend fun updateProfilePhoto(uid: String, photoPath: String) {
        firestore.collection("users").document(uid)
            .update("photoPath", photoPath)
            .await()
        SessionManager.currentUser = SessionManager.currentUser?.copy(photoPath = photoPath)
    }
}

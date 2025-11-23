package com.example.portableevm.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Entity(tableName = "elections")
data class ElectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startTimestamp: Long,
    val endTimestamp: Long? = null,
    val isCompleted: Boolean = false,
)

@Entity(
    tableName = "candidates",
    foreignKeys = [
        ForeignKey(
            entity = ElectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["electionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("electionId")]
)
data class CandidateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val electionId: Long,
    val name: String,
    val buttonNumber: Int,
    val votes: Int = 0,
)

@Entity(tableName = "admin_settings")
data class AdminSettingsEntity(
    @PrimaryKey val id: Int = 0,
    val password: String?,
    val requirePasswordForNewElection: Boolean,
)

data class ElectionWithCandidates(
    @Embedded val election: ElectionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "electionId"
    )
    val candidates: List<CandidateEntity>,
)

@Dao
interface ElectionDao {
    @Insert
    suspend fun insertElection(election: ElectionEntity): Long

    @Insert
    suspend fun insertCandidates(candidates: List<CandidateEntity>)

    @Transaction
    @Query("SELECT * FROM elections WHERE isCompleted = 0 LIMIT 1")
    fun observeActiveElection(): Flow<ElectionWithCandidates?>

    @Transaction
    @Query("SELECT * FROM elections ORDER BY startTimestamp DESC")
    fun observeElections(): Flow<List<ElectionWithCandidates>>

    @Transaction
    @Query("SELECT * FROM elections WHERE id = :id")
    fun observeElection(id: Long): Flow<ElectionWithCandidates?>

    @Query("UPDATE candidates SET votes = votes + 1 WHERE electionId = :electionId AND buttonNumber = :buttonNumber")
    suspend fun incrementVote(electionId: Long, buttonNumber: Int)

    @Query("UPDATE elections SET isCompleted = 1, endTimestamp = :endTimestamp WHERE id = :electionId")
    suspend fun completeElection(electionId: Long, endTimestamp: Long)
}

@Dao
interface AdminSettingsDao {
    @Query("SELECT * FROM admin_settings WHERE id = 0")
    fun observeSettings(): Flow<AdminSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: AdminSettingsEntity)
}

@Database(
    entities = [ElectionEntity::class, CandidateEntity::class, AdminSettingsEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class EvmDatabase : RoomDatabase() {
    abstract fun electionDao(): ElectionDao
    abstract fun adminSettingsDao(): AdminSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: EvmDatabase? = null

        fun getInstance(context: Context): EvmDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    EvmDatabase::class.java,
                    "portable_evm_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

class ElectionRepository(private val electionDao: ElectionDao) {
    fun observeActiveElection(): Flow<ElectionWithCandidates?> = electionDao.observeActiveElection()
    fun observeElections(): Flow<List<ElectionWithCandidates>> = electionDao.observeElections()
    fun observeElection(id: Long): Flow<ElectionWithCandidates?> = electionDao.observeElection(id)

    suspend fun startNewElection(name: String, candidates: List<Pair<String, Int>>): Long {
        val electionId = electionDao.insertElection(
            ElectionEntity(
                name = name,
                startTimestamp = System.currentTimeMillis(),
            )
        )
        val candidateEntities = candidates.map { (candidateName, buttonNumber) ->
            CandidateEntity(
                electionId = electionId,
                name = candidateName,
                buttonNumber = buttonNumber,
            )
        }
        electionDao.insertCandidates(candidateEntities)
        return electionId
    }

    suspend fun registerVote(electionId: Long, buttonNumber: Int) {
        electionDao.incrementVote(electionId, buttonNumber)
    }

    suspend fun endElection(electionId: Long) {
        electionDao.completeElection(electionId, System.currentTimeMillis())
    }
}

class AdminRepository(private val adminDao: AdminSettingsDao) {
    fun observeSettings(): Flow<AdminSettingsEntity?> = adminDao.observeSettings()

    suspend fun setPassword(password: String?, requireForNewElection: Boolean) {
        val settings = AdminSettingsEntity(
            id = 0,
            password = password,
            requirePasswordForNewElection = requireForNewElection,
        )
        adminDao.upsert(settings)
    }

    /**
     * Ensure there is an admin settings row. If missing, create one with a default password.
     */
    suspend fun ensureDefaultSettingsIfMissing(defaultPassword: String = "1234") {
        val current = adminDao.observeSettings().first()
        if (current == null) {
            val settings = AdminSettingsEntity(
                id = 0,
                password = defaultPassword,
                requirePasswordForNewElection = true,
            )
            adminDao.upsert(settings)
        }
    }
}

package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {

    @Query("SELECT * FROM transfer_tasks ORDER BY timestamp DESC")
    fun getAllTasks(): Flow<List<TransferTaskEntity>>

    @Query("SELECT * FROM transfer_tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): TransferTaskEntity?

    @Query("SELECT * FROM transfer_files WHERE taskId = :taskId ORDER BY id ASC")
    fun getFilesForTask(taskId: Long): Flow<List<TransferFileEntity>>

    @Query("SELECT * FROM transfer_files WHERE taskId = :taskId ORDER BY id ASC")
    suspend fun getFileListForTask(taskId: Long): List<TransferFileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TransferTaskEntity): Long

    @Update
    suspend fun updateTask(task: TransferTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<TransferFileEntity>)

    @Update
    suspend fun updateFile(file: TransferFileEntity)

    @Query("UPDATE transfer_tasks SET status = :status WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: Long, status: TransferStatus)

    @Query("UPDATE transfer_files SET status = :status WHERE id = :fileId")
    suspend fun updateFileStatus(fileId: Long, status: TransferStatus)

    @Query("UPDATE transfer_files SET transferredBytes = :transferredBytes, status = :status WHERE id = :fileId")
    suspend fun updateFileProgress(fileId: Long, transferredBytes: Long, status: TransferStatus)

    @Query("UPDATE transfer_tasks SET transferredBytes = :transferredBytes, completedFiles = :completedFiles, status = :status WHERE id = :taskId")
    suspend fun updateTaskProgress(taskId: Long, transferredBytes: Long, completedFiles: Int, status: TransferStatus)

    @Delete
    suspend fun deleteTask(task: TransferTaskEntity)

    @Query("DELETE FROM transfer_files WHERE taskId = :taskId")
    suspend fun deleteFilesForTask(taskId: Long)

    @Query("SELECT * FROM p2p_peers ORDER BY lastConnected DESC")
    fun getAllPeers(): Flow<List<PeerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeer(peer: PeerEntity)
}

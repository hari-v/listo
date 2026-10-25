package com.hariv.listo.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos WHERE archivedAt IS NULL")
    fun observeActive(): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE archivedAt IS NULL")
    suspend fun active(): List<Todo>

    @Query("SELECT * FROM todos WHERE archivedAt IS NOT NULL ORDER BY archivedAt DESC")
    fun observeArchived(): Flow<List<Todo>>

    @Query("SELECT * FROM todos")
    suspend fun all(): List<Todo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: Todo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(todos: List<Todo>)

    // In an UPDATE, every expression reads the row's old values, so both CASEs see the old "done".
    @Query(
        "UPDATE todos SET done = CASE WHEN done = 1 THEN 0 ELSE 1 END, " +
            "completedAt = CASE WHEN done = 1 THEN NULL ELSE :now END " +
            "WHERE id = :id AND archivedAt IS NULL"
    )
    suspend fun toggle(id: String, now: Long): Int

    @Query("UPDATE todos SET category = :category WHERE id = :id")
    suspend fun setCategory(id: String, category: String): Int

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun delete(id: String): Int

    @Query("UPDATE todos SET archivedAt = :now, archivedBy = 'cleared' WHERE done = 1 AND archivedAt IS NULL")
    suspend fun clearCompleted(now: Long): Int

    @Query(
        "UPDATE todos SET archivedAt = :now, archivedBy = 'auto' " +
            "WHERE done = 1 AND archivedAt IS NULL AND completedAt IS NOT NULL AND completedAt < :cutoff"
    )
    suspend fun autoArchive(now: Long, cutoff: Long): Int

    @Query("DELETE FROM todos")
    suspend fun deleteAll()

    @Query("UPDATE todos SET category = :to WHERE category = :from")
    suspend fun renameCategory(from: String, to: String): Int

    @Query("UPDATE todos SET category = :to WHERE category = :from AND archivedAt IS NULL")
    suspend fun moveActiveCategory(from: String, to: String): Int

    @Query("SELECT COUNT(*) FROM todos WHERE category = :category AND archivedAt IS NULL")
    suspend fun countActive(category: String): Int
}

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules ORDER BY createdAt")
    fun observeAll(): Flow<List<Rule>>

    @Query("SELECT * FROM rules")
    suspend fun all(): List<Rule>

    @Query("SELECT * FROM rules WHERE id = :id")
    suspend fun get(id: String): Rule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: Rule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<Rule>)

    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE rules SET paused = :paused WHERE id = :id")
    suspend fun setPaused(id: String, paused: Boolean)

    @Query("UPDATE rules SET lastDueDate = :date WHERE id = :id")
    suspend fun setLastDue(id: String, date: String)

    @Query("DELETE FROM rules")
    suspend fun deleteAll()

    @Query("UPDATE rules SET category = :to WHERE category = :from")
    suspend fun renameCategory(from: String, to: String): Int

    @Query("SELECT COUNT(*) FROM rules WHERE category = :category")
    suspend fun count(category: String): Int
}

@Database(entities = [Todo::class, Rule::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todos(): TodoDao
    abstract fun rules(): RuleDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "todo.db").build()
    }
}

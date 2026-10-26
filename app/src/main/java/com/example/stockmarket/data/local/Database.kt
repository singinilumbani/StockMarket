package com.example.stockmarket.data.local


import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.example.stockmarket.data.model.Company
import com.example.stockmarket.data.remote.CompanyDto
import kotlinx.coroutines.flow.Flow

// One row of the "companies" table
@Entity(tableName = "companies")
data class CompanyEntity(
    @PrimaryKey val symbol: String,
    val name: String,
    val sector: String,
    val price: Double,
    val changePct: Double,
    val updatedAt: String,
    val historyCsv: String        // the 30 closing prices joined with commas: "2051.07,2009.78,..."
)

@Dao
abstract class CompanyDao {

    // A Flow: it sends the whole list again every time the table changes
    @Query("SELECT * FROM companies")
    abstract fun observeAll(): Flow<List<CompanyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(items: List<CompanyEntity>)

    @Query("DELETE FROM companies")
    abstract suspend fun deleteAll()

    // All or nothing: if the insert fails, the delete is undone too
    @Transaction
    open suspend fun replaceAll(items: List<CompanyEntity>) {
        deleteAll()
        insertAll(items)
    }
}

@Database(entities = [CompanyEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun companyDao(): CompanyDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        // One shared database for the whole app
        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mse_invest.db"
                ).build().also { instance = it }
            }
    }
}

// ---------- Converting between the three shapes of a company ----------

// From the server's shape to a table row
fun CompanyDto.toEntity() = CompanyEntity(
    symbol = symbol,
    name = name,
    sector = sector,
    price = price,
    changePct = changePct,
    updatedAt = updatedAt,
    historyCsv = history.joinToString(",")
)

// From a table row to the shape our screens use
fun CompanyEntity.toCompany() = Company(
    symbol = symbol,
    name = name,
    sector = sector,
    price = price,
    changePct = changePct,
    history = historyCsv
        .split(",")
        .mapNotNull { it.toDoubleOrNull() }
)
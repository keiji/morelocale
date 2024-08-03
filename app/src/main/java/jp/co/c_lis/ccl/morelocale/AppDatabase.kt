package jp.co.c_lis.ccl.morelocale

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import jp.co.c_lis.ccl.morelocale.dao.LocaleIsoItemDao
import jp.co.c_lis.ccl.morelocale.dao.LocaleItemDao
import jp.co.c_lis.ccl.morelocale.entity.Converters
import jp.co.c_lis.ccl.morelocale.entity.LocaleIsoItem
import jp.co.c_lis.ccl.morelocale.entity.LocaleItem

@Database(
        entities = [LocaleItem::class, LocaleIsoItem::class],
        version = 3
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun localeItemDao(): LocaleItemDao

    abstract fun localeIsoItemDao(): LocaleIsoItemDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `LocaleIsoItem`" +
                " (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT, `label` TEXT NOT NULL, `value` TEXT NOT NULL)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DELETE FROM `LocaleItem`")
        database.execSQL("ALTER TABLE `LocaleItem` ADD COLUMN `script` TEXT")
    }
}

package com.bliss.aimemorysearch.db;

import androidx.annotation.VisibleForTesting;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public final class EmailMigrations {
    private EmailMigrations() {}

    public static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            applyV9(database::execSQL);
        }
    };

    @VisibleForTesting
    public static void applyV9(SqlExecutor database) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `emails` ("
                + "`id` TEXT NOT NULL, `provider` TEXT NOT NULL, "
                + "`account` TEXT NOT NULL, `messageId` TEXT NOT NULL, "
                + "`threadId` TEXT, `subject` TEXT, `sender` TEXT, "
                + "`toRecipients` TEXT, `ccRecipients` TEXT, `body` TEXT, "
                + "`timestamp` INTEGER NOT NULL, `attachmentCount` INTEGER NOT NULL, "
                + "`indexedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS "
                + "`index_emails_provider_account_messageId` ON `emails` "
                + "(`provider`, `account`, `messageId`)");
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_emails_threadId` "
                + "ON `emails` (`threadId`)");
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_emails_timestamp` "
                + "ON `emails` (`timestamp`)");
        database.execSQL("ALTER TABLE `chunks` ADD COLUMN `sourceType` TEXT");
        database.execSQL("ALTER TABLE `chunks` ADD COLUMN `sourceId` TEXT");
    }

    public interface SqlExecutor {
        void execSQL(String sql);
    }
}

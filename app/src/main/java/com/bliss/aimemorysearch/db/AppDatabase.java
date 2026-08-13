package com.bliss.aimemorysearch.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.bliss.aimemorysearch.db.ChunkEntity;
import com.bliss.aimemorysearch.db.TokenIndexEntity;
import com.bliss.aimemorysearch.db.TokenIndexDao;
@Database(
        entities = {
                FileEntity.class,
                FavoriteEntity.class,
                ChunkEntity.class,
                TokenIndexEntity.class,
                EmailEntity.class
        },
        version = 9
)
public abstract class AppDatabase
        extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract FileDao fileDao();
    public abstract ChunkDao chunkDao();
    public abstract TokenIndexDao tokenIndexDao();
    public abstract FavoriteDao favoriteDao();
    public abstract EmailDao emailDao();

    public static AppDatabase getInstance(
            Context context
    ) {

        if (INSTANCE == null) {

            synchronized (AppDatabase.class) {

                if (INSTANCE == null) {

                    INSTANCE =
                            Room.databaseBuilder(
                                            context.getApplicationContext(),
                                            AppDatabase.class,
                                            "ai_memory_db"
                                    )
                                    .addMigrations(EmailMigrations.MIGRATION_8_9)
                                    .build();
                }
            }
        }

        return INSTANCE;
    }
}

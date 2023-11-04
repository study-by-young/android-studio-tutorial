package com.example.todolist.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = arrayOf(TodoEntity::class), version = 1) // 조건 1
abstract class AppDatabase : RoomDatabase() { // 조건 2

    abstract fun getTodoDao() : TodoDao // 조건 3

    companion object {
        val databaseName = "db_todo"
        var appDatabase : AppDatabase? = null

        fun getInstance(context : Context) : AppDatabase? {
            if(appDatabase == null) {
                appDatabase = Room.databaseBuilder(context,
                    AppDatabase::class.java,
                    databaseName)
                    .fallbackToDestructiveMigration() // 마이그레이션에 실패 시 db table 재생성, 영구적으로 데이터 손실 가능성 있음
                    .build()
            }
            return appDatabase
        }
    }

}
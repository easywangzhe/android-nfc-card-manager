package com.opencode.nfccardmanager.feature.template

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private const val TEMPLATE_DB_NAME = "templates.db"
private const val TEMPLATE_DB_VERSION = 1
private const val TABLE_TEMPLATE = "templates"

class TemplateDbHelper(context: Context) : SQLiteOpenHelper(
    context,
    TEMPLATE_DB_NAME,
    null,
    TEMPLATE_DB_VERSION,
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_TEMPLATE (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                version TEXT NOT NULL,
                content TEXT NOT NULL,
                description TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun queryAll(): List<WriteTemplate> {
        val cursor = readableDatabase.query(
            TABLE_TEMPLATE,
            null,
            null,
            null,
            null,
            null,
            "name ASC",
        )

        return buildList {
            cursor.use {
                while (it.moveToNext()) {
                    add(
                        WriteTemplate(
                            id = it.getString(it.getColumnIndexOrThrow("id")),
                            name = it.getString(it.getColumnIndexOrThrow("name")),
                            version = it.getString(it.getColumnIndexOrThrow("version")),
                            content = it.getString(it.getColumnIndexOrThrow("content")),
                            description = it.getString(it.getColumnIndexOrThrow("description")),
                        )
                    )
                }
            }
        }
    }

    fun insert(template: WriteTemplate) {
        writableDatabase.insert(
            TABLE_TEMPLATE,
            null,
            ContentValues().apply {
                put("id", template.id)
                put("name", template.name)
                put("version", template.version)
                put("content", template.content)
                put("description", template.description)
            },
        )
    }

    fun update(template: WriteTemplate) {
        writableDatabase.update(
            TABLE_TEMPLATE,
            ContentValues().apply {
                put("name", template.name)
                put("version", template.version)
                put("content", template.content)
                put("description", template.description)
            },
            "id = ?",
            arrayOf(template.id),
        )
    }

    fun delete(id: String) {
        writableDatabase.delete(TABLE_TEMPLATE, "id = ?", arrayOf(id))
    }
}

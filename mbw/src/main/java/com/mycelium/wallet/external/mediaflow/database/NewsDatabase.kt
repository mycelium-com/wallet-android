package com.mycelium.wallet.external.mediaflow.database

import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import android.database.sqlite.SQLiteStatement
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mycelium.wallet.activity.news.NewsUtils
import com.mycelium.wallet.external.mediaflow.database.NewsSQLiteHelper.NEWS
import com.mycelium.wallet.external.mediaflow.model.*
import java.util.*


object NewsDatabase {
    private lateinit var database: SQLiteDatabase
    private lateinit var insertOrReplaceNews: SQLiteStatement
    private lateinit var readNews: SQLiteStatement
    private val gson = GsonBuilder().create()

    enum class SqlState {
        INSERTED, UPDATED
    }

    // call it before start work with database
    fun initialize(context: Context) {
        val helper = NewsSQLiteHelper(context)
        database = helper.writableDatabase

        insertOrReplaceNews = database.compileStatement("INSERT OR REPLACE INTO $NEWS(id,title,content,date,author,short_URL,category,image,excerpt,isfull,tags)" +
                " VALUES (?,?,?,?,?,?,?,?,?,?,?)")
        readNews = database.compileStatement("UPDATE $NEWS SET read = ? WHERE id = ?")
    }

    fun getNews(search: String?, categories: List<Category>, limit: Int = -1, offset: Int = 0): List<News> {
        val where = StringBuilder()
        if (search != null && search.isNotEmpty()) {
            where.append("(title like '%$search%' OR content like '%$search%')")
        }
        if (categories.isNotEmpty()) {
            if (where.isNotEmpty()) {
                where.append(" AND ")
            }
            where.append(categories.joinToString(" OR ") { "category = '${it.name}'" })
        }
        val builder = SQLiteQueryBuilder()
        builder.tables = NEWS

        val cursor = builder.query(database
                , arrayOf("id", "title", "content", "date", "author", "short_URL", "image", "category", "categories", "read", "excerpt", "isfull", "tags")
                , where.toString(), null, null, null, "date desc"
                , if (limit != -1) "$offset,$limit" else null)
        val result = mutableListOf<News>()
        while (cursor.moveToNext()) {
            result.add(News().apply {
                parse(cursor)
            })
        }
        cursor.close()
        return result
    }

    private fun News.parse(cursor: Cursor) {
        id = cursor.getInt(cursor.getColumnIndex("id"))
        title = Content(cursor.getString(cursor.getColumnIndex("title")))
        content = Content(cursor.getString(cursor.getColumnIndex("content")))
        date = Date(cursor.getLong(cursor.getColumnIndex("date")))
        author = Author(cursor.getString(cursor.getColumnIndex("author")))
        image = cursor.getString(cursor.getColumnIndex("image"))
        link = cursor.getString(cursor.getColumnIndex("short_URL"))
        isRead = cursor.getInt(cursor.getColumnIndex("read")) != 0
        excerpt = Content(cursor.getString(cursor.getColumnIndex("excerpt")))
        val category = cursor.getString(cursor.getColumnIndex("category"))
        categories = listOf(Category(category))
        isFull = cursor.getInt(cursor.getColumnIndex("isfull")) != 0
        tags = gson.fromJson(cursor.getString(cursor.getColumnIndex("tags")), object : TypeToken<List<Tag>>() {}.type)
    }

    fun getNewsCount(search: String?, categories: List<Category>): Long {
        val where = StringBuilder()
        if (search != null && search.isNotEmpty()) {
            where.append("(title like '%$search%' OR content like '%$search%')")
        }
        if (categories.isNotEmpty()) {
            if (where.isNotEmpty()) {
                where.append(" AND ")
            }
            where.append("(").append(categories.map { "category = '${it.name}'" }.joinToString(" OR ")).append(")")
        }
        return DatabaseUtils.queryNumEntries(database, NEWS, where.toString())
    }

    fun saveNews(news: List<News>): Map<News, SqlState> {
        val result = mutableMapOf<News, SqlState>()
        try {
            database.beginTransaction()
            news.forEach {
                val cursor = database.query("news", arrayOf("id"), "id = ?"
                        , arrayOf(it.id.toString()), null, null, null)
                val isExists = cursor.count > 0
                cursor.close()

                insertOrReplaceNews.clearBindings()
                insertOrReplaceNews.bindLong(1, it.id.toLong())
                insertOrReplaceNews.bindString(2, it.title.rendered)
                insertOrReplaceNews.bindString(3, it.content.rendered ?: "")
                insertOrReplaceNews.bindLong(4, it.date?.time ?: 0)
                insertOrReplaceNews.bindString(5, it.author?.name ?: "")
                insertOrReplaceNews.bindString(6, it.link ?: "")
                insertOrReplaceNews.bindString(7, it.categories?.firstOrNull()?.name ?: NewsUtils.NEWS_CATEGORY)
                insertOrReplaceNews.bindString(8, it.image ?: "")
                insertOrReplaceNews.bindString(9, it.excerpt.rendered ?: "")
                insertOrReplaceNews.bindLong(10, if (it.isFull) 1 else 0)
                insertOrReplaceNews.bindString(11, gson.toJson(it.tags))
                insertOrReplaceNews.executeInsert()

                result[it] = if (isExists) SqlState.UPDATED else SqlState.INSERTED
            }
            database.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e("NewsDatabase", "saveNews", e)
        } finally {
            database.endTransaction()
        }

        return result
    }

    fun markRead(news: News) {
        try {
            database.beginTransaction()
            readNews.clearBindings()
            readNews.bindLong(1, 1)
            readNews.bindLong(2, news.id.toLong())
            readNews.executeInsert()
            database.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e("NewsDatabase", "read", e)
        } finally {
            database.endTransaction()
        }
    }

    fun getCategories(): List<Category> {
        val builder = SQLiteQueryBuilder()
        builder.tables = NEWS
        val cursor = builder.query(database, arrayOf("category"), null, null
                , "category", null, null)
        val result = mutableListOf<Category>()
        while (cursor.moveToNext()) {
            val category = Category()
            category.name = cursor.getString(cursor.getColumnIndex("category"))
            result.add(category)
        }
        cursor.close()
        return result
    }

    fun delete(id: String) {
        database.delete(NEWS, "id=?", arrayOf(id))
    }

    fun getTopic(id: Int): News? {
        val cursor = database.query(NEWS,
                arrayOf("id", "title", "content", "date", "author", "short_URL", "image", "category", "categories", "read", "excerpt", "isfull", "tags"),
                "id=?", arrayOf(id.toString()), null, null, null)
        var result: News? = null
        if (cursor.moveToFirst()) {
            result = News().apply {
                parse(cursor)
            }
        }
        cursor.close()
        return result

    }
}

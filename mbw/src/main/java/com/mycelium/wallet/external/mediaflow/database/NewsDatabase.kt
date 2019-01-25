package com.mycelium.wallet.external.mediaflow.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import android.database.sqlite.SQLiteStatement
import android.util.Log
import com.mycelium.wallet.external.mediaflow.database.NewsSQLiteHelper.NEWS
import com.mycelium.wallet.external.mediaflow.model.Author
import com.mycelium.wallet.external.mediaflow.model.Category
import com.mycelium.wallet.external.mediaflow.model.News
import java.util.*


object NewsDatabase {
    private lateinit var database: SQLiteDatabase
    private lateinit var insertOrReplaceNews: SQLiteStatement
    private lateinit var readNews: SQLiteStatement

    enum class SqlState {
        INSERTED, UPDATED
    }

    // call it before start work with database
    fun initialize(context: Context) {
        val helper = NewsSQLiteHelper(context)
        database = helper.writableDatabase

        insertOrReplaceNews = database.compileStatement("INSERT OR REPLACE INTO $NEWS(id,title,content,date,author,short_URL,category,image,excerpt)" +
                " VALUES (?,?,?,?,?,?,?,?,?)")
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

            categories.forEach {
                where.append("category = '${it.name}' OR ")
            }
            where.delete(where.length - 3, where.length)
        }
        val builder = SQLiteQueryBuilder()
        builder.tables = NewsSQLiteHelper.NEWS

        val cursor = builder.query(database
                , arrayOf("id", "title", "content", "date", "author", "short_URL", "image", "category", "categories", "read", "excerpt")
                , where.toString(), null, null, null, "date desc"
                , if (limit != -1) offset.toString() + "," + limit.toString() else null)
        val result = mutableListOf<News>()
        while (cursor.moveToNext()) {
            val news = News()
            news.id = cursor.getInt(cursor.getColumnIndex("id"))
            news.title = cursor.getString(cursor.getColumnIndex("title"))
            news.content = cursor.getString(cursor.getColumnIndex("content"))
            news.date = Date(cursor.getLong(cursor.getColumnIndex("date")))
            news.author = Author(cursor.getString(cursor.getColumnIndex("author")))
            news.image = cursor.getString(cursor.getColumnIndex("image"))
            news.link = cursor.getString(cursor.getColumnIndex("short_URL"))
            val category = cursor.getString(cursor.getColumnIndex("category"))
            news.isRead = cursor.getInt(cursor.getColumnIndex("read")) != 0
            news.excerpt = cursor.getString(cursor.getColumnIndex("excerpt"))
            news.categories = mapOf<String, Category>(category to Category(category))
            result.add(news)
        }
        cursor.close()
        return result
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
                insertOrReplaceNews.bindString(2, it.title)
                insertOrReplaceNews.bindString(3, it.content)
                insertOrReplaceNews.bindLong(4, it.date.time)
                insertOrReplaceNews.bindString(5, it.author.name)
                insertOrReplaceNews.bindString(6, it.link)
                insertOrReplaceNews.bindString(7, it.categories.values.elementAt(0).name)
                insertOrReplaceNews.bindString(8, it.image)
                insertOrReplaceNews.bindString(9, it.excerpt)
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

    fun read(news: News) {
        try {
            database.beginTransaction()
            readNews.clearBindings()
            readNews.bindLong(1, 1)
            readNews.bindLong(2, news.id.toLong())
            readNews.executeInsert()
            database.setTransactionSuccessful()
        }catch (e: Exception) {
            Log.e("NewsDatabase", "read", e)
        } finally {
            database.endTransaction()
        }
    }

    fun getCategories(): List<Category> {
        val builder = SQLiteQueryBuilder()
        builder.tables = NewsSQLiteHelper.NEWS
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
}

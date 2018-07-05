package com.mycelium.wallet.external.news.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import android.database.sqlite.SQLiteStatement
import com.mycelium.wallet.external.news.model.Author
import com.mycelium.wallet.external.news.model.Category
import com.mycelium.wallet.external.news.model.News
import java.util.*


object NewsDatabase {
    private lateinit var database: SQLiteDatabase
    private lateinit var insertOrReplaceNews: SQLiteStatement

    init {

    }

    // call it before start work with database
    fun initilize(context: Context) {
        val helper = NewsSQLiteHelper(context)
        database = helper.writableDatabase

        insertOrReplaceNews = database.compileStatement("INSERT OR REPLACE INTO news VALUES (?,?,?,?,?,?,?,?)")
    }

    fun getNews(search: String?, categories: List<Category>): List<News> {
        var where = ""
        if (search != null && search.isNotEmpty()) {
            where = "title like '%$search%' OR content like '%$search%'"
        }
        val builder = SQLiteQueryBuilder()
        builder.tables = "news"
        val cursor = builder.query(database
                , arrayOf("id", "title", "content", "date", "author", "short_URL", "categories")
                , where, null, null, null, null)
        val result = mutableListOf<News>()
        while (cursor.moveToNext()) {
            val news = News()
            news.id = cursor.getInt(cursor.getColumnIndex("id"))
            news.title = cursor.getString(cursor.getColumnIndex("title"))
            news.content = cursor.getString(cursor.getColumnIndex("content"))
            news.date = Date(cursor.getLong(cursor.getColumnIndex("date")))
            news.author = Author(cursor.getString(cursor.getColumnIndex("author")))
            news.link = cursor.getString(cursor.getColumnIndex("short_URL"))
            val category = cursor.getString(cursor.getColumnIndex("category"))
            news.categories = mapOf<String, Category>(category to Category(category))
            news.categories = mapOf<String, Category>()
            result.add(news)
        }
        cursor.close()
        return result
    }

    fun saveNews(news: List<News>) {
        database.beginTransaction()
        news.forEach {
            insertOrReplaceNews.clearBindings()
            insertOrReplaceNews.bindLong(1, it.id.toLong())
            insertOrReplaceNews.bindString(2, it.title)
            insertOrReplaceNews.bindString(3, it.content)
            insertOrReplaceNews.bindLong(4, it.date.time)
            insertOrReplaceNews.bindString(5, it.author.name)
            insertOrReplaceNews.bindString(6, it.link)
            insertOrReplaceNews.bindString(7, it.categories.values.elementAt(0).name)
            insertOrReplaceNews.executeInsert()
        }
        database.setTransactionSuccessful()
        database.endTransaction()
    }

    fun getCategories(): List<Category> {
        val cursor = SQLiteQueryBuilder().query(database, arrayOf("category"), null, null
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

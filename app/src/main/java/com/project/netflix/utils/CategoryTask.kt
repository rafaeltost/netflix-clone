package com.project.netflix.utils

import android.os.Handler
import android.os.Looper
import com.project.netflix.model.Category
import com.project.netflix.model.Movie
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

class CategoryTask(private val callback: CallBack){

    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    interface CallBack{
        fun onPreExecute()
        fun onResult(categories: List<Category>)
        fun onFailure(message:String)
    }

    fun execute(url: String){
        callback.onPreExecute()

        executor.execute{
            callback.onPreExecute()
            var urlConnection: HttpsURLConnection? = null
            var stream: InputStream? = null
            try {
                val requestUrl = URL(url)
                urlConnection = requestUrl.openConnection() as HttpsURLConnection
                urlConnection.connectTimeout = 2000
                urlConnection.readTimeout = 2000

                val statusCode = urlConnection.responseCode
                if (statusCode > 404){
                    throw IOException("Erro na comunicação do servidor")
                }

                stream = urlConnection.inputStream
                val jsonAsString = stream.bufferedReader().use { it.readText() }

                val categories = toCategories(jsonAsString)

                handler.post {
                    callback.onResult(categories)
                }

            } catch (e: IOException){
                val message = e.message ?: "Erro desconhecido"
                handler.post {
                    callback.onFailure(message)
                }
            } finally {
                urlConnection?.disconnect()
                stream?.close()
            }
        }

    }

    private fun toCategories(jsonAsString: String): List<Category> {
        val categories = mutableListOf<Category>()

        val jsonRoot = JSONObject(jsonAsString)
        val jsonCategories = jsonRoot.getJSONArray("category")

        for(category in 0 until jsonCategories.length()){
            val jsonCategory = jsonCategories.getJSONObject(category)

            val title = jsonCategory.getString("title")
            val jsonMovies = jsonCategory.getJSONArray("movie")

            val movies = mutableListOf<Movie>()

            for(movie in 0 until jsonMovies.length()){
                val jsonMovie = jsonMovies.getJSONObject(movie)
                val id = jsonMovie.getInt("id")
                val coverUrl = jsonMovie.getString("cover_url")
                movies.add(Movie(id, coverUrl))
            }
            categories.add(Category(title,movies))
        }
        return categories
    }

}
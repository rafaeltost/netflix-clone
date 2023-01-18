package com.project.netflix.utils

import android.os.Handler
import android.os.Looper
import com.project.netflix.model.Movie
import com.project.netflix.model.MovieDetail
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

class MovieTask(val callback:CallBack) {
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    interface CallBack{
        fun onPreExecute()
        fun onResult(movieDetail: MovieDetail)
        fun onFailure(message:String)
    }

    fun execute(url: String){
        callback.onPreExecute()

        executor.execute{
            callback.onPreExecute()
            var urlConnection: HttpsURLConnection? = null
            var stream: InputStream? = null
            var buffer: BufferedInputStream? = null
            try {
                val requestUrl = URL(url)
                urlConnection = requestUrl.openConnection() as HttpsURLConnection
                urlConnection.connectTimeout = 2000
                urlConnection.readTimeout = 2000

                val statusCode = urlConnection.responseCode

                if(statusCode == 400){
                    stream = urlConnection.errorStream
                    buffer = BufferedInputStream(stream)
                    val jsonAsString = toString(buffer)

                    val json = JSONObject(jsonAsString)
                    val message = json.getString("message")
                    throw IOException(message)

                } else if (statusCode > 404){
                    throw IOException("Erro na comunicação do servidor")
                }

                stream = urlConnection.inputStream

                buffer = BufferedInputStream(stream)
                val jsonAsString = toString(buffer)

                val movieDetail = toMovieDetail(jsonAsString)

                handler.post {
                    callback.onResult(movieDetail)
                }

            } catch (e: IOException){
                val message = e.message ?: "Erro desconhecido"
                handler.post {
                    callback.onFailure(message)
                }
            } finally {
                urlConnection?.disconnect()
                stream?.close()
                buffer?.close()
            }
        }

    }
    private fun toMovieDetail(jsonAsString: String): MovieDetail{

        val json = JSONObject(jsonAsString)

        val id = json.getInt("id")
        val title = json.getString("title")
        val desc = json.getString("desc")
        val cast = json.getString("cast")
        val coverUrl = json.getString("cover_url")
        val jsonMovies = json.getJSONArray("movie")

        val similars = mutableListOf<Movie>()

        for(movie in 0 until jsonMovies.length()){
            val jsonMovie = jsonMovies.getJSONObject(movie)

            val similarId = jsonMovie.getInt("id")
            val similarCoverUrl = jsonMovie.getString("cover_url")

            similars.add(Movie(similarId, similarCoverUrl))
        }
        val movie = Movie(id,coverUrl,title,desc,cast)

        return MovieDetail(movie,similars)
    }

    private fun toString(stream: InputStream): String {
        val bytes = ByteArray(1024)
        val baos = ByteArrayOutputStream()
        var read: Int

        while(true){
            read = stream.read(bytes)
            if(read<=0){
                break
            }
            baos.write(bytes,0,read)
        }
        return String(baos.toByteArray())

    }
}
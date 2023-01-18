package com.project.netflix

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.LayerDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.project.netflix.model.Movie
import com.project.netflix.model.MovieDetail
import com.project.netflix.utils.DownloadImageTask
import com.project.netflix.utils.MovieTask

class MovieActivity : AppCompatActivity(), MovieTask.CallBack {
    private lateinit var txtTitle: TextView
    private lateinit var txtDesc: TextView
    private lateinit var txtCast: TextView
    private lateinit var movieAdapter: MovieAdapter
    private lateinit var progress: ProgressBar
    private val movies = mutableListOf<Movie>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie)

        txtTitle = findViewById(R.id.movie_txt_title)
        txtDesc = findViewById(R.id.movie_txt_desc)
        txtCast = findViewById(R.id.movie_txt_cast)
        progress = findViewById(R.id.movie_progress)

        val recyclerView: RecyclerView = findViewById(R.id.movies_rv_similars)

        movieAdapter = MovieAdapter(movies, R.layout.movie_item_similars)
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = movieAdapter

        val id = intent?.getIntExtra("id",0) ?: throw IllegalStateException("Id não encontrado")

        val url = "https://api.tiagoaguiar.co/netflixapp/movie/$id?apiKey=f86294d0-71b5-4f68-b8ce-79e92497f4cf"

        MovieTask(this).execute(url)

        val toolbar: Toolbar = findViewById(R.id.movie_toolbar)

        setSupportActionBar(toolbar)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_24)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = null

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

    override fun onPreExecute() {
        progress.visibility = View.VISIBLE
    }

    override fun onResult(movieDetail: MovieDetail) {
        txtTitle.text = movieDetail.movie.title
        txtDesc.text = movieDetail.movie.desc
        txtCast.text = movieDetail.movie.cast

        movies.clear()
        movies.addAll(movieDetail.similars)

        movieAdapter.notifyDataSetChanged()

        DownloadImageTask(object: DownloadImageTask.CallBack{
            override fun onResult(bitmap: Bitmap) {
                val layerDrawable: LayerDrawable = ContextCompat.getDrawable(this@MovieActivity, R.drawable.shadows) as LayerDrawable
                val movieCover = BitmapDrawable(resources,bitmap)
                val coverImage: ImageView = findViewById(R.id.movie_img)

                layerDrawable.setDrawableByLayerId(R.id.cover_drawable, movieCover)
                coverImage.setImageDrawable(layerDrawable)
            }
        }).execute(movieDetail.movie.coverUrl)

        progress.visibility = View.GONE
    }

    override fun onFailure(message: String) {
        progress.visibility = View.GONE
        Toast.makeText(this,message,Toast.LENGTH_LONG).show()
    }
}
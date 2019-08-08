package com.example.movieproject.activities

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.example.movieproject.Injection
import com.example.movieproject.MovieApiViewModel
import com.example.movieproject.R
import com.example.movieproject.ViewModelFactory
import com.example.movieproject.adapters.CreditsAdapter
import com.example.movieproject.adapters.LogosAdapter
import com.example.movieproject.adapters.OnMoreClickedListener
import com.example.movieproject.adapters.ParentAdapter
import com.example.movieproject.api.models.*
import com.example.movieproject.listeners.*
import com.google.android.youtube.player.YouTubeStandalonePlayer
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_movie_description.*

class MovieDescription : AppCompatActivity(), MovieClickListener,
    OnMoreClickedListener {

    private lateinit var viewModelFactory: ViewModelFactory
    private lateinit var viewModel: MovieApiViewModel
    private val disposables = CompositeDisposable()



    override fun onMovieClicked(movie: Movie) {
        start(this, movie.id.toString(), movie)
    }

    override fun onMoreClicked(result: Result) {
        MovieListActivity.start(this, movies)
    }

    lateinit var movieDetails: MovieDetails
    lateinit var movies: Result
    val list = mutableListOf<Result>()
    private val creditsAdapter by lazy { CreditsAdapter() }
    private val logosAdapter by lazy { LogosAdapter() }
    private val firebaseCloudstore by lazy { FirebaseFirestore.getInstance() }
    private val favourites by lazy { firebaseCloudstore.collection(FAVOURITES) }


    private fun loadRecommendedMovies(movies: Result) {

        list.add(movies)
        with(recommended_movies){
            layoutManager = LinearLayoutManager(this@MovieDescription, LinearLayout.VERTICAL, false)
            adapter =
                ParentAdapter(list, this@MovieDescription, this@MovieDescription)
        }

    }


    private fun loadDetails(movieDetails: MovieDetails) {
        Picasso.get().load("http://image.tmdb.org/t/p/w780" + movieDetails.poster_path).into(movie_poster)
        movie_title.text = movieDetails.title
        movie_overview.text = movieDetails.overview

    }


    companion object {
        private const val FAVOURITES = "favourites"

        private const val ARG_ID = "movie_id"
        private const val ARG_MOVIE = "movie"
        private val TAG = "argyn"

        fun start(context: Context, id: String, movie: Movie) {
            context.startActivity(
                Intent(context,
                    MovieDescription::class.java).apply {
                    putExtra(ARG_ID, id)
                    putExtra(ARG_MOVIE, movie)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie_description)

        viewModelFactory = Injection.provideViewModelFactory()
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(MovieApiViewModel::class.java)
        initUI()
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
    }

    private fun initUI() {


        with(credits_list) {
            layoutManager = LinearLayoutManager(context)
            adapter = creditsAdapter
        }
        with(logos_list) {
            layoutManager = LinearLayoutManager(context)
            adapter = logosAdapter
        }
        var movieId: String = intent.getStringExtra(ARG_ID)
        var movie: Movie = intent.getParcelableExtra(ARG_MOVIE)
        disposables.add(viewModel.loadTopCredits(movieId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                var list = it.cast
                list = list.subList(0,3)
                creditsAdapter.setCasts(list)
            }, { error -> Log.e(TAG, "Unable to get top rated movies", error) }))

        addToFavourites.setOnClickListener{
            favourites.document(movie.id.toString()).set(movie).addOnCompleteListener {
                    task ->
                run {
                    if (task.isSuccessful) {
                        Toast.makeText(this, R.string.success_message,
                            Toast.LENGTH_LONG).show()


                    } else {
                        Toast.makeText(this, task.exception?.message,
                            Toast.LENGTH_LONG).show()
                    }
                }
            }

        }
        removeFromFavourites.setOnClickListener{
            favourites.document(movie.id.toString()).delete().addOnSuccessListener {
                Log.d("argyn", "deleted")
            }.addOnFailureListener{
                Log.d("argyn", "not deleted")
            }
        }


        disposables.add(viewModel.loadMovieDesc(movieId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                movieDetails = it
                loadDetails(movieDetails)
                logosAdapter.setLogos(movieDetails.production_companies)
            }, { error -> Log.e(TAG, "Unable to get movie description", error) }))
        disposables.add(viewModel.loadRecommendedMovies(movieId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                movies = it
                loadRecommendedMovies(movies)
            }, { error -> Log.e(TAG, "Unable to get recommended movies", error) }))
        disposables.add(viewModel.loadSimilarMovies(movieId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                movies = it
                loadRecommendedMovies(movies)
            }, { error -> Log.e(TAG, "Unable to get similar movies", error) }))
        disposables.add(viewModel.loadVideos(movieId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                val video = it
                movie_video.setOnClickListener {
                    var intent = YouTubeStandalonePlayer.createVideoIntent(this, "AIzaSyB2K-Jq5IF_4GxOwROSrTBfEuISnLKe4nM",
                        video.results[0].key)
                    startActivity(intent)
                }
            }, { error -> Log.e(TAG, "Unable to load video", error) }))
    }
}

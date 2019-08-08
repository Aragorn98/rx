package com.example.movieproject.activities

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Toast
import com.example.movieproject.*
import com.example.movieproject.adapters.OnMoreClickedListener
import com.example.movieproject.adapters.ParentAdapter
import com.example.movieproject.api.MovieService
import com.example.movieproject.api.models.*
import com.example.movieproject.api.models.DeleteSession
import com.example.movieproject.listeners.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(),  MovieClickListener,
    OnMoreClickedListener, AdapterView.OnItemSelectedListener{

    private lateinit var viewModelFactory: ViewModelFactory
    private lateinit var viewModel: MovieApiViewModel
    private val disposables = CompositeDisposable()
    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }
    val list = mutableListOf<Result>()
    private lateinit var requestToken: String
    var sessionId: String? = null
    val genreName = mutableListOf<String>()
    lateinit var genres: List<Genre>

    companion object {
        private const val FAVOURITES = "favourites"
        private val TAG = "argyn"
        fun start(context: Context) {
            context.startActivity(
                Intent(context,
                    MainActivity::class.java)
            )
        }
    }
    private val firebaseCloudstore by lazy { FirebaseFirestore.getInstance() }
    private val favourites by lazy { firebaseCloudstore.collection(FAVOURITES) }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModelFactory = Injection.provideViewModelFactory()
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(MovieApiViewModel::class.java)

        val user = firebaseAuth.currentUser

        if (user == null) {
            LoginActivity.start(this)
            finish()
        }

        createRequestToken.setOnClickListener{
            disposables.add(viewModel.loadRequestToken()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    requestToken = it.request_token
                    val url = "https://www.themoviedb.org/authenticate/$requestToken"
                    Log.d("argyn", this.requestToken)
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

                }, { error -> Log.e(TAG, "Unable to get request token", error) }))
        }

        createSession.setOnClickListener{
//            CreateSession(this, RequestTokenClass(this.requestToken)).createSession()
            disposables.add(viewModel.createSession(RequestTokenClass(requestToken))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    sessionId = it.session_id
                    Toast.makeText(this, "Session created", Toast.LENGTH_LONG).show()
                }, { error -> Log.e(TAG, "Unable to create session", error) }))
        }

        deleteSession.setOnClickListener{
            if(sessionId != null){
                disposables.add(viewModel.deleteSession(SessionIdClass(sessionId.toString()))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe ({
                        Toast.makeText(this, it.success.toString(), Toast.LENGTH_LONG).show()
                    }, { error -> Log.e(TAG, "Unable to delete session", error) }))
            }
            else Toast.makeText(this, "There is no session", Toast.LENGTH_LONG).show()
        }

        eng.setOnClickListener { LangPref.lang = "en"
        Log.d("agryn", "en")
        }
        eng.setOnClickListener { LangPref.lang = "ru"
            Log.d("agryn", "ru")
        }
        favourite_movies.setOnClickListener {
            var movies = ArrayList<Movie>()
            favourites.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        document.forEach { snapshot ->
                            Log.d("argyn", snapshot.toString())
                            movies.add(snapshot.toObject(Movie::class.java))
                        }


                        val result = Result("Favourites", 0, movies, 0, 0)

                        Log.d("argyn", result.toString())
                        Log.d("argyn", movies.toString())
                        MovieListActivity.start(this, result)

                        Log.d("argyn", " not null")
                    } else {
                        Log.d("argyn", "No such document")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("argyn", "get failed with ", exception)
                }
        }



        genres_spinner.setSelection(0, false)

        disposables.add(viewModel.loadGenres()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({
                genres = it.genres
                for (genre: Genre in genres){
                    genreName.add(genre.name)
                }
                val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genreName)
                arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                genres_spinner!!.adapter = arrayAdapter
                genres_spinner.onItemSelectedListener = this
            }, { error -> Log.e(TAG, "Unable to get genres", error) }))
        disposables.add(viewModel.loadTopRatedMovies()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({onMoviesChanged(it)}, { error -> Log.e(TAG, "Unable to get top rated movies", error) }))
        disposables.add(viewModel.loadPopularMovies()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({onMoviesChanged(it)}, { error -> Log.e(TAG, "Unable to get popular movies", error) }))
        disposables.add(viewModel.loadUpcomingMovies()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({onMoviesChanged(it)}, { error -> Log.e(TAG, "Unable to get upcoming movies", error) }))
        disposables.add(viewModel.loadNowPlayingMovies()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({onMoviesChanged(it)}, { error -> Log.e(TAG, "Unable to get now playing movies", error) }))

    }

    private fun onMoviesChanged(movies: Result) {
        list.add(movies)
        if(list.size == 4) {
            Log.d("argyn", "size is 4")
            Log.d("argyn", list.toString())
            with(rv_parent){
                layoutManager = LinearLayoutManager(this@MainActivity, LinearLayout.VERTICAL, false)
                adapter = ParentAdapter(list, this@MainActivity, this@MainActivity)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        disposables.add(viewModel.loadMoviesByGenre(genres[position].id.toString(), genres[position].name)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({MovieListActivity.start(this, it)},
                { error -> Log.e(TAG, "Unable to load movies by genre", error) }))
    }


    override fun onMoreClicked(result: Result) {
        MovieListActivity.start(this, result)
    }

    override fun onMovieClicked(movie: Movie) {
        Log.d("argyn", movie.id.toString())
        MovieDescription.start(this, movie.id.toString(), movie)
    }
}

package com.example.movieproject

import android.arch.lifecycle.ViewModel
import com.example.movieproject.api.MovieApi
import com.example.movieproject.api.models.*
import io.reactivex.Completable
import io.reactivex.Flowable

class MovieApiViewModel(private val dataSource: MovieApi) : ViewModel() {
    var moviesListType: String = "ee"


    fun loadGenres(): Flowable<Genres>{
        return dataSource.getGenres(LangPref.lang)
    }

    fun loadPopularMovies(): Flowable<Result> {
        moviesListType = "Popular"
        return dataSource.getPopularMovies(LangPref.lang)
    }

    fun loadTopRatedMovies(): Flowable<Result> {
        moviesListType = "Top Rated"
        return dataSource.getTopRatedMovies(LangPref.lang)
    }

    fun loadUpcomingMovies(): Flowable<Result> {
        moviesListType = "Upcoming"
        return dataSource.getUpcomingMovies(LangPref.lang)
    }

    fun loadNowPlayingMovies(): Flowable<Result> {
        moviesListType = "Now Playing"
        return dataSource.getNowPlayingMovies(LangPref.lang)
    }

    fun loadRecommendedMovies(id: String): Flowable<Result> {
        moviesListType = "Recommended Movies"
        return dataSource.getRecommendedMovies(id, LangPref.lang)
    }

    fun loadSimilarMovies(id: String): Flowable<Result> {
        moviesListType = "Similar movies"
        return dataSource.getSimilarMovies(id,LangPref.lang)
    }

    fun loadMoviesByGenre(id: String, name: String): Flowable<Result> {
        moviesListType = name
        return dataSource.getMoviesByGenre(id, LangPref.lang)
    }

    fun createSession(requestToken: RequestTokenClass): Flowable<CreateSession> {
        return dataSource.createSession(requestToken)
    }

    fun loadTopCredits(id: String): Flowable<Credits> {
        return dataSource.getTopCredits(id, LangPref.lang)
    }

    fun deleteSession(sessionId: SessionIdClass): Flowable<DeleteSession> {
        return dataSource.deleteSession(sessionId)
    }
    fun loadMovieDesc(id: String): Flowable<MovieDetails> {

        return dataSource.getMovieDesc(id, LangPref.lang)
    }
    fun loadRequestToken(): Flowable<RequestToken> {
        return dataSource.getRequestToken()
    }
    fun loadVideos(id: String): Flowable<Video> {
        return dataSource.getVideos(id, LangPref.lang)
    }

}

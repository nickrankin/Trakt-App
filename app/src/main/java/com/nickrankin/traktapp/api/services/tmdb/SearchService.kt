package com.nickrankin.traktapp.api.services.tmdb

import com.uwetrottmann.tmdb2.entities.*
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface SearchService {
    /**
     * Search for companies.
     *
     * @see [Documentation](https://developers.themoviedb.org/3/search/search-companies)
     */
    @GET("search/company")
    suspend fun company(
        @Query("query") query: String?,
        @Query("page") page: Int?
    ): CompanyResultsPage

    /**
     * Search for collections.
     *
     * @see [Documentation](https://developers.themoviedb.org/3/search/search-collections)
     */
    @GET("search/collection")
    suspend fun collection(
        @Query("query") query: String?,
        @Query("page") page: Int?,
        @Query("language") language: String?
    ): CollectionResultsPage

    /**
     * Search for keywords.
     *
     * @see [Documentation](https://developers.themoviedb.org/3/search/search-keywords)
     */
    @GET("search/keyword")
    suspend fun keyword(
        @Query("query") query: String?,
        @Query("page") page: Int?
    ): KeywordResultsPage

    /**
     * Search for movies.
     *
     * @see [Documentation](https://developers.themoviedb.org/3/search/search-movies)
     */
    @GET("search/movie")
    suspend fun movie(
        @Query("query") query: String?,
        @Query("page") page: Int?,
        @Query("language") language: String?,
        @Query("region") region: String?,
        @Query("include_adult") includeAdult: Boolean?,
        @Query("year") year: Int?,
        @Query("primary_release_year") primaryReleaseYear: Int?
    ): MovieResultsPage

    /**
     * Search multiple models in a single request.
     * Multi search currently supports searching for movies,
     * tv shows and people in a single request.
     *
     * @see [Documentation](https://developers.themoviedb.org/3/search/multi-search)
     */
    @GET("search/multi")
    suspend fun multi(
        @Query("query") query: String?,
        @Query("page") page: Int?,
        @Query("language") language: String?,
        @Query("region") region: String?,
        @Query("include_adult") includeAdult: Boolean?
    ): MediaResultsPage

    /**
     * Search for people.
     *
     * @see [Documentation](https://developers.themoviedb.org/3/search/search-people)
     */
    @GET("search/person")
    suspend fun person(
        @Query("query") query: String?,
        @Query("page") page: Int?,
        @Query("language") language: String?,
        @Query("region") region: String?,
        @Query("include_adult") includeAdult: Boolean?
    ): PersonResultsPage

    /**
     * Search for TV shows.
     *
     * @see [Documentation](https://developers.themoviedb.org/3/search/search-tv-shows)
     */
    @GET("search/tv")
    suspend fun tv(
        @Query("query") query: String?,
        @Query("page") page: Int?,
        @Query("language") language: String?,
        @Query("first_air_date_year") firstAirDateYear: Int?,
        @Query("include_adult") includeAdult: Boolean?
    ): TvShowResultsPage
}

package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;

public class MoviesStore {

    private ArrayList<Movie> moviesCollection;

    public MoviesStore() {
        moviesCollection = new ArrayList<>();
    }

    public ArrayList<Movie> getMoviesCollection() {
        return moviesCollection;
    }

    public void addMovie(Movie movie) {
        moviesCollection.add(movie);
    }

    public void deleteMovieById(int id) {
        moviesCollection.remove(id - 1);
    }

    public int getLastId() {
        return moviesCollection.getLast().getId();
    }

    public void clearMoviesCollection() {
        moviesCollection.clear();
    }
}
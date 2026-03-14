package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {
    private MoviesStore moviesStore;

    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {

        Endpoint endpoint = getEndpoint(ex.getRequestURI().getPath(), ex.getRequestMethod(), ex.getRequestURI().getQuery());

        switch (endpoint) {
            case GET_MOVIES: {
                handleGetMovies(ex);
                break;
            }
            case POST_MOVIE: {
                handlePostMovie(ex);
                break;
            }
            case GET_MOVIE_ID: {
                handleGetMovieById(ex);
                break;
            }
            case GET_MOVIE_YEAR: {
                handleGetMovieByYear(ex);
                break;
            }
            case DELETE_MOVIE_ID: {
                handleDeleteMovieById(ex);
                break;
            }
            case PUT_MOVIE: {
                sendJson(ex, 405, "Метод не поддерживается");
            }
            default:
                sendJson(ex, 404, "Такого эндпоинта не существует");

        }
    }

    private void handleGetMovies(HttpExchange ex) {

        String moviesArrayJson = gson.toJson(moviesStore.getMoviesCollection());

        try {
            sendJson(ex, 200, moviesArrayJson);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handlePostMovie(HttpExchange ex) {
        InputStream inputStream = ex.getRequestBody();
        try {
            String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            if (!isValidJson(body)) {
                sendJson(ex, 400, "Некорректный JSON");
                return;
            }
            Movie movie = gson.fromJson(body, Movie.class);

            if (validateWrite(ex, movie)) {
                return;
            }

            Map<String, List<String>> headers = ex.getRequestHeaders();

            if (headers.containsKey("Content-Type")) {
                List<String> contentTypes = headers.get("Content-Type");
                String contentType = contentTypes.getFirst();

                if (!contentType.equalsIgnoreCase("application/json")) {
                    sendJson(ex, 415, new ErrorResponse(415, "Unsupported Media Type").toString());
                    return;
                }
            }

            if (moviesStore.getMoviesCollection().isEmpty()) {
                movie.setId(1);
            } else {
                movie.setId(moviesStore.getLastId() + 1);
            }
            moviesStore.addMovie(movie);
            String movieJson = gson.toJson(movie);
            sendJson(ex, 201, movieJson);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void handleGetMovieById(HttpExchange ex) throws IOException {
        Optional<Integer> movieIdOpt = getMovieId(ex);
        if (movieIdOpt.isEmpty()) {
            sendJson(ex, 400, new ErrorResponse(400, "Некорректный ID").toString());
            return;
        }
        int movieId = movieIdOpt.get();

        ArrayList<Movie> movieArrayList = moviesStore.getMoviesCollection();

        for (Movie movie : movieArrayList) {
            if (movieId == movie.getId()) {
                String moviesArrayJson = gson.toJson(movie);
                sendJson(ex, 200, moviesArrayJson);
                return;
            }
        }

        sendJson(ex, 404, new ErrorResponse(404, "Фильм не найден.").toString());
    }

    private void handleDeleteMovieById(HttpExchange ex) throws IOException {
        Optional<Integer> movieIdOpt = getMovieId(ex);
        if (movieIdOpt.isEmpty()) {
            sendJson(ex, 400, new ErrorResponse(400, "Некорректный ID").toString());
            return;
        }
        int movieId = movieIdOpt.get();

        ArrayList<Movie> movieArrayList = moviesStore.getMoviesCollection();

        for (Movie movie : movieArrayList) {
            if (movieId == movie.getId()) {
                moviesStore.deleteMovieById(movieId);
                sendNoContent(ex);
                return;
            }
        }

        sendJson(ex, 404, new ErrorResponse(404, "Фильм не найден.").toString());
    }

    private void handleGetMovieByYear(HttpExchange ex) throws IOException {
        Optional<Integer> movieYearOpt = getMovieYear(ex);
        if (movieYearOpt.isEmpty()) {
            sendJson(ex, 400, new ErrorResponse(400, "Некорректно указан год.").toString());
            return;
        }
        int year = movieYearOpt.get();

        ArrayList<Movie> sortedMovieListByYear = new ArrayList<>();
        for (int i = 0; i < moviesStore.getMoviesCollection().size(); i++) {
            if (moviesStore.getMoviesCollection().get(i).getYear() == year) {
                sortedMovieListByYear.add(moviesStore.getMoviesCollection().get(i));
            }
        }
        try {
            String moviesArrayJson = gson.toJson(sortedMovieListByYear);
            sendJson(ex, 200, moviesArrayJson);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<Integer> getMovieId(HttpExchange ex) {
        String[] pathParts = ex.getRequestURI().getPath().split("/");
        try {
            return Optional.of(Integer.parseInt(pathParts[2]));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private Optional<Integer> getMovieYear(HttpExchange ex) {
        String requestQuery = ex.getRequestURI().getQuery();
        try {
            return Optional.of(Integer.parseInt(requestQuery.substring(5)));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private boolean isValidJson(String json) {
        try {
            JsonParser.parseString(json);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }

    private boolean validateWrite(HttpExchange ex, Movie movie) throws IOException {
        boolean isYearNotInRange = movie.getYear() < 1888 || movie.getYear() > LocalDate.now().getYear();
        boolean isTitleTooLong = movie.getTitle().length() > 100;

        if (isYearNotInRange) {
            sendJson(ex, 422, new ErrorResponse(422, "Ошибка валидации. Год должен быть между 1888 и " + LocalDate.now().getYear() + ".").toString());
            return true;
        }

        if (isTitleTooLong) {
            sendJson(ex, 422, new ErrorResponse(422, "Ошибка валидации. Название не должно превышать 100 символов.").toString());
            return true;
        }

        if (movie.getTitle().isEmpty()) {
            sendJson(ex, 422, new ErrorResponse(422, "Ошибка валидации. Название не должно быть пустым.").toString());
            return true;
        }

        return false;
    }

    private Endpoint getEndpoint(String requestPath, String requestMethod, String requestQuery) {
        boolean hasQuery = requestQuery != null && !requestQuery.isEmpty();
        String[] pathParts = requestPath.split("/");

        if (pathParts.length == 2 && pathParts[1].equals("movies")) {
            if (requestMethod.equals("GET")) {
                if (hasQuery) {
                    return Endpoint.GET_MOVIE_YEAR;
                }
                return Endpoint.GET_MOVIES;
            }

            if (requestMethod.equals("POST")) {
                return Endpoint.POST_MOVIE;
            }
        }

        if (pathParts.length == 3 && pathParts[1].equals("movies") && !pathParts[2].isBlank()) {

            if (requestMethod.equals("GET")) {
                return Endpoint.GET_MOVIE_ID;
            }
            if (requestMethod.equals("DELETE")) {
                return Endpoint.DELETE_MOVIE_ID;
            }
        }

        if (requestMethod.equals("PUT")) {
            return Endpoint.PUT_MOVIE;
        }

        return Endpoint.UNKNOWN;
    }

}

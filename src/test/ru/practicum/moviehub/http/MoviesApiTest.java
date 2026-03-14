package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.*;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {

    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static final int PORT = 8080;
    private static MoviesStore store;
    private static Gson gson;

    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();

        server = new MoviesServer(store ,PORT);
        server.start();


        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    @BeforeEach
    void beforeEach() {
        store.clearMoviesCollection();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void getMovies_returnsMoviesArray() throws Exception {

        store.addMovie(new Movie(1,"Гарри Поттер и философский камень",2001));
        store.addMovie(new Movie(2,"Назад в будущее",1985));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies")) // !!! Добавьте правильный URI
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.contains("Гарри"),
                "Проверяем, содержится ли строка в json-массиве");
    }

    @Test
    void post_movie_returnSuccess() throws IOException, InterruptedException {

        String jsonBody = "{\"title\":\"Inception\",\"year\":2010}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();


        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");

    }

    @Test
    void post_movie_returnBadReqNoTitle() throws IOException, InterruptedException {

        String jsonBody = "{\"title\":\"\",\"year\":2010}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");
    }


    @Test
    void post_movie_returnBadReqTooLongTitle() throws IOException, InterruptedException {

        String jsonBody = "{\"title\":\"Inceptionhhhkhskdsfdsfdsfddsdsfdsfdsfdjdhkjhdsdsfdskfjdsljfdsjfdklsjfkldsjfjsdlkjfdkjdlsdslfjdklsfjldsj\",\"year\":2010}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

    }

    @Test
    void post_movie_returnBadReqIfYearNotInRange() throws IOException, InterruptedException {

        String jsonBody = "{\"title\":\"Inception\",\"year\":2050}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

    }

    @Test
    void post_movie_returnBadReqTypeNotJson() throws IOException, InterruptedException {

        String jsonBody = "{\"title\":\"Inception\",\"year\":2010}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type","text/html")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        assertEquals(415, resp.statusCode(), "POST /movies должен вернуть 415");

    }

    @Test
    void post_movie_returnBadReqUnCorrectJson() throws IOException, InterruptedException {

        String jsonBody = "\"title\":\"Inception\",\"year\":2010}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        assertEquals(400, resp.statusCode(), "POST /movies должен вернуть 400");

    }

    @Test
    void getMovie_returnsMovieById() throws Exception {

        store.addMovie(new Movie(1,"Гарри Поттер и философский камень",2001));
        store.addMovie(new Movie(2,"Назад в будущее",1985));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");


        Movie movie = gson.fromJson(resp.body().trim(),Movie.class);

        Assertions.assertEquals(store.getMoviesCollection().getFirst(),movie,
                "Проверяем соответствует ли фильм из ответа с фильмом из коллекции");
    }


    @Test
    void getMovie_returnsBadReqMovieNotFound() throws Exception {

        store.addMovie(new Movie(1,"Гарри Поттер и философский камень",2001));
        store.addMovie(new Movie(2,"Назад в будущее",1985));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/3"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode(), "GET /movies должен вернуть 404");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

    }


    @Test
    void getMovie_returnsBadReqIdIsNotNumber() throws Exception {

        store.addMovie(new Movie(1,"Гарри Поттер и философский камень",2001));
        store.addMovie(new Movie(2,"Назад в будущее",1985));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/test"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "GET /movies должен вернуть 400");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

    }

    @Test
    void deleteMovie_returnsSuccess() throws Exception {

        store.addMovie(new Movie(1,"Гарри Поттер и философский камень",2001));
        store.addMovie(new Movie(2,"Назад в будущее",1985));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, resp.statusCode(), "GET /movies должен вернуть 204");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        Assertions.assertEquals(1, store.getMoviesCollection().size(),
                "Проверяем количество фильмов. После удаления должен остаться только один");

    }


    @Test
    void deleteMovie_returnsBadReqNotFound() throws Exception {

        store.addMovie(new Movie(1,"Гарри Поттер и философский камень",2001));
        store.addMovie(new Movie(2,"Назад в будущее",1985));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/3"))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode(), "GET /movies должен вернуть 404");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

    }

    @Test
    void deleteMovie_returnsBadReqIsNotNumber() throws Exception {

        store.addMovie(new Movie(1,"Гарри Поттер и философский камень",2001));
        store.addMovie(new Movie(2,"Назад в будущее",1985));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/test"))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "GET /movies должен вернуть 404");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

    }

    @Test
    void getMovie_returnsMovieByYear() throws Exception {

        store.addMovie(new Movie(1,"Гарри Поттер и философский камень",2001));
        store.addMovie(new Movie(2,"Назад в будущее",1985));
        store.addMovie(new Movie(3,"Форсаж",2001));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2001"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");


        ArrayList<Movie> movieArrayList = gson.fromJson(resp.body().trim(), new ListOfMoviesTypeToken().getType());

        assertEquals(2,movieArrayList.size(),
                "Должно вернуться количество возвращенных фильмов, равное 2");
    }

    @Test
    void getMovie_returnsEmptyArrayByYear() throws Exception {

        store.addMovie(new Movie(1,"Гарри Поттер и философский камень",2001));
        store.addMovie(new Movie(2,"Назад в будущее",1985));
        store.addMovie(new Movie(3,"Форсаж",2001));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2002"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");


        ArrayList<Movie> movieArrayList = gson.fromJson(resp.body().trim(), new ListOfMoviesTypeToken().getType());

        assertEquals(0,movieArrayList.size(),
                "Должен вернуться пустой массив");
    }

    @Test
    void getMovie_returnsBadReqIfYearNotNumber() throws Exception {

        store.addMovie(new Movie(1,"Гарри Поттер и философский камень",2001));
        store.addMovie(new Movie(2,"Назад в будущее",1985));
        store.addMovie(new Movie(3,"Форсаж",2001));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=test"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

    }

    @Test
    void put_movie_returnMethodNotAllowed() throws IOException, InterruptedException {

        String jsonBody = "{\"title\":\"Inception\",\"year\":2010}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type","text/html")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        assertEquals(405, resp.statusCode(), "POST /movies должен вернуть 405");

    }
}
package ru.practicum.moviehub;

import ru.practicum.moviehub.http.MoviesServer;
import ru.practicum.moviehub.store.MoviesStore;

public class MovieHubApp {
    private static final int port = 8080;

    public static void main(String[] args) {
        final MoviesServer server = new MoviesServer(new MoviesStore(), port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();
    }
}
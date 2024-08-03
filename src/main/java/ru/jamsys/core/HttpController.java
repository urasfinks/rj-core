package ru.jamsys.core;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
@RestController
public class HttpController {

    @RequestMapping(value = "/*")
    public CompletableFuture<Void> handler(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        return new HttpAsyncResponse(new CompletableFuture<>(), request, response).getServletResponse();
    }

}

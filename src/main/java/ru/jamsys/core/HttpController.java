package ru.jamsys.core;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
@RestController
public class HttpController {

    @RequestMapping(value = "/*")
    public CompletableFuture<Void> handler(HttpServletRequest request, HttpServletResponse response) {
        return new HttpAsyncResponse(new CompletableFuture<>(), request, response).getServletResponse();
    }

}

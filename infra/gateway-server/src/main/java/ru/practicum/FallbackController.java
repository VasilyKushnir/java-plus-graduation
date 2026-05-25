package ru.practicum;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/comments")
    public Mono<ResponseEntity<String>> commentsFallback() {
        return Mono.just(
                ResponseEntity
                        .status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Comment service unavailable")
        );
    }

    @GetMapping("/users")
    public Mono<ResponseEntity<String>> usersFallback() {
        return Mono.just(
                ResponseEntity
                        .status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("User service unavailable")
        );
    }

    @GetMapping("/requests")
    public Mono<ResponseEntity<String>> requestsFallback() {
        return Mono.just(
                ResponseEntity
                        .status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Request service unavailable")
        );
    }

    @GetMapping("/events")
    public Mono<ResponseEntity<String>> eventsFallback() {
        return Mono.just(
                ResponseEntity
                        .status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Event service unavailable")
        );
    }
}
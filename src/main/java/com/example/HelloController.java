package com.example;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;

@RestController
public class HelloController {
    WebSocketClient client = new ReactorNettyWebSocketClient();

    @GetMapping(path = "/", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> demo() {
        Flux<WebSocketMessage> messages = Flux.create(sink -> {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, "bearer dummy"); // Authorization
            Disposable receive = client.execute(URI.create("ws://demos.kaazing.com/echo"), headers,
                    session -> {
                        // Dummy request to start stream
                        Disposable send = Flux.interval(Duration.ofMillis(500)).flatMap(i -> {
                            WebSocketMessage message = session.textMessage("Hi " + i);
                            return session.send(Mono.just(message));
                        }).log("send").subscribe();
                        return session.receive()
                                .doOnNext(sink::next)
                                .doOnError(sink::error)
                                .doFinally(x -> {
                                    send.dispose();
                                    session.close();
                                })
                                .log("receive")
                                .then();
                    })
                    .doOnError(sink::error)
                    .subscribe();
            sink.onCancel(receive);
        });
        return messages
                .map(WebSocketMessage::getPayloadAsText)
                .log("demo");
    }
}

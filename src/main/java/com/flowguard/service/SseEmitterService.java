package com.flowguard.service;

import com.flowguard.event.RequestEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages all active SSE (Server-Sent Events) connections.
 * When a request event arrives, broadcasts it to every connected browser tab
 * instantly.
 */
@Service
@Slf4j
public class SseEmitterService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(0L); // 0 = never timeout
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        log.debug("New SSE client connected. Total clients: {}", emitters.size());
        return emitter;
    }

    public void broadcast(RequestEvent event) {
        if (emitters.isEmpty())
            return;

        String json;
        try {
            json = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            log.warn("Failed to serialize SSE event", e);
            return;
        }

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("request")
                        .data(json));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }

    public int getConnectedClients() {
        return emitters.size();
    }
}

package com.flowguard.controller;

import com.flowguard.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/v1/api/stream")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SseController {

    private final SseEmitterService sseEmitterService;

    /**
     * Browser connects here to receive real-time request events.
     * Dashboard subscribes with: new EventSource('/api/stream/events')
     */
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents() {
        return sseEmitterService.createEmitter();
    }

    @GetMapping("/clients")
    public int connectedClients() {
        return sseEmitterService.getConnectedClients();
    }
}

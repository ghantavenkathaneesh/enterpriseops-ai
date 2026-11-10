package com.enterpriseops.monitoring;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Fan-out of server-sent events to connected dashboards. */
@Component
public class SseHub {
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter e = new SseEmitter(30 * 60 * 1000L);
        emitters.add(e);
        e.onCompletion(() -> emitters.remove(e));
        e.onTimeout(() -> emitters.remove(e));
        e.onError(t -> emitters.remove(e));
        return e;
    }

    public void publish(String eventName, Object data) {
        for (SseEmitter e : emitters) {
            try {
                e.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException | IllegalStateException ex) {
                emitters.remove(e);
            }
        }
    }

    public int subscriberCount() { return emitters.size(); }
}

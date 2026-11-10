package com.enterpriseops.ai;

import com.enterpriseops.config.AppProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Calls the Python FastAPI service. Every method degrades to Optional.empty() when the service is down or disabled. */
@Component
public class AiClient {
    private static final Logger log = LoggerFactory.getLogger(AiClient.class);

    public record Classification(String category, double confidence, @JsonProperty("model_version") String modelVersion) {}
    public record AnomalyResult(@JsonProperty("is_anomaly") boolean anomaly, double score, @JsonProperty("model_version") String modelVersion) {}
    public record Recommendation(String category, double confidence, @JsonProperty("model_version") String modelVersion,
                                 @JsonProperty("recommended_actions") List<String> recommendedActions, String note) {}

    private final RestClient rest;
    private final boolean enabled;

    public AiClient(AppProperties props) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofSeconds(props.ai().timeoutSeconds()));
        f.setReadTimeout(Duration.ofSeconds(props.ai().timeoutSeconds()));
        this.rest = RestClient.builder().baseUrl(props.ai().baseUrl()).requestFactory(f).build();
        this.enabled = props.ai().enabled();
    }

    public Optional<Classification> classify(String text) {
        return call(() -> rest.post().uri("/api/v1/ai/classify-incident").body(Map.of("text", clip(text)))
            .retrieve().body(Classification.class));
    }

    public Optional<Recommendation> recommend(String text) {
        return call(() -> rest.post().uri("/api/v1/ai/recommend-actions").body(Map.of("text", clip(text)))
            .retrieve().body(Recommendation.class));
    }

    public Optional<AnomalyResult> detectAnomaly(double cpu, double ram, double disk, double networkMbps) {
        return call(() -> rest.post().uri("/api/v1/ai/detect-anomaly")
            .body(Map.of("cpu", cpu, "ram", ram, "disk", disk, "network_mbps", networkMbps))
            .retrieve().body(AnomalyResult.class));
    }

    public boolean isHealthy() {
        return call(() -> rest.get().uri("/health").retrieve().body(Map.class)).isPresent();
    }

    private static String clip(String s) { return s.length() > 2000 ? s.substring(0, 2000) : s; }

    private <T> Optional<T> call(Supplier<T> s) {
        if (!enabled) return Optional.empty();
        try {
            return Optional.ofNullable(s.get());
        } catch (RestClientException e) {
            log.warn("AI service unavailable, continuing without it: {}", e.getMessage());
            return Optional.empty();
        }
    }
}

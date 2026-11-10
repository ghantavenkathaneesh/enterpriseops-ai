package com.enterpriseops.ai;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "ai_predictions") @Getter @Setter @NoArgsConstructor
public class AiPrediction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String kind;
    @Column(name = "ticket_id") private Long ticketId;
    @Column(name = "input_text", columnDefinition = "text") private String inputText;
    @Column(name = "predicted_label", nullable = false) private String predictedLabel;
    @Column(precision = 5, scale = 4) private BigDecimal confidence;
    @Column(name = "model_version", nullable = false) private String modelVersion;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = Instant.now(); }

    public static AiPrediction classification(Long ticketId, String text, String label, double confidence, String version) {
        AiPrediction p = new AiPrediction();
        p.kind = "CLASSIFICATION"; p.ticketId = ticketId; p.inputText = text; p.predictedLabel = label;
        p.confidence = BigDecimal.valueOf(confidence).setScale(4, RoundingMode.HALF_UP); p.modelVersion = version;
        return p;
    }

    public static AiPrediction anomaly(String label, double score, String version) {
        AiPrediction p = new AiPrediction();
        p.kind = "ANOMALY"; p.predictedLabel = label;
        p.confidence = null; p.modelVersion = version; p.inputText = "score=" + score;
        return p;
    }
}

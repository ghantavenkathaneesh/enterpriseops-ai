package com.enterpriseops.ai;

import com.enterpriseops.common.BadRequestException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','SUPPORT_AGENT')")
public class AiController {
    public record TextRequest(@NotBlank @Size(min = 3, max = 2000) String text) {}

    private final AiClient ai;

    public AiController(AiClient ai) { this.ai = ai; }

    @GetMapping("/status")
    public Map<String, Object> status() { return Map.of("aiServiceHealthy", ai.isHealthy()); }

    @PostMapping("/classify")
    public ResponseEntity<?> classify(@Valid @RequestBody TextRequest r) {
        return ai.classify(r.text()).<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(this::unavailable);
    }

    @PostMapping("/recommend")
    public ResponseEntity<?> recommend(@Valid @RequestBody TextRequest r) {
        return ai.recommend(r.text()).<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(this::unavailable);
    }

    private ResponseEntity<?> unavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", "AI service is unavailable"));
    }
}

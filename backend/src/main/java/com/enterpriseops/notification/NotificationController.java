package com.enterpriseops.notification;

import com.enterpriseops.common.NotFoundException;
import com.enterpriseops.common.PageResponse;
import com.enterpriseops.security.CurrentUser;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationRepository repo;

    public NotificationController(NotificationRepository repo) { this.repo = repo; }

    @GetMapping
    public PageResponse<Notification> mine(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(repo.findByUserId(CurrentUser.get().getUserId(),
            PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"))), n -> n);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread() { return Map.of("count", repo.countByUserIdAndReadFalse(CurrentUser.get().getUserId())); }

    @PostMapping("/{id}/read")
    @Transactional
    public Notification markRead(@PathVariable Long id) {
        Notification n = repo.findById(id).filter(x -> x.getUserId().equals(CurrentUser.get().getUserId()))
            .orElseThrow(() -> new NotFoundException("Notification", id));
        n.setRead(true);
        return repo.save(n);
    }

    @PostMapping("/read-all")
    @Transactional
    public Map<String, Integer> readAll() { return Map.of("updated", repo.markAllRead(CurrentUser.get().getUserId())); }
}

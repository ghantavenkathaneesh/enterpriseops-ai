package com.enterpriseops.notification;

import com.enterpriseops.user.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final NotificationRepository repo;
    private final UserRepository users;

    public NotificationService(NotificationRepository repo, UserRepository users) { this.repo = repo; this.users = users; }

    public void notifyUser(Long userId, String type, String message, String link) {
        Notification n = new Notification();
        n.setUserId(userId); n.setType(type); n.setMessage(message); n.setLink(link);
        repo.save(n);
    }

    public void notifyRoles(List<String> roles, String type, String message, String link) {
        users.findByRoleNameInAndActiveTrue(roles).forEach(u -> notifyUser(u.getId(), type, message, link));
    }
}

package vn.vieshift.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.vieshift.entity.Notification;
import vn.vieshift.entity.User;
import vn.vieshift.repository.UserRepository;
import vn.vieshift.service.NotificationService;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationApiController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationApiController(NotificationService notificationService,
                                     UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    @GetMapping
    public ResponseEntity<?> getNotifications(@RequestParam(value = "limit", defaultValue = "10") int limit,
                                              Authentication auth) {
        User user = getAuthenticatedUser(auth);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        List<Notification> list = notificationService.getUserNotifications(user.getId(), limit);
        long unreadCount = notificationService.getUnreadCount(user.getId());

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        List<Map<String, Object>> dtos = list.stream().map(n -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", n.getId());
            map.put("title", n.getTitle());
            map.put("content", n.getContent());
            map.put("type", n.getType().name());
            map.put("typeDisplayName", n.getType().getDisplayName());
            map.put("icon", n.getType().getIcon());
            map.put("color", n.getType().getColor());
            map.put("link", n.getLink() != null ? n.getLink() : "#");
            map.put("isRead", n.isRead());
            map.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().format(dtf) : "");
            return map;
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("unreadCount", unreadCount);
        response.put("notifications", dtos);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable("id") Long id, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        notificationService.markAsRead(id, user.getId());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(Authentication auth) {
        User user = getAuthenticatedUser(auth);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(Map.of("success", true));
    }
}

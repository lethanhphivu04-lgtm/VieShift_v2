package vn.vieshift.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.vieshift.entity.ChatMessage;
import vn.vieshift.entity.User;
import vn.vieshift.repository.UserRepository;
import vn.vieshift.service.ChatService;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatApiController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    public ChatApiController(ChatService chatService, UserRepository userRepository) {
        this.chatService = chatService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    @GetMapping("/{jobId}/messages")
    public ResponseEntity<?> getMessages(@PathVariable("jobId") Long jobId, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            List<ChatMessage> messages = chatService.getMessages(jobId, user.getId());
            chatService.markAsRead(jobId, user.getId());

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm, dd/MM");
            List<Map<String, Object>> dtos = messages.stream().map(m -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", m.getId());
                map.put("senderId", m.getSender().getId());
                map.put("senderName", m.getSender().getFullName());
                map.put("isMe", m.getSender().getId().equals(user.getId()));
                map.put("content", m.getContent());
                map.put("attachmentUrl", m.getAttachmentUrl());
                map.put("createdAt", m.getCreatedAt() != null ? m.getCreatedAt().format(dtf) : "");
                return map;
            }).toList();

            return ResponseEntity.ok(Map.of("messages", dtos, "currentUserId", user.getId()));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{jobId}/send")
    public ResponseEntity<?> sendMessage(@PathVariable("jobId") Long jobId,
                                         @RequestParam(value = "content", required = false) String content,
                                         @RequestParam(value = "image", required = false) MultipartFile image,
                                         Authentication auth) {
        User user = getAuthenticatedUser(auth);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            ChatMessage msg = chatService.sendMessage(jobId, user.getId(), content, image);
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm, dd/MM");

            Map<String, Object> dto = new HashMap<>();
            dto.put("id", msg.getId());
            dto.put("senderId", msg.getSender().getId());
            dto.put("senderName", msg.getSender().getFullName());
            dto.put("isMe", true);
            dto.put("content", msg.getContent());
            dto.put("attachmentUrl", msg.getAttachmentUrl());
            dto.put("createdAt", msg.getCreatedAt() != null ? msg.getCreatedAt().format(dtf) : "");

            return ResponseEntity.ok(dto);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}

package vn.vieshift.service;

import org.springframework.web.multipart.MultipartFile;
import vn.vieshift.entity.ChatMessage;

import java.util.List;

public interface ChatService {

    ChatMessage sendMessage(Long jobId, Long senderId, String content, MultipartFile attachment);

    List<ChatMessage> getMessages(Long jobId, Long userId);

    void markAsRead(Long jobId, Long userId);
}

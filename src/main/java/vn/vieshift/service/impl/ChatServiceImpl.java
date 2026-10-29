package vn.vieshift.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import vn.vieshift.entity.ChatMessage;
import vn.vieshift.entity.Job;
import vn.vieshift.entity.NotificationType;
import vn.vieshift.entity.User;
import vn.vieshift.exception.ResourceNotFoundException;
import vn.vieshift.exception.UnauthorizedActionException;
import vn.vieshift.repository.ChatMessageRepository;
import vn.vieshift.repository.JobRepository;
import vn.vieshift.repository.UserRepository;
import vn.vieshift.service.ChatService;
import vn.vieshift.service.NotificationService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);
    private final Path chatUploadRoot = Paths.get("uploads/chat").toAbsolutePath().normalize();

    private final ChatMessageRepository chatMessageRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ChatServiceImpl(ChatMessageRepository chatMessageRepository,
                           JobRepository jobRepository,
                           UserRepository userRepository,
                           NotificationService notificationService) {
        this.chatMessageRepository = chatMessageRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Override
    public ChatMessage sendMessage(Long jobId, Long senderId, String content, MultipartFile attachment) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc ID: " + jobId));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người gửi ID: " + senderId));

        User customer = job.getCustomer();
        User vieker = job.getVieker();

        if (vieker == null) {
            throw new UnauthorizedActionException("Công việc này chưa có Thợ nhận, chưa thể nhắn tin");
        }

        User recipient;
        if (sender.getId().equals(customer.getId())) {
            recipient = vieker;
        } else if (sender.getId().equals(vieker.getId())) {
            recipient = customer;
        } else {
            throw new UnauthorizedActionException("Bạn không có quyền nhắn tin trong đơn hàng này");
        }

        String attachmentUrl = null;
        if (attachment != null && !attachment.isEmpty()) {
            try {
                if (!Files.exists(chatUploadRoot)) {
                    Files.createDirectories(chatUploadRoot);
                }
                String originalFilename = StringUtils.cleanPath(attachment.getOriginalFilename() != null ? attachment.getOriginalFilename() : "chat_img.jpg");
                String storedFilename = "chat_" + jobId + "_" + UUID.randomUUID().toString().substring(0, 8) + "_" + originalFilename;
                Path targetLocation = chatUploadRoot.resolve(storedFilename).normalize();

                Files.copy(attachment.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
                attachmentUrl = "/uploads/chat/" + storedFilename;
            } catch (IOException e) {
                log.error("Lỗi lưu ảnh chat", e);
            }
        }

        String messageContent = content != null ? content.trim() : "";
        if (messageContent.isEmpty() && attachmentUrl == null) {
            throw new IllegalArgumentException("Nội dung tin nhắn không được để trống");
        }

        ChatMessage chatMessage = new ChatMessage(job, sender, recipient, messageContent, attachmentUrl);
        ChatMessage saved = chatMessageRepository.save(chatMessage);

        // Gửi thông báo đến người nhận
        try {
            String link = recipient.getId().equals(customer.getId()) ? "/customer/jobs/" + job.getId() : "/vieker?tab=my-jobs";
            String preview = messageContent.length() > 50 ? messageContent.substring(0, 50) + "..." : messageContent;
            if (preview.isEmpty() && attachmentUrl != null) preview = "[Đã gửi một hình ảnh]";

            notificationService.notifyUser(
                    recipient,
                    sender.getFullName() + " nhắn cho bạn",
                    "Đơn #" + job.getId() + " (" + job.getTitle() + "): " + preview,
                    link,
                    NotificationType.SYSTEM
            );
        } catch (Exception ignored) {}

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessage> getMessages(Long jobId, Long userId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc ID: " + jobId));

        boolean isCustomer = job.getCustomer().getId().equals(userId);
        boolean isVieker = job.getVieker() != null && job.getVieker().getId().equals(userId);

        if (!isCustomer && !isVieker) {
            throw new UnauthorizedActionException("Bạn không có quyền xem tin nhắn của đơn hàng này");
        }

        return chatMessageRepository.findByJobIdOrderByCreatedAtAsc(jobId);
    }

    @Override
    public void markAsRead(Long jobId, Long userId) {
        chatMessageRepository.markAllAsReadForRecipient(jobId, userId);
    }
}

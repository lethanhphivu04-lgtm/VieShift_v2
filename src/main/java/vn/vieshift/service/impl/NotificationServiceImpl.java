package vn.vieshift.service.impl;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vieshift.entity.Notification;
import vn.vieshift.entity.NotificationType;
import vn.vieshift.entity.User;
import vn.vieshift.entity.VerificationStatus;
import vn.vieshift.entity.ViekerProfile;
import vn.vieshift.repository.NotificationRepository;
import vn.vieshift.repository.ViekerProfileRepository;
import vn.vieshift.service.NotificationService;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ViekerProfileRepository profileRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   ViekerProfileRepository profileRepository) {
        this.notificationRepository = notificationRepository;
        this.profileRepository = profileRepository;
    }

    @Override
    @Transactional
    public Notification notifyUser(User recipient, String title, String content, String link, NotificationType type) {
        if (recipient == null) {
            return null;
        }
        Notification notification = new Notification(recipient, title, content, link, type);
        return notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void notifyWorkersOfNewJob(String title, String content, String link, Long categoryId, String zone) {
        List<ViekerProfile> approvedViekers = profileRepository.findByVerificationStatusOrderByCreatedAtDesc(VerificationStatus.APPROVED);
        for (ViekerProfile vp : approvedViekers) {
            if (vp.getUser() != null) {
                notifyUser(vp.getUser(), title, content, link, NotificationType.JOB_NEW);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(Long userId, int limit) {
        int pageSize = limit > 0 ? Math.min(limit, 50) : 15;
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, pageSize));
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUser() != null && n.getUser().getId().equals(userId)) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }
}

package vn.vieshift.service;

import vn.vieshift.entity.Notification;
import vn.vieshift.entity.NotificationType;
import vn.vieshift.entity.User;

import java.util.List;

public interface NotificationService {

    Notification notifyUser(User recipient, String title, String content, String link, NotificationType type);

    void notifyWorkersOfNewJob(String title, String content, String link, Long categoryId, String zone);

    List<Notification> getUserNotifications(Long userId, int limit);

    long getUnreadCount(Long userId);

    void markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);
}

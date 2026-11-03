package vn.vieshift.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.vieshift.entity.ChatMessage;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByJobIdOrderByCreatedAtAsc(Long jobId);

    long countByJobIdAndRecipientIdAndIsReadFalse(Long jobId, Long recipientId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.job.id = :jobId AND m.recipient.id = :recipientId AND m.isRead = false")
    void markAllAsReadForRecipient(@Param("jobId") Long jobId, @Param("recipientId") Long recipientId);
}

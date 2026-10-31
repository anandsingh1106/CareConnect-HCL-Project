package com.careconnect.messaging.repository;

import com.careconnect.messaging.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("select m from Message m where m.sender.id = :userId or m.recipient.id = :userId order by m.createdAt desc")
    List<Message> findAllForUser(@Param("userId") Long userId);

    @Query("""
            select m from Message m
            where (m.sender.id = :a and m.recipient.id = :b) or (m.sender.id = :b and m.recipient.id = :a)
            order by m.createdAt asc
            """)
    List<Message> findThread(@Param("a") Long userA, @Param("b") Long userB);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Message m set m.readAt = :now where m.recipient.id = :me and m.sender.id = :other and m.readAt is null")
    int markThreadRead(@Param("me") Long me, @Param("other") Long other, @Param("now") LocalDateTime now);
}

package com.hrconnect.payroll.infrastructure.dlq;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FailedMessageRepository extends JpaRepository<FailedMessage, Long> {

    List<FailedMessage> findByTopicOrderByCreatedAtDesc(String topic);
}

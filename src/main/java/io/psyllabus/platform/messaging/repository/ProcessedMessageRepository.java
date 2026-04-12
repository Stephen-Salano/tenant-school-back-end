package io.psyllabus.platform.messaging.repository;

import io.psyllabus.platform.messaging.entities.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, String> {
}

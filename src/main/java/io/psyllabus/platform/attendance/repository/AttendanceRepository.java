package io.psyllabus.platform.attendance.repository;

import io.psyllabus.platform.attendance.entities.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceRecord, UUID>, JpaSpecificationExecutor<AttendanceRecord> {

    boolean existsByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    boolean existsByTenantIdAndClassIdAndDate(UUID tenantId, UUID classId, LocalDate date);

    long countByTenantIdAndClassIdAndDate(UUID tenantId, UUID classId, LocalDate date);

    List<AttendanceRecord> findAllByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    List<AttendanceRecord> findAllByTenantIdAndClassIdAndDateOrderByCreatedAtAsc(UUID tenantId, UUID classId, LocalDate date);
}

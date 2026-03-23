package io.psyllabus.platform.school.repository;

import io.psyllabus.platform.school.entities.SchoolProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SchoolProfileRepository extends JpaRepository<SchoolProfile, UUID> {
    /**
     * Fetch the profile for a given school.
     * Returns {@link Optional#empty()} when the admin has not yet completed the
     * settings wizard — callers should handle this gracefully and return sensible defaults.
     *
     * @param schoolId the school (tenant) UUID
     * @return the school's profile if it exists
     */
    Optional<SchoolProfile> findBySchoolId(UUID schoolId);

    /**
     * Check whether a profile record already exists for a school.
     * Used during onboarding to decide whether to INSERT or UPDATE.
     *
     * @param schoolId the school (tenant) UUID
     * @return {@code true} if a profile row exists
     */
    boolean existsBySchoolId(UUID schoolId);
}

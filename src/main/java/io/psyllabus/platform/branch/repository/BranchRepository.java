package io.psyllabus.platform.branch.repository;

import io.psyllabus.platform.branch.entities.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BranchRepository extends JpaRepository<Branch, UUID> {
}

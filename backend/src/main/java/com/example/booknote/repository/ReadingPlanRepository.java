
package com.example.booknote.repository;

import com.example.booknote.entity.ReadingPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingPlanRepository extends JpaRepository<ReadingPlan, Long> {
    List<ReadingPlan> findByBookIdOrderByCreatedAtDesc(Long bookId);

    Optional<ReadingPlan> findFirstByBookIdOrderByCreatedAtDesc(Long bookId);
}

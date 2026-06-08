
package com.example.booknote.repository;

import com.example.booknote.entity.ReadingPlanSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReadingPlanSegmentRepository extends JpaRepository<ReadingPlanSegment, Long> {
    List<ReadingPlanSegment> findByReadingPlanIdOrderBySortOrderAsc(Long planId);

    void deleteByReadingPlanId(Long planId);
}

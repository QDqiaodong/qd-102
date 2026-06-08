
package com.example.booknote.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reading_plan_segments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadingPlanSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private ReadingPlan readingPlan;

    @Column(name = "segment_title", length = 200)
    private String segmentTitle;

    @Column(name = "start_page", nullable = false)
    private Integer startPage;

    @Column(name = "end_page", nullable = false)
    private Integer endPage;

    @Column(name = "estimated_completion_date")
    private LocalDate estimatedCompletionDate;

    @Column(name = "current_page")
    private Integer currentPage;

    @Column(name = "actual_completion_date")
    private LocalDate actualCompletionDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SegmentStatus status;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = SegmentStatus.NOT_STARTED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum SegmentStatus {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED
    }
}

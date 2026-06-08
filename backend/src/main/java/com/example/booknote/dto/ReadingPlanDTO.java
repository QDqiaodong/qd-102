
package com.example.booknote.dto;

import com.example.booknote.entity.ReadingPlan;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadingPlanDTO {
    private Long id;
    private Long bookId;
    private String title;
    private Integer totalPages;
    private List<ReadingPlanSegmentDTO> segments = new ArrayList<>();
    private Double overallProgress;
    private Integer completedSegments;
    private Integer totalSegments;
    private String createdAt;
    private String updatedAt;

    public static ReadingPlanDTO fromEntity(ReadingPlan plan) {
        ReadingPlanDTO dto = new ReadingPlanDTO();
        dto.setId(plan.getId());
        dto.setBookId(plan.getBook() != null ? plan.getBook().getId() : null);
        dto.setTitle(plan.getTitle());
        dto.setTotalPages(plan.getTotalPages());
        dto.setCreatedAt(plan.getCreatedAt() != null ? plan.getCreatedAt().toString() : null);
        dto.setUpdatedAt(plan.getUpdatedAt() != null ? plan.getUpdatedAt().toString() : null);

        if (plan.getSegments() != null && !plan.getSegments().isEmpty()) {
            List<ReadingPlanSegmentDTO> segmentDTOs = plan.getSegments().stream()
                    .map(ReadingPlanSegmentDTO::fromEntity)
                    .collect(Collectors.toList());
            dto.setSegments(segmentDTOs);
            dto.setTotalSegments(segmentDTOs.size());

            long completed = segmentDTOs.stream()
                    .filter(s -> "COMPLETED".equals(s.getStatus()))
                    .count();
            dto.setCompletedSegments((int) completed);

            double totalProgress = segmentDTOs.stream()
                    .mapToDouble(s -> s.getProgressPercent() != null ? s.getProgressPercent() : 0.0)
                    .average()
                    .orElse(0.0);
            dto.setOverallProgress(Math.round(totalProgress * 10.0) / 10.0);
        } else {
            dto.setTotalSegments(0);
            dto.setCompletedSegments(0);
            dto.setOverallProgress(0.0);
        }

        return dto;
    }

    public ReadingPlan toEntity() {
        ReadingPlan plan = new ReadingPlan();
        plan.setId(this.id);
        plan.setTitle(this.title);
        plan.setTotalPages(this.totalPages);

        if (this.segments != null) {
            plan.setSegments(this.segments.stream()
                    .map(ReadingPlanSegmentDTO::toEntity)
                    .collect(Collectors.toList()));
        }

        return plan;
    }
}


package com.example.booknote.dto;

import com.example.booknote.entity.ReadingPlanSegment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadingPlanSegmentDTO {
    private Long id;
    private String segmentTitle;
    private Integer startPage;
    private Integer endPage;
    private LocalDate estimatedCompletionDate;
    private Integer currentPage;
    private LocalDate actualCompletionDate;
    private String status;
    private Integer sortOrder;
    private Double progressPercent;
    private Integer pageCount;

    public static ReadingPlanSegmentDTO fromEntity(ReadingPlanSegment segment) {
        ReadingPlanSegmentDTO dto = new ReadingPlanSegmentDTO();
        dto.setId(segment.getId());
        dto.setSegmentTitle(segment.getSegmentTitle());
        dto.setStartPage(segment.getStartPage());
        dto.setEndPage(segment.getEndPage());
        dto.setEstimatedCompletionDate(segment.getEstimatedCompletionDate());
        dto.setCurrentPage(segment.getCurrentPage());
        dto.setActualCompletionDate(segment.getActualCompletionDate());
        dto.setStatus(segment.getStatus() != null ? segment.getStatus().name() : null);
        dto.setSortOrder(segment.getSortOrder());

        int pageCount = segment.getEndPage() - segment.getStartPage() + 1;
        dto.setPageCount(pageCount);

        if (pageCount > 0 && segment.getCurrentPage() != null) {
            int readPages = Math.max(0, segment.getCurrentPage() - segment.getStartPage() + 1);
            double percent = Math.min(100.0, (readPages * 100.0) / pageCount);
            dto.setProgressPercent(Math.round(percent * 10.0) / 10.0);
        } else {
            dto.setProgressPercent(0.0);
        }

        return dto;
    }

    public ReadingPlanSegment toEntity() {
        ReadingPlanSegment segment = new ReadingPlanSegment();
        segment.setId(this.id);
        segment.setSegmentTitle(this.segmentTitle);
        segment.setStartPage(this.startPage);
        segment.setEndPage(this.endPage);
        segment.setEstimatedCompletionDate(this.estimatedCompletionDate);
        segment.setCurrentPage(this.currentPage);
        segment.setActualCompletionDate(this.actualCompletionDate);
        segment.setStatus(this.status != null ? ReadingPlanSegment.SegmentStatus.valueOf(this.status) : null);
        segment.setSortOrder(this.sortOrder);
        return segment;
    }
}

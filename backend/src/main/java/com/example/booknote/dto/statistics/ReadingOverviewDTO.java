
package com.example.booknote.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadingOverviewDTO {
    private List<StatusDistributionDTO> statusDistribution;
    private List<CategoryDistributionDTO> categoryDistribution;
    private List<MonthlyNoteOutputDTO> monthlyNoteOutput;
    private List<ProgressRangeDTO> progressRange;
    private long totalBooks;
    private long totalNotes;
}

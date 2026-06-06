
package com.example.booknote.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgressRangeDTO {
    private String range;
    private long count;
    private double percentage;
}

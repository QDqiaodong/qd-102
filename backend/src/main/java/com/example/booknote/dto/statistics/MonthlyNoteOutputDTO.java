
package com.example.booknote.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyNoteOutputDTO {
    private String month;
    private long noteCount;
}

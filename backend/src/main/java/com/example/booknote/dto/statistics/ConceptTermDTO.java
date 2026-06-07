
package com.example.booknote.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConceptTermDTO {
    private String term;
    private String type;
    private long frequency;
    private long bookCount;
    private long noteCount;
    private double score;
}

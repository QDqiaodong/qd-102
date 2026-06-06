
package com.example.booknote.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimilarNotePairDTO {
    private NoteDTO note1;
    private NoteDTO note2;
    private double similarityScore;
    private double titleSimilarity;
    private double contentSimilarity;
    private String similarityReason;
}

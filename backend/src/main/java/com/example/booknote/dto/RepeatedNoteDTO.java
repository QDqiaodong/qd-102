
package com.example.booknote.dto;

import com.example.booknote.entity.RepeatedNote;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepeatedNoteDTO {
    private Long id;
    private NoteDTO note;
    private LocalDateTime nextReviewTime;
    private Integer reviewCount;
    private LocalDateTime lastReviewTime;
    private LocalDateTime createdAt;
    private String status;
    private long daysUntilReview;

    public static RepeatedNoteDTO fromEntity(RepeatedNote repeatedNote) {
        RepeatedNoteDTO dto = new RepeatedNoteDTO();
        dto.setId(repeatedNote.getId());
        dto.setNote(NoteDTO.fromEntity(repeatedNote.getNote()));
        dto.setNextReviewTime(repeatedNote.getNextReviewTime());
        dto.setReviewCount(repeatedNote.getReviewCount());
        dto.setLastReviewTime(repeatedNote.getLastReviewTime());
        dto.setCreatedAt(repeatedNote.getCreatedAt());

        LocalDateTime now = LocalDateTime.now();
        if (repeatedNote.getNextReviewTime().isBefore(now) || repeatedNote.getNextReviewTime().isEqual(now)) {
            dto.setStatus("due");
            long days = java.time.Duration.between(repeatedNote.getNextReviewTime(), now).toDays();
            dto.setDaysUntilReview(-days);
        } else {
            dto.setStatus("upcoming");
            long days = java.time.Duration.between(now, repeatedNote.getNextReviewTime()).toDays() + 1;
            dto.setDaysUntilReview(days);
        }

        return dto;
    }
}

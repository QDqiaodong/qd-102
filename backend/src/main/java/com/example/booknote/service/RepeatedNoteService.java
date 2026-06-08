
package com.example.booknote.service;

import com.example.booknote.dto.RepeatedNoteDTO;
import com.example.booknote.entity.Note;
import com.example.booknote.entity.RepeatedNote;
import com.example.booknote.repository.NoteRepository;
import com.example.booknote.repository.RepeatedNoteRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RepeatedNoteService {

    private static final int[] REVIEW_INTERVALS_DAYS = {1, 2, 4, 7, 15, 30, 60, 120, 240};

    @Autowired
    private RepeatedNoteRepository repeatedNoteRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Transactional
    public RepeatedNoteDTO addToRepeatList(Long noteId) {
        if (repeatedNoteRepository.existsByNoteId(noteId)) {
            throw new IllegalArgumentException("该笔记已在复读清单中");
        }

        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("笔记不存在"));

        RepeatedNote repeatedNote = new RepeatedNote();
        repeatedNote.setNote(note);
        repeatedNote.setReviewCount(0);
        repeatedNote.setNextReviewTime(calculateNextReviewTime(0));

        RepeatedNote saved = repeatedNoteRepository.save(repeatedNote);
        return RepeatedNoteDTO.fromEntity(saved);
    }

    @Transactional
    public void removeFromRepeatList(Long noteId) {
        if (!repeatedNoteRepository.existsByNoteId(noteId)) {
            throw new IllegalArgumentException("该笔记不在复读清单中");
        }
        repeatedNoteRepository.deleteByNoteId(noteId);
    }

    @Transactional
    public RepeatedNoteDTO markAsReviewed(Long noteId) {
        RepeatedNote repeatedNote = repeatedNoteRepository.findByNoteId(noteId)
                .orElseThrow(() -> new IllegalArgumentException("该笔记不在复读清单中"));

        int newReviewCount = repeatedNote.getReviewCount() + 1;
        repeatedNote.setReviewCount(newReviewCount);
        repeatedNote.setLastReviewTime(LocalDateTime.now());
        repeatedNote.setNextReviewTime(calculateNextReviewTime(newReviewCount));

        RepeatedNote saved = repeatedNoteRepository.save(repeatedNote);
        return RepeatedNoteDTO.fromEntity(saved);
    }

    public Optional<RepeatedNoteDTO> getByNoteId(Long noteId) {
        return repeatedNoteRepository.findByNoteId(noteId)
                .map(RepeatedNoteDTO::fromEntity);
    }

    public RepeatedNoteListDTO getAllRepeatedNotes() {
        List<RepeatedNote> allNotes = repeatedNoteRepository.findAllWithNoteAndBookOrderByNextReviewTime();
        LocalDateTime now = LocalDateTime.now();

        List<RepeatedNoteDTO> dueNotes = new ArrayList<>();
        List<RepeatedNoteDTO> upcomingNotes = new ArrayList<>();

        for (RepeatedNote rn : allNotes) {
            RepeatedNoteDTO dto = RepeatedNoteDTO.fromEntity(rn);
            if (rn.getNextReviewTime().isBefore(now) || rn.getNextReviewTime().isEqual(now)) {
                dueNotes.add(dto);
            } else {
                upcomingNotes.add(dto);
            }
        }

        RepeatedNoteListDTO result = new RepeatedNoteListDTO();
        result.setDueNotes(dueNotes);
        result.setUpcomingNotes(upcomingNotes);
        result.setDueCount(dueNotes.size());
        result.setUpcomingCount(upcomingNotes.size());
        result.setTotalCount(allNotes.size());

        return result;
    }

    public long getDueCount() {
        return repeatedNoteRepository.countDueNotes(LocalDateTime.now());
    }

    public long getTotalCount() {
        return repeatedNoteRepository.countAllRepeatedNotes();
    }

    private LocalDateTime calculateNextReviewTime(int reviewCount) {
        int intervalIndex = Math.min(reviewCount, REVIEW_INTERVALS_DAYS.length - 1);
        int days = REVIEW_INTERVALS_DAYS[intervalIndex];
        return LocalDateTime.now().plusDays(days);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepeatedNoteListDTO {
        private List<RepeatedNoteDTO> dueNotes;
        private List<RepeatedNoteDTO> upcomingNotes;
        private int dueCount;
        private int upcomingCount;
        private int totalCount;
    }
}

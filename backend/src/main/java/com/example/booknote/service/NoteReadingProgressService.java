
package com.example.booknote.service;

import com.example.booknote.entity.NoteReadingProgress;
import com.example.booknote.repository.NoteReadingProgressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class NoteReadingProgressService {

    @Autowired
    private NoteReadingProgressRepository progressRepository;

    public Optional<NoteReadingProgress> getProgressByBookId(Long bookId) {
        return progressRepository.findByBookId(bookId);
    }

    @Transactional
    public NoteReadingProgress saveProgress(Long bookId, Long lastNoteId, Integer notePageIndex, Integer scrollTop) {
        NoteReadingProgress progress = progressRepository.findByBookId(bookId)
                .orElse(new NoteReadingProgress());

        progress.setBookId(bookId);
        progress.setLastNoteId(lastNoteId);
        progress.setNotePageIndex(notePageIndex);
        progress.setScrollTop(scrollTop);
        progress.setUpdatedAt(LocalDateTime.now());

        return progressRepository.save(progress);
    }

    @Transactional
    public void deleteProgress(Long bookId) {
        progressRepository.deleteByBookId(bookId);
    }

    public static class ProgressDTO {
        private Long bookId;
        private Long lastNoteId;
        private Integer notePageIndex;
        private Integer scrollTop;
        private LocalDateTime updatedAt;

        public ProgressDTO() {}

        public ProgressDTO(NoteReadingProgress progress) {
            this.bookId = progress.getBookId();
            this.lastNoteId = progress.getLastNoteId();
            this.notePageIndex = progress.getNotePageIndex();
            this.scrollTop = progress.getScrollTop();
            this.updatedAt = progress.getUpdatedAt();
        }

        public Long getBookId() { return bookId; }
        public void setBookId(Long bookId) { this.bookId = bookId; }
        public Long getLastNoteId() { return lastNoteId; }
        public void setLastNoteId(Long lastNoteId) { this.lastNoteId = lastNoteId; }
        public Integer getNotePageIndex() { return notePageIndex; }
        public void setNotePageIndex(Integer notePageIndex) { this.notePageIndex = notePageIndex; }
        public Integer getScrollTop() { return scrollTop; }
        public void setScrollTop(Integer scrollTop) { this.scrollTop = scrollTop; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }
}

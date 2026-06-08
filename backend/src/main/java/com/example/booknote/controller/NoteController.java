
package com.example.booknote.controller;

import com.example.booknote.dto.NoteDTO;
import com.example.booknote.entity.Note;
import com.example.booknote.service.NoteService;
import com.example.booknote.service.NoteReadingProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class NotePageDTO {
    private List<NoteDTO> content;
    private long totalElements;
    private int totalPages;
    
    public NotePageDTO(List<NoteDTO> content, long totalElements, int totalPages) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }
    
    public List<NoteDTO> getContent() { return content; }
    public void setContent(List<NoteDTO> content) { this.content = content; }
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}

@RestController
@RequestMapping("/api/notes")
public class NoteController {
    
    @Autowired
    private NoteService noteService;

    @Autowired
    private NoteReadingProgressService progressService;
    
    @GetMapping("/book/{bookId}")
    public ResponseEntity<NotePageDTO> getNotesByBookId(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Note> notes = noteService.getNotesByBookId(bookId, pageable);
        List<NoteDTO> dtos = notes.getContent().stream().map(NoteDTO::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(new NotePageDTO(dtos, notes.getTotalElements(), notes.getTotalPages()));
    }
    
    @GetMapping("/book/{bookId}/count")
    public ResponseEntity<Long> getNotesCountByBookId(@PathVariable Long bookId) {
        return ResponseEntity.ok(noteService.getNotesCountByBookId(bookId));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<NoteDTO> getNoteById(@PathVariable Long id) {
        return noteService.getNoteById(id)
                .map(note -> ResponseEntity.ok(NoteDTO.fromEntity(note)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/book/{bookId}")
    public ResponseEntity<NoteDTO> createNote(@PathVariable Long bookId, @RequestBody NoteDTO noteDTO) {
        Note note = noteDTO.toEntity();
        Note saved = noteService.createNote(bookId, note);
        return ResponseEntity.status(HttpStatus.CREATED).body(NoteDTO.fromEntity(saved));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<NoteDTO> updateNote(@PathVariable Long id, @RequestBody NoteDTO noteDTO) {
        Note note = noteDTO.toEntity();
        Note updated = noteService.updateNote(id, note);
        return ResponseEntity.ok(NoteDTO.fromEntity(updated));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id) {
        noteService.deleteNote(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<NoteDTO>> searchNotes(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Note> notes = noteService.searchNotesByContent(keyword, pageable);
        List<NoteDTO> dtos = notes.getContent().stream().map(NoteDTO::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @PostMapping("/draft/{bookId}")
    public ResponseEntity<Map<String, Object>> saveDraft(
            @PathVariable Long bookId,
            @RequestBody NoteService.DraftDTO draft) {
        NoteService.DraftResponse response = noteService.saveDraft(bookId, draft);
        return ResponseEntity.ok(Map.of(
            "draftId", response.getDraftId(),
            "savedAt", response.getSavedAt()
        ));
    }
    
    @GetMapping("/draft/{bookId}")
    public ResponseEntity<NoteService.DraftDTO> getDraft(@PathVariable Long bookId) {
        NoteService.DraftDTO draft = noteService.getDraft(bookId);
        if (draft == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(draft);
    }
    
    @DeleteMapping("/draft/{bookId}")
    public ResponseEntity<Void> deleteDraft(@PathVariable Long bookId) {
        noteService.deleteDraft(bookId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/book/{bookId}/reading-progress")
    public ResponseEntity<NoteReadingProgressService.ProgressDTO> getReadingProgress(@PathVariable Long bookId) {
        return progressService.getProgressByBookId(bookId)
                .map(progress -> ResponseEntity.ok(new NoteReadingProgressService.ProgressDTO(progress)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/book/{bookId}/reading-progress")
    public ResponseEntity<NoteReadingProgressService.ProgressDTO> saveReadingProgress(
            @PathVariable Long bookId,
            @RequestBody Map<String, Object> request) {
        Long lastNoteId = request.get("lastNoteId") != null ?
                Long.valueOf(request.get("lastNoteId").toString()) : null;
        Integer notePageIndex = request.get("notePageIndex") != null ?
                Integer.valueOf(request.get("notePageIndex").toString()) : null;
        Integer scrollTop = request.get("scrollTop") != null ?
                Integer.valueOf(request.get("scrollTop").toString()) : null;

        NoteReadingProgress progress = progressService.saveProgress(bookId, lastNoteId, notePageIndex, scrollTop);
        return ResponseEntity.ok(new NoteReadingProgressService.ProgressDTO(progress));
    }

    @DeleteMapping("/book/{bookId}/reading-progress")
    public ResponseEntity<Void> deleteReadingProgress(@PathVariable Long bookId) {
        progressService.deleteProgress(bookId);
        return ResponseEntity.noContent().build();
    }
}

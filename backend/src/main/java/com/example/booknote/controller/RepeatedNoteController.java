
package com.example.booknote.controller;

import com.example.booknote.dto.RepeatedNoteDTO;
import com.example.booknote.service.RepeatedNoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/repeated-notes")
public class RepeatedNoteController {

    @Autowired
    private RepeatedNoteService repeatedNoteService;

    @GetMapping
    public ResponseEntity<RepeatedNoteService.RepeatedNoteListDTO> getAllRepeatedNotes() {
        return ResponseEntity.ok(repeatedNoteService.getAllRepeatedNotes());
    }

    @GetMapping("/note/{noteId}")
    public ResponseEntity<RepeatedNoteDTO> getByNoteId(@PathVariable Long noteId) {
        Optional<RepeatedNoteDTO> result = repeatedNoteService.getByNoteId(noteId);
        return result.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/note/{noteId}")
    public ResponseEntity<RepeatedNoteDTO> addToRepeatList(@PathVariable Long noteId) {
        return ResponseEntity.ok(repeatedNoteService.addToRepeatList(noteId));
    }

    @DeleteMapping("/note/{noteId}")
    public ResponseEntity<Void> removeFromRepeatList(@PathVariable Long noteId) {
        repeatedNoteService.removeFromRepeatList(noteId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/note/{noteId}/review")
    public ResponseEntity<RepeatedNoteDTO> markAsReviewed(@PathVariable Long noteId) {
        return ResponseEntity.ok(repeatedNoteService.markAsReviewed(noteId));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        long dueCount = repeatedNoteService.getDueCount();
        long totalCount = repeatedNoteService.getTotalCount();
        return ResponseEntity.ok(Map.of(
                "dueCount", dueCount,
                "totalCount", totalCount
        ));
    }
}

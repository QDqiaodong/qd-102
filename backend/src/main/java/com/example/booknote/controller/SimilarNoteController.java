
package com.example.booknote.controller;

import com.example.booknote.dto.SimilarNotePairDTO;
import com.example.booknote.service.SimilarNoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/similar-notes")
public class SimilarNoteController {

    @Autowired
    private SimilarNoteService similarNoteService;

    @GetMapping
    public ResponseEntity<List<SimilarNotePairDTO>> findSimilarNotes(
            @RequestParam(required = false) Long bookId,
            @RequestParam(required = false, defaultValue = "0.6") Double threshold,
            @RequestParam(required = false, defaultValue = "false") Boolean crossBook) {
        List<SimilarNotePairDTO> similarNotes = similarNoteService.findSimilarNotes(bookId, threshold, crossBook);
        return ResponseEntity.ok(similarNotes);
    }

    @GetMapping("/note/{noteId}")
    public ResponseEntity<List<SimilarNotePairDTO>> findSimilarNotesForNote(
            @PathVariable Long noteId,
            @RequestParam(required = false, defaultValue = "0.6") Double threshold,
            @RequestParam(required = false, defaultValue = "false") Boolean crossBook) {
        List<SimilarNotePairDTO> similarNotes = similarNoteService.findSimilarNotesForNote(noteId, threshold, crossBook);
        return ResponseEntity.ok(similarNotes);
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getSimilarityStatistics(
            @RequestParam(required = false) Long bookId,
            @RequestParam(required = false, defaultValue = "false") Boolean crossBook) {
        Map<String, Object> statistics = similarNoteService.getSimilarityStatistics(bookId, crossBook);
        return ResponseEntity.ok(statistics);
    }
}

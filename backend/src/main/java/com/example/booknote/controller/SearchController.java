
package com.example.booknote.controller;

import com.example.booknote.dto.BookDTO;
import com.example.booknote.dto.NoteDTO;
import com.example.booknote.entity.Book;
import com.example.booknote.entity.Note;
import com.example.booknote.repository.BookRepository;
import com.example.booknote.repository.NoteRepository;
import com.example.booknote.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
public class SearchController {
    
    @Autowired
    private SearchService searchService;
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private NoteRepository noteRepository;
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> fullTextSearch(@RequestParam String keyword) throws IOException {
        List<SearchService.SearchResult> results = searchService.search(keyword);
        
        List<BookDTO> books = new ArrayList<>();
        List<NoteDTO> notes = new ArrayList<>();
        
        for (SearchService.SearchResult result : results) {
            if ("book".equals(result.getType())) {
                Optional<Book> book = bookRepository.findById(result.getId());
                book.ifPresent(b -> {
                    BookDTO dto = BookDTO.fromEntity(b);
                    dto.setHighlightedTitle(result.getHighlightedTitle());
                    dto.setHighlightedContent(result.getHighlightedContent());
                    books.add(dto);
                });
            } else if ("note".equals(result.getType())) {
                Optional<Note> note = noteRepository.findById(result.getId());
                note.ifPresent(n -> {
                    NoteDTO dto = NoteDTO.fromEntity(n);
                    dto.setHighlightedTitle(result.getHighlightedTitle());
                    dto.setHighlightedContent(result.getHighlightedContent());
                    dto.setHighlightedTags(result.getHighlightedTags());
                    notes.add(dto);
                });
            }
        }
        
        return ResponseEntity.ok(Map.of(
            "books", books,
            "notes", notes
        ));
    }

    @GetMapping("/advanced")
    public ResponseEntity<Map<String, Object>> advancedSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) List<Long> tagIds,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String searchType) throws IOException {
        
        LocalDate start = startDate != null && !startDate.isEmpty() ? LocalDate.parse(startDate) : null;
        LocalDate end = endDate != null && !endDate.isEmpty() ? LocalDate.parse(endDate) : null;
        
        SearchService.AdvancedSearchResult result = searchService.advancedSearch(
            keyword, status, category, tagIds, start, end, searchType
        );
        
        List<BookDTO> bookDTOs = result.getBooks().stream()
                .map(book -> BookDTO.fromEntity(book, book.getNotes() != null ? (long) book.getNotes().size() : 0L))
                .collect(Collectors.toList());
        
        List<NoteDTO> noteDTOs = result.getNotes().stream()
                .map(NoteDTO::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(Map.of(
            "books", bookDTOs,
            "notes", noteDTOs,
            "totalBooks", bookDTOs.size(),
            "totalNotes", noteDTOs.size()
        ));
    }
}

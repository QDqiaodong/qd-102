
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
                book.ifPresent(b -> books.add(BookDTO.fromEntity(b)));
            } else if ("note".equals(result.getType())) {
                Optional<Note> note = noteRepository.findById(result.getId());
                note.ifPresent(n -> notes.add(NoteDTO.fromEntity(n)));
            }
        }
        
        return ResponseEntity.ok(Map.of(
            "books", books,
            "notes", notes
        ));
    }
}

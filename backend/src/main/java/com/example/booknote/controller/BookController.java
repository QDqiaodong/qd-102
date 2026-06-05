
package com.example.booknote.controller;

import com.example.booknote.dto.BookDTO;
import com.example.booknote.entity.Book;
import com.example.booknote.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/books")
public class BookController {
    
    @Autowired
    private BookService bookService;
    
    @GetMapping
    public ResponseEntity<List<BookDTO>> filterBooks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer minProgress,
            @RequestParam(required = false) Integer maxProgress,
            @RequestParam(required = false) Boolean hasNotes) {
        Book.ReadingStatus statusEnum = null;
        if (status != null && !status.isEmpty()) {
            statusEnum = Book.ReadingStatus.valueOf(status);
        }
        List<Book> books = bookService.filterBooks(statusEnum, category, minProgress, maxProgress, hasNotes);
        List<BookDTO> dtos = books.stream()
                .map(book -> BookDTO.fromEntity(book, bookService.getNoteCountForBook(book.getId())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        return ResponseEntity.ok(bookService.getAllCategories());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<BookDTO> getBookById(@PathVariable Long id) {
        return bookService.getBookById(id)
                .map(book -> ResponseEntity.ok(BookDTO.fromEntity(book, bookService.getNoteCountForBook(book.getId()))))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<BookDTO> createBook(@RequestBody BookDTO bookDTO) {
        Book book = bookDTO.toEntity();
        Book saved = bookService.createBook(book);
        return ResponseEntity.status(HttpStatus.CREATED).body(BookDTO.fromEntity(saved));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<BookDTO> updateBook(@PathVariable Long id, @RequestBody BookDTO bookDTO) {
        Book book = bookDTO.toEntity();
        Book updated = bookService.updateBook(id, book);
        return ResponseEntity.ok(BookDTO.fromEntity(updated));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<BookDTO>> searchBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author) {
        List<Book> books;
        if (title != null && !title.isEmpty()) {
            books = bookService.searchBooksByTitle(title);
        } else if (author != null && !author.isEmpty()) {
            books = bookService.searchBooksByAuthor(author);
        } else {
            books = bookService.getAllBooks();
        }
        List<BookDTO> dtos = books.stream().map(BookDTO::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}

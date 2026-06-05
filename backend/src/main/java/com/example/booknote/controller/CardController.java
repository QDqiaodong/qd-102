
package com.example.booknote.controller;

import com.example.booknote.dto.NoteDTO;
import com.example.booknote.entity.Book;
import com.example.booknote.entity.Tag;
import com.example.booknote.service.CardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    @Autowired
    private CardService cardService;

    @GetMapping
    public ResponseEntity<List<NoteDTO>> getCards(
            @RequestParam(required = false) Long bookId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) Integer pageNumber) {
        List<NoteDTO> cards = cardService.getCards(bookId, tagId, pageNumber);
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/paged")
    public ResponseEntity<CardPageDTO> getCardsPaged(
            @RequestParam(required = false) Long bookId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NoteDTO> cardsPage = cardService.getCardsPaged(bookId, tagId, pageNumber, pageable);
        return ResponseEntity.ok(new CardPageDTO(
                cardsPage.getContent(),
                cardsPage.getTotalElements(),
                cardsPage.getTotalPages(),
                cardsPage.getNumber(),
                cardsPage.getSize()
        ));
    }

    @GetMapping("/books")
    public ResponseEntity<List<Book>> getAllBooks() {
        List<Book> books = cardService.getAllBooks();
        return ResponseEntity.ok(books);
    }

    @GetMapping("/tags")
    public ResponseEntity<List<Tag>> getAllTags() {
        List<Tag> tags = cardService.getAllTags();
        return ResponseEntity.ok(tags);
    }

    @GetMapping("/pages/{bookId}")
    public ResponseEntity<List<Integer>> getPageNumbers(@PathVariable Long bookId) {
        List<Integer> pages = cardService.getPageNumbersByBook(bookId);
        return ResponseEntity.ok(pages);
    }

    @GetMapping("/summary")
    public ResponseEntity<CardService.CardSummary> getSummary() {
        return ResponseEntity.ok(cardService.getCardSummary());
    }

    public static class CardPageDTO {
        private List<NoteDTO> content;
        private long totalElements;
        private int totalPages;
        private int number;
        private int size;

        public CardPageDTO(List<NoteDTO> content, long totalElements, int totalPages, int number, int size) {
            this.content = content;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
            this.number = number;
            this.size = size;
        }

        public List<NoteDTO> getContent() { return content; }
        public void setContent(List<NoteDTO> content) { this.content = content; }
        public long getTotalElements() { return totalElements; }
        public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
        public int getNumber() { return number; }
        public void setNumber(int number) { this.number = number; }
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
    }
}

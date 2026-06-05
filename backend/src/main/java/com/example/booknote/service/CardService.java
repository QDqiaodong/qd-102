
package com.example.booknote.service;

import com.example.booknote.dto.NoteDTO;
import com.example.booknote.entity.Book;
import com.example.booknote.entity.Note;
import com.example.booknote.entity.Tag;
import com.example.booknote.repository.BookRepository;
import com.example.booknote.repository.NoteRepository;
import com.example.booknote.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CardService {

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private TagRepository tagRepository;

    public List<NoteDTO> getCards(Long bookId, Long tagId, Integer pageNumber) {
        List<Note> notes = noteRepository.findCardsWithFilters(bookId, pageNumber, tagId);
        return notes.stream().map(NoteDTO::fromEntity).collect(Collectors.toList());
    }

    public Page<NoteDTO> getCardsPaged(Long bookId, Long tagId, Integer pageNumber, Pageable pageable) {
        List<Note> allNotes = noteRepository.findCardsWithFilters(bookId, pageNumber, tagId);
        int total = allNotes.size();
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), total);
        List<Note> pageContent = start < total ? allNotes.subList(start, end) : new ArrayList<>();
        List<NoteDTO> dtos = pageContent.stream().map(NoteDTO::fromEntity).collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, total);
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

    public List<Integer> getPageNumbersByBook(Long bookId) {
        if (bookId == null) {
            return new ArrayList<>();
        }
        return noteRepository.findDistinctPageNumbersByBookId(bookId);
    }

    public CardSummary getCardSummary() {
        long totalNotes = noteRepository.count();
        long totalBooks = bookRepository.count();
        long totalTags = tagRepository.count();
        return new CardSummary(totalNotes, totalBooks, totalTags);
    }

    public static class CardSummary {
        private final long totalNotes;
        private final long totalBooks;
        private final long totalTags;

        public CardSummary(long totalNotes, long totalBooks, long totalTags) {
            this.totalNotes = totalNotes;
            this.totalBooks = totalBooks;
            this.totalTags = totalTags;
        }

        public long getTotalNotes() { return totalNotes; }
        public long getTotalBooks() { return totalBooks; }
        public long getTotalTags() { return totalTags; }
    }
}

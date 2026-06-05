
package com.example.booknote.service;

import com.example.booknote.entity.Book;
import com.example.booknote.repository.BookRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BookService {
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private SearchService searchService;
    
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }
    
    public List<Book> getBooksByStatus(Book.ReadingStatus status) {
        return bookRepository.findByStatus(status);
    }
    
    public List<Book> filterBooks(Book.ReadingStatus status, String category, 
                                   Integer minProgress, Integer maxProgress, 
                                   Boolean hasNotes) {
        Specification<Book> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            
            if (category != null && !category.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("category"), category));
            }
            
            if (minProgress != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("progress"), minProgress));
            }
            
            if (maxProgress != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("progress"), maxProgress));
            }
            
            if (hasNotes != null) {
                if (hasNotes) {
                    predicates.add(criteriaBuilder.isNotEmpty(root.get("notes")));
                } else {
                    predicates.add(criteriaBuilder.isEmpty(root.get("notes")));
                }
            }
            
            query.distinct(true);
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        
        return bookRepository.findAll(spec);
    }
    
    public List<String> getAllCategories() {
        return bookRepository.findAll().stream()
                .map(Book::getCategory)
                .filter(cat -> cat != null && !cat.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }
    
    public long getNoteCountForBook(Long bookId) {
        return bookRepository.countNotesByBookId(bookId);
    }
    
    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }
    
    public Book createBook(Book book) {
        Book saved = bookRepository.save(book);
        try {
            searchService.rebuildIndex();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return saved;
    }
    
    public Book updateBook(Long id, Book bookDetails) {
        Book book = bookRepository.findById(id).orElseThrow(() -> new RuntimeException("Book not found"));
        book.setTitle(bookDetails.getTitle());
        book.setAuthor(bookDetails.getAuthor());
        book.setCoverUrl(bookDetails.getCoverUrl());
        book.setCategory(bookDetails.getCategory());
        book.setStatus(bookDetails.getStatus());
        book.setProgress(bookDetails.getProgress());
        book.setDescription(bookDetails.getDescription());
        Book updated = bookRepository.save(book);
        try {
            searchService.rebuildIndex();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return updated;
    }
    
    public void deleteBook(Long id) {
        bookRepository.deleteById(id);
        try {
            searchService.rebuildIndex();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public List<Book> searchBooksByTitle(String title) {
        return bookRepository.findByTitleContaining(title);
    }
    
    public List<Book> searchBooksByAuthor(String author) {
        return bookRepository.findByAuthorContaining(author);
    }
}

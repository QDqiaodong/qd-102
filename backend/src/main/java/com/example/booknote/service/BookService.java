
package com.example.booknote.service;

import com.example.booknote.entity.Book;
import com.example.booknote.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

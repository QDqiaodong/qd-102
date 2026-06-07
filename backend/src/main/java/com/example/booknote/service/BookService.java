
package com.example.booknote.service;

import com.example.booknote.entity.Book;
import com.example.booknote.entity.Note;
import com.example.booknote.entity.Tag;
import com.example.booknote.repository.BookRepository;
import com.example.booknote.repository.NoteRepository;
import com.example.booknote.repository.TagRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookService {
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private NoteRepository noteRepository;
    
    @Autowired
    private TagRepository tagRepository;
    
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

    public List<List<Book>> findDuplicateBooks() {
        List<Book> allBooks = bookRepository.findAll();
        
        Map<String, List<Book>> groups = new HashMap<>();
        for (Book book : allBooks) {
            String key = generateDuplicateKey(book.getTitle(), book.getAuthor());
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(book);
        }
        
        return groups.values().stream()
                .filter(list -> list.size() > 1)
                .peek(list -> list.sort(Comparator.comparing(Book::getCreatedAt)))
                .collect(Collectors.toList());
    }

    private String generateDuplicateKey(String title, String author) {
        String t = title != null ? title.trim().toLowerCase() : "";
        String a = author != null ? author.trim().toLowerCase() : "";
        return t + "||" + a;
    }

    @Transactional
    public Book mergeBooks(Long targetBookId, List<Long> sourceBookIds) {
        Book targetBook = bookRepository.findById(targetBookId)
                .orElseThrow(() -> new RuntimeException("目标书籍不存在"));

        int maxProgress = targetBook.getProgress() != null ? targetBook.getProgress() : 0;
        List<Note> allNotes = new ArrayList<>();
        List<Tag> allTags = new ArrayList<>();
        
        if (targetBook.getNotes() != null) {
            allNotes.addAll(targetBook.getNotes());
        }

        for (Long sourceId : sourceBookIds) {
            if (sourceId.equals(targetBookId)) continue;
            
            Book sourceBook = bookRepository.findById(sourceId)
                    .orElseThrow(() -> new RuntimeException("源书籍不存在: " + sourceId));

            if (sourceBook.getProgress() != null && sourceBook.getProgress() > maxProgress) {
                maxProgress = sourceBook.getProgress();
            }

            if (sourceBook.getNotes() != null) {
                for (Note note : sourceBook.getNotes()) {
                    note.setBook(targetBook);
                    allNotes.add(note);
                    if (note.getTags() != null) {
                        for (Tag tag : note.getTags()) {
                            if (!allTags.contains(tag)) {
                                allTags.add(tag);
                            }
                        }
                    }
                }
            }

            if (targetBook.getDescription() == null || targetBook.getDescription().isEmpty()) {
                if (sourceBook.getDescription() != null && !sourceBook.getDescription().isEmpty()) {
                    targetBook.setDescription(sourceBook.getDescription());
                }
            }

            if (targetBook.getCoverUrl() == null || targetBook.getCoverUrl().isEmpty()) {
                if (sourceBook.getCoverUrl() != null && !sourceBook.getCoverUrl().isEmpty()) {
                    targetBook.setCoverUrl(sourceBook.getCoverUrl());
                }
            }

            if (targetBook.getCategory() == null || targetBook.getCategory().isEmpty()) {
                if (sourceBook.getCategory() != null && !sourceBook.getCategory().isEmpty()) {
                    targetBook.setCategory(sourceBook.getCategory());
                }
            }

            bookRepository.delete(sourceBook);
        }

        targetBook.setProgress(maxProgress);
        targetBook.setNotes(allNotes);
        
        Book saved = bookRepository.save(targetBook);
        
        try {
            searchService.rebuildIndex();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return saved;
    }

    public MergePreview getMergePreview(List<Long> bookIds) {
        MergePreview preview = new MergePreview();
        List<Book> books = new ArrayList<>();
        
        for (Long id : bookIds) {
            bookRepository.findById(id).ifPresent(books::add);
        }
        
        if (books.isEmpty()) {
            return preview;
        }

        books.sort(Comparator.comparing(Book::getCreatedAt));
        Book primary = books.get(0);
        
        preview.setPrimaryBookId(primary.getId());
        preview.setBookCount(books.size());
        
        int totalNotes = 0;
        int maxProgress = 0;
        for (Book book : books) {
            totalNotes += book.getNotes() != null ? book.getNotes().size() : 0;
            if (book.getProgress() != null && book.getProgress() > maxProgress) {
                maxProgress = book.getProgress();
            }
        }
        
        preview.setTotalNotes(totalNotes);
        preview.setMergedProgress(maxProgress);
        preview.setBooks(books);
        
        return preview;
    }

    public static class MergePreview {
        private Long primaryBookId;
        private int bookCount;
        private int totalNotes;
        private int mergedProgress;
        private List<Book> books = new ArrayList<>();

        public Long getPrimaryBookId() { return primaryBookId; }
        public void setPrimaryBookId(Long primaryBookId) { this.primaryBookId = primaryBookId; }
        public int getBookCount() { return bookCount; }
        public void setBookCount(int bookCount) { this.bookCount = bookCount; }
        public int getTotalNotes() { return totalNotes; }
        public void setTotalNotes(int totalNotes) { this.totalNotes = totalNotes; }
        public int getMergedProgress() { return mergedProgress; }
        public void setMergedProgress(int mergedProgress) { this.mergedProgress = mergedProgress; }
        public List<Book> getBooks() { return books; }
        public void setBooks(List<Book> books) { this.books = books; }
    }
}

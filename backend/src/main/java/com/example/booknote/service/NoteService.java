
package com.example.booknote.service;

import com.example.booknote.entity.Book;
import com.example.booknote.entity.Note;
import com.example.booknote.entity.Tag;
import com.example.booknote.repository.BookRepository;
import com.example.booknote.repository.NoteRepository;
import com.example.booknote.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class NoteService {
    
    @Autowired
    private NoteRepository noteRepository;
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private TagRepository tagRepository;
    
    @Autowired
    private SearchService searchService;
    
    public Page<Note> getNotesByBookId(Long bookId, Pageable pageable) {
        return noteRepository.findByBookId(bookId, pageable);
    }
    
    public Long getNotesCountByBookId(Long bookId) {
        return noteRepository.countByBookId(bookId);
    }
    
    public List<Note> getAllNotesByBookId(Long bookId) {
        return noteRepository.findByBookId(bookId);
    }
    
    public Optional<Note> getNoteById(Long id) {
        return noteRepository.findById(id);
    }
    
    public Note createNote(Long bookId, Note note) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new RuntimeException("Book not found"));
        note.setBook(book);
        
        List<Tag> tags = new ArrayList<>();
        for (Tag tag : note.getTags()) {
            Tag existingTag = tagRepository.findByName(tag.getName())
                    .orElseGet(() -> tagRepository.save(tag));
            tags.add(existingTag);
        }
        note.setTags(tags);
        
        Note saved = noteRepository.save(note);
        try {
            searchService.rebuildIndex();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return saved;
    }
    
    public Note updateNote(Long id, Note noteDetails) {
        Note note = noteRepository.findById(id).orElseThrow(() -> new RuntimeException("Note not found"));
        note.setTitle(noteDetails.getTitle());
        note.setContent(noteDetails.getContent());
        note.setPageNumber(noteDetails.getPageNumber());
        
        List<Tag> tags = new ArrayList<>();
        for (Tag tag : noteDetails.getTags()) {
            Tag existingTag = tagRepository.findByName(tag.getName())
                    .orElseGet(() -> tagRepository.save(tag));
            tags.add(existingTag);
        }
        note.setTags(tags);
        
        Note updated = noteRepository.save(note);
        try {
            searchService.rebuildIndex();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return updated;
    }
    
    public void deleteNote(Long id) {
        noteRepository.deleteById(id);
        try {
            searchService.rebuildIndex();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public Page<Note> searchNotesByContent(String content, Pageable pageable) {
        return noteRepository.findByContentContaining(content, pageable);
    }
    
    // 草稿缓存 (内存存储，适用个人轻量化场景)
    private final ConcurrentHashMap<Long, DraftDTO> draftCache = new ConcurrentHashMap<>();
    private final AtomicLong draftIdGenerator = new AtomicLong(1);
    
    public DraftResponse saveDraft(Long bookId, DraftDTO draft) {
        Long draftId = draft.getDraftId();
        if (draftId == null) {
            draftId = draftIdGenerator.getAndIncrement();
        }
        draft.setDraftId(draftId);
        draft.setBookId(bookId);
        draft.setSavedAt(LocalDateTime.now());
        draftCache.put(bookId, draft);
        return new DraftResponse(draftId, draft.getSavedAt());
    }
    
    public DraftDTO getDraft(Long bookId) {
        return draftCache.get(bookId);
    }
    
    public void deleteDraft(Long bookId) {
        draftCache.remove(bookId);
    }
    
    // 草稿DTO
    public static class DraftDTO {
        private Long draftId;
        private Long bookId;
        private String title;
        private String content;
        private Integer pageNumber;
        private List<TagDTO> tags;
        private LocalDateTime savedAt;
        
        public Long getDraftId() { return draftId; }
        public void setDraftId(Long draftId) { this.draftId = draftId; }
        public Long getBookId() { return bookId; }
        public void setBookId(Long bookId) { this.bookId = bookId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Integer getPageNumber() { return pageNumber; }
        public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }
        public List<TagDTO> getTags() { return tags; }
        public void setTags(List<TagDTO> tags) { this.tags = tags; }
        public LocalDateTime getSavedAt() { return savedAt; }
        public void setSavedAt(LocalDateTime savedAt) { this.savedAt = savedAt; }
    }
    
    public static class TagDTO {
        private Long id;
        private String name;
        private String color;
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
    }
    
    public static class DraftResponse {
        private final Long draftId;
        private final LocalDateTime savedAt;
        
        public DraftResponse(Long draftId, LocalDateTime savedAt) {
            this.draftId = draftId;
            this.savedAt = savedAt;
        }
        
        public Long getDraftId() { return draftId; }
        public LocalDateTime getSavedAt() { return savedAt; }
    }
}

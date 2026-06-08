
package com.example.booknote.dto;

import com.example.booknote.entity.Book;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookDTO {
    private Long id;
    private String title;
    private String author;
    private String coverUrl;
    private String category;
    private String status;
    private Integer progress;
    private String description;
    private String highlightedTitle;
    private String highlightedContent;
    private Long noteCount;
    
    public static BookDTO fromEntity(Book book) {
        return fromEntity(book, null);
    }
    
    public static BookDTO fromEntity(Book book, Long noteCount) {
        BookDTO dto = new BookDTO();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setAuthor(book.getAuthor());
        dto.setCoverUrl(book.getCoverUrl());
        dto.setCategory(book.getCategory());
        dto.setStatus(book.getStatus() != null ? book.getStatus().name() : null);
        dto.setProgress(book.getProgress());
        dto.setDescription(book.getDescription());
        if (noteCount != null) {
            dto.setNoteCount(noteCount);
        } else {
            dto.setNoteCount(book.getNotes() != null ? (long) book.getNotes().size() : 0L);
        }
        return dto;
    }
    
    public Book toEntity() {
        Book book = new Book();
        book.setId(this.id);
        book.setTitle(this.title);
        book.setAuthor(this.author);
        book.setCoverUrl(this.coverUrl);
        book.setCategory(this.category);
        book.setStatus(this.status != null ? Book.ReadingStatus.valueOf(this.status) : null);
        book.setProgress(this.progress);
        book.setDescription(this.description);
        return book;
    }
}


package com.example.booknote.dto;

import com.example.booknote.entity.Note;
import com.example.booknote.entity.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteDTO {
    private Long id;
    private String title;
    private String content;
    private Integer pageNumber;
    private Long bookId;
    private String bookTitle;
    private List<TagDTO> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String highlightedTitle;
    private String highlightedContent;
    private String highlightedTags;
    
    public static NoteDTO fromEntity(Note note) {
        NoteDTO dto = new NoteDTO();
        dto.setId(note.getId());
        dto.setTitle(note.getTitle());
        dto.setContent(note.getContent());
        dto.setPageNumber(note.getPageNumber());
        dto.setBookId(note.getBook() != null ? note.getBook().getId() : null);
        dto.setBookTitle(note.getBook() != null ? note.getBook().getTitle() : null);
        dto.setTags(note.getTags().stream().map(TagDTO::fromEntity).collect(Collectors.toList()));
        dto.setCreatedAt(note.getCreatedAt());
        dto.setUpdatedAt(note.getUpdatedAt());
        return dto;
    }
    
    public Note toEntity() {
        Note note = new Note();
        note.setId(this.id);
        note.setTitle(this.title);
        note.setContent(this.content);
        note.setPageNumber(this.pageNumber);
        note.setTags(this.tags != null ? this.tags.stream().map(TagDTO::toEntity).collect(Collectors.toList()) : List.of());
        return note;
    }
}

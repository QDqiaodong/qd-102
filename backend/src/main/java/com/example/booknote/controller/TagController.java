
package com.example.booknote.controller;

import com.example.booknote.dto.TagDTO;
import com.example.booknote.entity.Tag;
import com.example.booknote.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tags")
public class TagController {
    
    @Autowired
    private TagService tagService;
    
    @GetMapping
    public ResponseEntity<List<TagDTO>> getAllTags() {
        List<Tag> tags = tagService.getAllTags();
        List<TagDTO> dtos = tags.stream().map(TagDTO::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<TagDTO> getTagById(@PathVariable Long id) {
        return tagService.getTagById(id)
                .map(tag -> ResponseEntity.ok(TagDTO.fromEntity(tag)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<TagDTO> createTag(@RequestBody TagDTO tagDTO) {
        Tag tag = tagDTO.toEntity();
        Tag saved = tagService.createTag(tag);
        return ResponseEntity.status(HttpStatus.CREATED).body(TagDTO.fromEntity(saved));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<TagDTO> updateTag(@PathVariable Long id, @RequestBody TagDTO tagDTO) {
        Tag tag = tagDTO.toEntity();
        Tag updated = tagService.updateTag(id, tag);
        return ResponseEntity.ok(TagDTO.fromEntity(updated));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return ResponseEntity.noContent().build();
    }
}

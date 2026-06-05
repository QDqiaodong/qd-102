
package com.example.booknote.service;

import com.example.booknote.entity.Tag;
import com.example.booknote.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TagService {
    
    @Autowired
    private TagRepository tagRepository;
    
    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }
    
    public Optional<Tag> getTagById(Long id) {
        return tagRepository.findById(id);
    }
    
    public Optional<Tag> getTagByName(String name) {
        return tagRepository.findByName(name);
    }
    
    public Tag createTag(Tag tag) {
        if (tagRepository.existsByName(tag.getName())) {
            throw new RuntimeException("Tag already exists");
        }
        return tagRepository.save(tag);
    }
    
    public Tag updateTag(Long id, Tag tagDetails) {
        Tag tag = tagRepository.findById(id).orElseThrow(() -> new RuntimeException("Tag not found"));
        if (!tag.getName().equals(tagDetails.getName()) && tagRepository.existsByName(tagDetails.getName())) {
            throw new RuntimeException("Tag name already exists");
        }
        tag.setName(tagDetails.getName());
        tag.setColor(tagDetails.getColor());
        return tagRepository.save(tag);
    }
    
    public void deleteTag(Long id) {
        tagRepository.deleteById(id);
    }
}

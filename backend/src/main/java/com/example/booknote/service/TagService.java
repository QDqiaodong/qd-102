
package com.example.booknote.service;

import com.example.booknote.dto.TagGraphDTO;
import com.example.booknote.entity.Note;
import com.example.booknote.entity.Tag;
import com.example.booknote.repository.NoteRepository;
import com.example.booknote.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TagService {
    
    @Autowired
    private TagRepository tagRepository;
    
    @Autowired
    private NoteRepository noteRepository;
    
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
    
    public TagGraphDTO getTagGraph() {
        List<Note> notes = noteRepository.findAll();
        Map<String, Integer> tagPairCounts = new HashMap<>();
        Map<Long, Integer> tagNoteCounts = new HashMap<>();
        Set<Long> allTagIds = new HashSet<>();
        
        for (Note note : notes) {
            List<Tag> noteTags = note.getTags();
            if (noteTags == null || noteTags.isEmpty()) {
                continue;
            }
            
            for (Tag tag : noteTags) {
                allTagIds.add(tag.getId());
                tagNoteCounts.merge(tag.getId(), 1, Integer::sum);
            }
            
            if (noteTags.size() >= 2) {
                for (int i = 0; i < noteTags.size(); i++) {
                    for (int j = i + 1; j < noteTags.size(); j++) {
                        Tag tag1 = noteTags.get(i);
                        Tag tag2 = noteTags.get(j);
                        String pairKey = createPairKey(tag1.getId(), tag2.getId());
                        tagPairCounts.merge(pairKey, 1, Integer::sum);
                    }
                }
            }
        }
        
        List<Tag> allTags = tagRepository.findAllById(allTagIds);
        Map<Long, Tag> tagMap = allTags.stream().collect(Collectors.toMap(Tag::getId, t -> t));
        
        TagGraphDTO graphDTO = new TagGraphDTO();
        List<TagGraphDTO.TagNode> nodes = new ArrayList<>();
        
        for (Tag tag : allTags) {
            TagGraphDTO.TagNode node = new TagGraphDTO.TagNode();
            node.setId(tag.getId());
            node.setName(tag.getName());
            node.setColor(tag.getColor());
            node.setNoteCount(tagNoteCounts.getOrDefault(tag.getId(), 0));
            node.setExpanded(false);
            node.setX(0);
            node.setY(0);
            nodes.add(node);
        }
        graphDTO.setNodes(nodes);
        
        List<TagGraphDTO.TagEdge> edges = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : tagPairCounts.entrySet()) {
            String[] ids = entry.getKey().split("_");
            Long sourceId = Long.parseLong(ids[0]);
            Long targetId = Long.parseLong(ids[1]);
            
            if (tagMap.containsKey(sourceId) && tagMap.containsKey(targetId)) {
                TagGraphDTO.TagEdge edge = new TagGraphDTO.TagEdge();
                edge.setId(entry.getKey());
                edge.setSource(sourceId);
                edge.setTarget(targetId);
                edge.setWeight(entry.getValue());
                edges.add(edge);
            }
        }
        graphDTO.setEdges(edges);
        
        return graphDTO;
    }
    
    public TagGraphDTO getTagNeighbors(Long tagId) {
        Optional<Tag> tagOpt = tagRepository.findById(tagId);
        if (tagOpt.isEmpty()) {
            return new TagGraphDTO();
        }
        
        Tag centerTag = tagOpt.get();
        Set<Tag> neighborTags = new HashSet<>();
        Map<String, Integer> edgeWeights = new HashMap<>();
        Map<Long, Integer> tagNoteCounts = new HashMap<>();
        
        tagNoteCounts.put(centerTag.getId(), centerTag.getNotes() != null ? centerTag.getNotes().size() : 0);
        
        for (Note note : centerTag.getNotes()) {
            for (Tag tag : note.getTags()) {
                if (!tag.getId().equals(tagId)) {
                    neighborTags.add(tag);
                    tagNoteCounts.merge(tag.getId(), 1, Integer::sum);
                    
                    String pairKey = createPairKey(tagId, tag.getId());
                    edgeWeights.merge(pairKey, 1, Integer::sum);
                }
            }
        }
        
        TagGraphDTO graphDTO = new TagGraphDTO();
        List<TagGraphDTO.TagNode> nodes = new ArrayList<>();
        
        TagGraphDTO.TagNode centerNode = new TagGraphDTO.TagNode();
        centerNode.setId(centerTag.getId());
        centerNode.setName(centerTag.getName());
        centerNode.setColor(centerTag.getColor());
        centerNode.setNoteCount(tagNoteCounts.getOrDefault(centerTag.getId(), 0));
        centerNode.setExpanded(true);
        centerNode.setX(0);
        centerNode.setY(0);
        nodes.add(centerNode);
        
        int index = 0;
        int totalNeighbors = neighborTags.size();
        for (Tag neighbor : neighborTags) {
            double angle = 2 * Math.PI * index / totalNeighbors;
            double radius = 150;
            
            TagGraphDTO.TagNode node = new TagGraphDTO.TagNode();
            node.setId(neighbor.getId());
            node.setName(neighbor.getName());
            node.setColor(neighbor.getColor());
            node.setNoteCount(tagNoteCounts.getOrDefault(neighbor.getId(), 0));
            node.setExpanded(false);
            node.setX(radius * Math.cos(angle));
            node.setY(radius * Math.sin(angle));
            nodes.add(node);
            index++;
        }
        graphDTO.setNodes(nodes);
        
        List<TagGraphDTO.TagEdge> edges = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : edgeWeights.entrySet()) {
            String[] ids = entry.getKey().split("_");
            TagGraphDTO.TagEdge edge = new TagGraphDTO.TagEdge();
            edge.setId(entry.getKey());
            edge.setSource(Long.parseLong(ids[0]));
            edge.setTarget(Long.parseLong(ids[1]));
            edge.setWeight(entry.getValue());
            edges.add(edge);
        }
        graphDTO.setEdges(edges);
        
        return graphDTO;
    }
    
    public Tag mergeTags(Long targetTagId, List<Long> sourceTagIds) {
        Tag targetTag = tagRepository.findById(targetTagId)
                .orElseThrow(() -> new RuntimeException("目标标签不存在"));
        
        if (sourceTagIds == null || sourceTagIds.isEmpty()) {
            throw new RuntimeException("请选择要合并的源标签");
        }
        
        if (sourceTagIds.contains(targetTagId)) {
            throw new RuntimeException("目标标签不能同时作为源标签");
        }
        
        Set<Long> sourceIdSet = new HashSet<>(sourceTagIds);
        List<Tag> sourceTags = tagRepository.findAllById(sourceIdSet);
        
        if (sourceTags.size() != sourceIdSet.size()) {
            throw new RuntimeException("部分源标签不存在");
        }
        
        for (Tag sourceTag : sourceTags) {
            List<Note> notes = sourceTag.getNotes();
            if (notes != null && !notes.isEmpty()) {
                for (Note note : notes) {
                    boolean alreadyHasTarget = note.getTags().stream()
                            .anyMatch(t -> t.getId().equals(targetTagId));
                    if (!alreadyHasTarget) {
                        note.getTags().add(targetTag);
                        noteRepository.save(note);
                    }
                }
            }
            tagRepository.delete(sourceTag);
        }
        
        return tagRepository.findById(targetTagId).orElse(targetTag);
    }
    
    private String createPairKey(Long id1, Long id2) {
        if (id1 < id2) {
            return id1 + "_" + id2;
        }
        return id2 + "_" + id1;
    }
}

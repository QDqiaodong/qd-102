
package com.example.booknote.dto;

import com.example.booknote.entity.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagDTO {
    private Long id;
    private String name;
    private String color;
    
    public static TagDTO fromEntity(Tag tag) {
        TagDTO dto = new TagDTO();
        dto.setId(tag.getId());
        dto.setName(tag.getName());
        dto.setColor(tag.getColor());
        return dto;
    }
    
    public Tag toEntity() {
        Tag tag = new Tag();
        tag.setId(this.id);
        tag.setName(this.name);
        tag.setColor(this.color);
        return tag;
    }
}

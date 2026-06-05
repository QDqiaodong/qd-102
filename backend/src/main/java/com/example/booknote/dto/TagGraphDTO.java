
package com.example.booknote.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagGraphDTO {
    
    private List<TagNode> nodes = new ArrayList<>();
    private List<TagEdge> edges = new ArrayList<>();
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TagNode {
        private Long id;
        private String name;
        private String color;
        private int noteCount;
        private boolean expanded;
        private double x;
        private double y;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TagEdge {
        private String id;
        private Long source;
        private Long target;
        private int weight;
    }
}

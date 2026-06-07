
package com.example.booknote.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagMergeRequestDTO {
    private Long targetTagId;
    private List<Long> sourceTagIds;
}

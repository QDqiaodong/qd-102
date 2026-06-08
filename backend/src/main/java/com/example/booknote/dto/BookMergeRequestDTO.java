
package com.example.booknote.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookMergeRequestDTO {
    private Long targetBookId;
    private List<Long> sourceBookIds;
}


package com.example.booknote.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "repeated_notes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepeatedNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false, unique = true)
    private Note note;

    @Column(name = "next_review_time", nullable = false)
    private LocalDateTime nextReviewTime;

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount;

    @Column(name = "last_review_time")
    private LocalDateTime lastReviewTime;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (reviewCount == null) {
            reviewCount = 0;
        }
        if (nextReviewTime == null) {
            nextReviewTime = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

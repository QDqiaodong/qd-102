
package com.example.booknote.repository;

import com.example.booknote.entity.RepeatedNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RepeatedNoteRepository extends JpaRepository<RepeatedNote, Long> {

    Optional<RepeatedNote> findByNoteId(Long noteId);

    boolean existsByNoteId(Long noteId);

    @Query("SELECT rn FROM RepeatedNote rn JOIN FETCH rn.note n JOIN FETCH n.book ORDER BY rn.nextReviewTime ASC")
    List<RepeatedNote> findAllWithNoteAndBookOrderByNextReviewTime();

    @Query("SELECT rn FROM RepeatedNote rn JOIN FETCH rn.note n JOIN FETCH n.book " +
           "WHERE rn.nextReviewTime <= :now ORDER BY rn.nextReviewTime ASC")
    List<RepeatedNote> findDueNotesWithNoteAndBook(@Param("now") LocalDateTime now);

    @Query("SELECT rn FROM RepeatedNote rn JOIN FETCH rn.note n JOIN FETCH n.book " +
           "WHERE rn.nextReviewTime > :now ORDER BY rn.nextReviewTime ASC")
    List<RepeatedNote> findUpcomingNotesWithNoteAndBook(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(rn) FROM RepeatedNote rn WHERE rn.nextReviewTime <= :now")
    long countDueNotes(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(rn) FROM RepeatedNote rn")
    long countAllRepeatedNotes();

    void deleteByNoteId(Long noteId);
}

package com.example.booknote.repository;

import com.example.booknote.entity.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
    Page<Note> findByBookId(Long bookId, Pageable pageable);

    List<Note> findByBookId(Long bookId);

    Long countByBookId(Long bookId);

    List<Note> findByContentContaining(String content);

    Page<Note> findByContentContaining(String content, Pageable pageable);

    @Query("SELECT n FROM Note n WHERE n.createdAt >= :start AND n.createdAt < :end ORDER BY n.createdAt DESC")
    List<Note> findByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT n FROM Note n JOIN FETCH n.book WHERE n.createdAt >= :start AND n.createdAt < :end ORDER BY n.createdAt DESC")
    List<Note> findByCreatedAtBetweenWithBook(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}

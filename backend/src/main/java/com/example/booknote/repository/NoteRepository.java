package com.example.booknote.repository;

import com.example.booknote.entity.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long>, JpaSpecificationExecutor<Note> {
    Page<Note> findByBookId(Long bookId, Pageable pageable);

    List<Note> findByBookId(Long bookId);

    Long countByBookId(Long bookId);

    List<Note> findByContentContaining(String content);

    Page<Note> findByContentContaining(String content, Pageable pageable);

    @Query("SELECT n FROM Note n WHERE n.createdAt >= :start AND n.createdAt < :end ORDER BY n.createdAt DESC")
    List<Note> findByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT n FROM Note n JOIN FETCH n.book WHERE n.createdAt >= :start AND n.createdAt < :end ORDER BY n.createdAt DESC")
    List<Note> findByCreatedAtBetweenWithBook(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT DISTINCT n FROM Note n JOIN FETCH n.book LEFT JOIN FETCH n.tags WHERE " +
           "(:bookId IS NULL OR n.book.id = :bookId) AND " +
           "(:pageNumber IS NULL OR n.pageNumber = :pageNumber) AND " +
           "(:tagId IS NULL OR :tagId IN (SELECT t.id FROM n.tags t)) " +
           "ORDER BY n.createdAt DESC")
    List<Note> findCardsWithFilters(
            @Param("bookId") Long bookId,
            @Param("pageNumber") Integer pageNumber,
            @Param("tagId") Long tagId);

    @Query("SELECT DISTINCT n FROM Note n JOIN FETCH n.book LEFT JOIN FETCH n.tags " +
           "WHERE (:bookId IS NULL OR n.book.id = :bookId) " +
           "AND (:tagId IS NULL OR :tagId IN (SELECT t.id FROM n.tags t)) " +
           "AND (:pageFrom IS NULL OR n.pageNumber >= :pageFrom) " +
           "AND (:pageTo IS NULL OR n.pageNumber <= :pageTo) " +
           "ORDER BY n.createdAt DESC")
    List<Note> findCardsWithPageRange(
            @Param("bookId") Long bookId,
            @Param("tagId") Long tagId,
            @Param("pageFrom") Integer pageFrom,
            @Param("pageTo") Integer pageTo);

    @Query("SELECT DISTINCT n.pageNumber FROM Note n WHERE n.book.id = :bookId AND n.pageNumber IS NOT NULL ORDER BY n.pageNumber")
    List<Integer> findDistinctPageNumbersByBookId(@Param("bookId") Long bookId);
}

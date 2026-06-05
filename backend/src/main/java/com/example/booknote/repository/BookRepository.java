package com.example.booknote.repository;

import com.example.booknote.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByStatus(Book.ReadingStatus status);

    List<Book> findByTitleContaining(String title);

    List<Book> findByAuthorContaining(String author);

    List<Book> findByCategory(String category);

    @Query("SELECT b FROM Book b WHERE b.updatedAt >= :start AND b.updatedAt < :end")
    List<Book> findByUpdatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT b FROM Book b WHERE b.status = 'READ' AND b.updatedAt >= :start AND b.updatedAt < :end")
    List<Book> findCompletedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}

package com.example.booknote.repository;

import com.example.booknote.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {
    List<Book> findByStatus(Book.ReadingStatus status);

    List<Book> findByTitleContaining(String title);

    List<Book> findByAuthorContaining(String author);

    List<Book> findByCategory(String category);

    @Query("SELECT b FROM Book b WHERE b.updatedAt >= :start AND b.updatedAt < :end")
    List<Book> findByUpdatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT b FROM Book b WHERE b.status = 'READ' AND b.updatedAt >= :start AND b.updatedAt < :end")
    List<Book> findCompletedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(n) FROM Note n WHERE n.book.id = :bookId")
    long countNotesByBookId(@Param("bookId") Long bookId);

    @Query("SELECT b FROM Book b WHERE b.status = 'READING' AND b.id NOT IN " +
           "(SELECT n.book.id FROM Note n WHERE n.createdAt >= :date)")
    List<Book> findReadingBooksWithNoNotesSince(@Param("date") LocalDateTime date);

    @Query("SELECT b FROM Book b WHERE b.status = 'WANT_TO_READ' " +
           "AND b.createdAt < :date AND (b.progress IS NULL OR b.progress = 0)")
    List<Book> findWantToReadBooksOlderThan(@Param("date") LocalDateTime date);

    @Query("SELECT b FROM Book b WHERE b.status = 'READ' " +
           "AND b.id NOT IN (SELECT n.book.id FROM Note n)")
    List<Book> findReadBooksWithNoNotes();

    @Query("SELECT MAX(n.createdAt) FROM Note n WHERE n.book.id = :bookId")
    LocalDateTime findLatestNoteDateByBookId(@Param("bookId") Long bookId);
}

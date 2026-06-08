
package com.example.booknote.repository;

import com.example.booknote.entity.NoteReadingProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NoteReadingProgressRepository extends JpaRepository<NoteReadingProgress, Long> {

    Optional<NoteReadingProgress> findByBookId(Long bookId);

    void deleteByBookId(Long bookId);
}

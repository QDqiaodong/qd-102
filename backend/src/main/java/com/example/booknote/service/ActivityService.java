package com.example.booknote.service;

import com.example.booknote.dto.BookDTO;
import com.example.booknote.dto.NoteDTO;
import com.example.booknote.entity.Book;
import com.example.booknote.entity.Note;
import com.example.booknote.repository.BookRepository;
import com.example.booknote.repository.NoteRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ActivityService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayActivity {
        private String date;
        private int noteCount;
        private int progressCount;
        private int completedCount;
        private int totalActivity;
        private int level;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeatmapData {
        private List<DayActivity> days;
        private int maxActivity;
        private Map<String, Object> summary;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateDetail {
        private String date;
        private List<BookDTO> books;
        private List<NoteDTO> notes;
        private int noteCount;
        private int progressCount;
        private int completedCount;
    }

    public HeatmapData getHeatmapData(int months) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(months);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<Note> notes = noteRepository.findByCreatedAtBetween(startDateTime, endDateTime);
        List<Book> updatedBooks = bookRepository.findByUpdatedAtBetween(startDateTime, endDateTime);
        List<Book> completedBooks = bookRepository.findCompletedBetween(startDateTime, endDateTime);

        Map<LocalDate, List<Note>> notesByDate = notes.stream()
                .collect(Collectors.groupingBy(n -> n.getCreatedAt().toLocalDate()));

        Map<LocalDate, List<Book>> updatedBooksByDate = updatedBooks.stream()
                .collect(Collectors.groupingBy(b -> b.getUpdatedAt().toLocalDate()));

        Set<LocalDate> completedDates = completedBooks.stream()
                .map(b -> b.getUpdatedAt().toLocalDate())
                .collect(Collectors.toSet());

        List<DayActivity> days = new ArrayList<>();
        int maxActivity = 0;
        int totalNotes = 0;
        int totalProgress = 0;
        int totalCompleted = 0;
        int activeDays = 0;

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            int noteCount = notesByDate.getOrDefault(date, Collections.emptyList()).size();
            List<Book> dayBooks = updatedBooksByDate.getOrDefault(date, Collections.emptyList());
            int progressCount = (int) dayBooks.stream()
                    .filter(b -> b.getStatus() != Book.ReadingStatus.READ || !completedDates.contains(date))
                    .count();
            int completedCount = completedDates.contains(date)
                    ? (int) completedBooks.stream().filter(b -> b.getUpdatedAt().toLocalDate().equals(date)).count()
                    : 0;

            int total = noteCount + progressCount + completedCount;
            if (total > maxActivity) {
                maxActivity = total;
            }

            totalNotes += noteCount;
            totalProgress += progressCount;
            totalCompleted += completedCount;
            if (total > 0) activeDays++;

            DayActivity day = new DayActivity();
            day.setDate(date.toString());
            day.setNoteCount(noteCount);
            day.setProgressCount(progressCount);
            day.setCompletedCount(completedCount);
            day.setTotalActivity(total);
            day.setLevel(0);
            days.add(day);
        }

        for (DayActivity day : days) {
            day.setLevel(calculateLevel(day.getTotalActivity(), maxActivity));
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalNotes", totalNotes);
        summary.put("totalProgress", totalProgress);
        summary.put("totalCompleted", totalCompleted);
        summary.put("activeDays", activeDays);
        summary.put("totalDays", days.size());

        return new HeatmapData(days, maxActivity, summary);
    }

    public DateDetail getDateDetail(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<Note> notes = noteRepository.findByCreatedAtBetweenWithBook(start, end);
        List<Book> updatedBooks = bookRepository.findByUpdatedAtBetween(start, end);
        List<Book> completedBooks = bookRepository.findCompletedBetween(start, end);

        Set<Long> completedBookIds = completedBooks.stream()
                .map(Book::getId)
                .collect(Collectors.toSet());

        List<BookDTO> bookDTOs = updatedBooks.stream()
                .map(BookDTO::fromEntity)
                .collect(Collectors.toList());

        List<NoteDTO> noteDTOs = notes.stream()
                .map(NoteDTO::fromEntity)
                .collect(Collectors.toList());

        DateDetail detail = new DateDetail();
        detail.setDate(date.toString());
        detail.setBooks(bookDTOs);
        detail.setNotes(noteDTOs);
        detail.setNoteCount(notes.size());
        detail.setProgressCount((int) updatedBooks.stream().filter(b -> !completedBookIds.contains(b.getId())).count());
        detail.setCompletedCount(completedBooks.size());

        return detail;
    }

    private int calculateLevel(int activity, int maxActivity) {
        if (activity == 0) return 0;
        if (maxActivity == 0) return 0;
        double ratio = (double) activity / maxActivity;
        if (ratio <= 0.25) return 1;
        if (ratio <= 0.50) return 2;
        if (ratio <= 0.75) return 3;
        return 4;
    }
}

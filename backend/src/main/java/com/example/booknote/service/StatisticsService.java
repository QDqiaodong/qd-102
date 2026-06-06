
package com.example.booknote.service;

import com.example.booknote.dto.statistics.*;
import com.example.booknote.entity.Book;
import com.example.booknote.entity.Note;
import com.example.booknote.repository.BookRepository;
import com.example.booknote.repository.NoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private NoteRepository noteRepository;

    public ReadingOverviewDTO getReadingOverview(int months) {
        ReadingOverviewDTO overview = new ReadingOverviewDTO();
        overview.setStatusDistribution(getStatusDistribution());
        overview.setCategoryDistribution(getCategoryDistribution());
        overview.setMonthlyNoteOutput(getMonthlyNoteOutput(months));
        overview.setProgressRange(getProgressRange());
        overview.setTotalBooks(bookRepository.count());
        overview.setTotalNotes(noteRepository.count());
        return overview;
    }

    public List<StatusDistributionDTO> getStatusDistribution() {
        List<Book> books = bookRepository.findAll();
        long total = books.size();
        if (total == 0) {
            return Collections.emptyList();
        }

        Map<Book.ReadingStatus, Long> statusCount = books.stream()
                .filter(book -> book.getStatus() != null)
                .collect(Collectors.groupingBy(Book::getStatus, Collectors.counting()));

        return Arrays.stream(Book.ReadingStatus.values())
                .map(status -> {
                    long count = statusCount.getOrDefault(status, 0L);
                    double percentage = (double) count / total * 100;
                    return new StatusDistributionDTO(status.name(), count, percentage);
                })
                .collect(Collectors.toList());
    }

    public List<CategoryDistributionDTO> getCategoryDistribution() {
        List<Book> books = bookRepository.findAll();
        long total = books.size();
        if (total == 0) {
            return Collections.emptyList();
        }

        Map<String, Long> categoryCount = books.stream()
                .filter(book -> book.getCategory() != null && !book.getCategory().isEmpty())
                .collect(Collectors.groupingBy(Book::getCategory, Collectors.counting()));

        return categoryCount.entrySet().stream()
                .map(entry -> {
                    double percentage = (double) entry.getValue() / total * 100;
                    return new CategoryDistributionDTO(entry.getKey(), entry.getValue(), percentage);
                })
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .collect(Collectors.toList());
    }

    public List<MonthlyNoteOutputDTO> getMonthlyNoteOutput(int months) {
        YearMonth endMonth = YearMonth.now();
        YearMonth startMonth = endMonth.minusMonths(months - 1);

        LocalDateTime startDateTime = startMonth.atDay(1).atStartOfDay();
        LocalDateTime endDateTime = endMonth.atEndOfMonth().atTime(23, 59, 59, 999999999);

        List<Note> notes = noteRepository.findByCreatedAtBetween(startDateTime, endDateTime);

        Map<YearMonth, Long> monthlyCount = notes.stream()
                .collect(Collectors.groupingBy(
                        note -> YearMonth.from(note.getCreatedAt()),
                        Collectors.counting()
                ));

        List<MonthlyNoteOutputDTO> result = new ArrayList<>();
        YearMonth current = startMonth;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        while (!current.isAfter(endMonth)) {
            long count = monthlyCount.getOrDefault(current, 0L);
            result.add(new MonthlyNoteOutputDTO(current.format(formatter), count));
            current = current.plusMonths(1);
        }

        return result;
    }

    public List<ProgressRangeDTO> getProgressRange() {
        List<Book> books = bookRepository.findAll();
        long total = books.size();
        if (total == 0) {
            return Collections.emptyList();
        }

        int[][] ranges = {
                {0, 0},
                {1, 25},
                {26, 50},
                {51, 75},
                {76, 99},
                {100, 100}
        };
        String[] rangeLabels = {"0%", "1%-25%", "26%-50%", "51%-75%", "76%-99%", "100%"};

        List<ProgressRangeDTO> result = new ArrayList<>();
        for (int i = 0; i < ranges.length; i++) {
            int min = ranges[i][0];
            int max = ranges[i][1];
            long count = books.stream()
                    .filter(book -> {
                        Integer progress = book.getProgress();
                        if (progress == null) return min == 0;
                        return progress >= min && progress <= max;
                    })
                    .count();
            double percentage = (double) count / total * 100;
            result.add(new ProgressRangeDTO(rangeLabels[i], count, percentage));
        }

        return result;
    }
}

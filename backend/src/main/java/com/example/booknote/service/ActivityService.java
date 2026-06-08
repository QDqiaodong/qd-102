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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;

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
            LocalDate currentDate = date;
            int noteCount = notesByDate.getOrDefault(currentDate, Collections.emptyList()).size();
            List<Book> dayBooks = updatedBooksByDate.getOrDefault(currentDate, Collections.emptyList());
            int progressCount = (int) dayBooks.stream()
                    .filter(b -> b.getStatus() != Book.ReadingStatus.READ || !completedDates.contains(currentDate))
                    .count();
            int completedCount = completedDates.contains(currentDate)
                    ? (int) completedBooks.stream().filter(b -> b.getUpdatedAt().toLocalDate().equals(currentDate)).count()
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
            day.setDate(currentDate.toString());
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WakeUpBookItem {
        private BookDTO book;
        private long daysInactive;
        private String reason;
        private String category;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WakeUpListData {
        private List<WakeUpBookItem> stagnantReading;
        private List<WakeUpBookItem> neglectedWantToRead;
        private List<WakeUpBookItem> readWithoutSummary;
        private int totalCount;
    }

    public WakeUpListData getWakeUpList(int daysThreshold) {
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(daysThreshold);
        LocalDateTime now = LocalDateTime.now();

        List<Book> stagnantReadingBooks = bookRepository.findReadingBooksWithNoNotesSince(thresholdDate);
        List<Book> neglectedWantToReadBooks = bookRepository.findWantToReadBooksOlderThan(thresholdDate);
        List<Book> allReadBooks = bookRepository.findByStatus(Book.ReadingStatus.READ);

        List<Book> readWithoutSummaryBooks = allReadBooks.stream()
                .filter(book -> !noteRepository.hasSummaryNoteByBookId(book.getId()))
                .collect(Collectors.toList());

        List<WakeUpBookItem> stagnantReading = stagnantReadingBooks.stream()
                .map(book -> {
                    WakeUpBookItem item = new WakeUpBookItem();
                    item.setBook(BookDTO.fromEntity(book, bookRepository.countNotesByBookId(book.getId())));
                    LocalDateTime latestNoteDate = bookRepository.findLatestNoteDateByBookId(book.getId());
                    long days = latestNoteDate != null
                            ? ChronoUnit.DAYS.between(latestNoteDate.toLocalDate(), now.toLocalDate())
                            : ChronoUnit.DAYS.between(book.getCreatedAt().toLocalDate(), now.toLocalDate());
                    item.setDaysInactive(days);
                    item.setReason(latestNoteDate != null
                            ? "已连续 " + days + " 天无新增笔记"
                            : "开读 " + days + " 天仍无笔记");
                    item.setCategory("stagnantReading");
                    return item;
                })
                .sorted(Comparator.comparingLong(WakeUpBookItem::getDaysInactive).reversed())
                .collect(Collectors.toList());

        List<WakeUpBookItem> neglectedWantToRead = neglectedWantToReadBooks.stream()
                .map(book -> {
                    WakeUpBookItem item = new WakeUpBookItem();
                    item.setBook(BookDTO.fromEntity(book, 0L));
                    long days = ChronoUnit.DAYS.between(book.getCreatedAt().toLocalDate(), now.toLocalDate());
                    item.setDaysInactive(days);
                    item.setReason("加入想读 " + days + " 天仍未开始");
                    item.setCategory("neglectedWantToRead");
                    return item;
                })
                .sorted(Comparator.comparingLong(WakeUpBookItem::getDaysInactive).reversed())
                .collect(Collectors.toList());

        List<WakeUpBookItem> readWithoutSummary = readWithoutSummaryBooks.stream()
                .map(book -> {
                    WakeUpBookItem item = new WakeUpBookItem();
                    long noteCount = bookRepository.countNotesByBookId(book.getId());
                    item.setBook(BookDTO.fromEntity(book, noteCount));
                    long days = ChronoUnit.DAYS.between(book.getUpdatedAt().toLocalDate(), now.toLocalDate());
                    item.setDaysInactive(days);
                    item.setReason(noteCount > 0
                            ? "已读完 " + days + " 天，有 " + noteCount + " 条笔记但无总结"
                            : "已读完 " + days + " 天但无总结笔记");
                    item.setCategory("readWithoutSummary");
                    return item;
                })
                .sorted(Comparator.comparingLong(WakeUpBookItem::getDaysInactive).reversed())
                .collect(Collectors.toList());

        int totalCount = stagnantReading.size() + neglectedWantToRead.size() + readWithoutSummary.size();

        return new WakeUpListData(stagnantReading, neglectedWantToRead, readWithoutSummary, totalCount);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveBookItem {
        private int rank;
        private BookDTO book;
        private double activityScore;
        private int noteCount;
        private int progressChange;
        private long daysSinceLastActivity;
        private List<String> activityReasons;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveRankingData {
        private List<ActiveBookItem> rankings;
        private int totalBooks;
        private int daysRange;
        private Map<String, Object> summary;
    }

    public ActiveRankingData getActiveRanking(int days, int limit) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now.minusDays(days);

        List<Book> allBooks = bookRepository.findAll();
        List<Note> recentNotes = noteRepository.findByCreatedAtBetweenWithBook(startDate, now);

        Map<Long, List<Note>> notesByBook = recentNotes.stream()
                .collect(Collectors.groupingBy(note -> note.getBook().getId()));

        List<ActiveBookItem> activeBooks = new ArrayList<>();

        for (Book book : allBooks) {
            List<Note> bookNotes = notesByBook.getOrDefault(book.getId(), Collections.emptyList());

            LocalDateTime latestActivity = getLatestActivityTime(book, bookNotes, now);
            long daysSinceLastActivity = ChronoUnit.DAYS.between(latestActivity.toLocalDate(), now.toLocalDate());

            if (daysSinceLastActivity > days && bookNotes.isEmpty()) {
                continue;
            }

            double noteScore = calculateNoteScore(bookNotes, now, days);
            double progressScore = calculateProgressScore(book, bookNotes, now, days);
            double recencyScore = calculateRecencyScore(daysSinceLastActivity, days);

            double totalScore = noteScore + progressScore + recencyScore;

            if (totalScore <= 0) {
                continue;
            }

            int progressChange = calculateProgressChange(book, bookNotes);

            List<String> reasons = generateActivityReasons(bookNotes.size(), progressChange, daysSinceLastActivity);

            BookDTO bookDTO = BookDTO.fromEntity(book, (long) bookNotes.size());

            ActiveBookItem item = new ActiveBookItem();
            item.setBook(bookDTO);
            item.setActivityScore(round(totalScore, 2));
            item.setNoteCount(bookNotes.size());
            item.setProgressChange(progressChange);
            item.setDaysSinceLastActivity(daysSinceLastActivity);
            item.setActivityReasons(reasons);

            activeBooks.add(item);
        }

        activeBooks.sort(Comparator.comparingDouble(ActiveBookItem::getActivityScore).reversed());

        for (int i = 0; i < activeBooks.size(); i++) {
            activeBooks.get(i).setRank(i + 1);
        }

        List<ActiveBookItem> rankedBooks = activeBooks.stream()
                .limit(limit)
                .collect(Collectors.toList());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalActiveBooks", activeBooks.size());
        summary.put("totalNotes", recentNotes.size());
        summary.put("averageScore", activeBooks.isEmpty() ? 0 :
                round(activeBooks.stream().mapToDouble(ActiveBookItem::getActivityScore).average().orElse(0), 2));

        return new ActiveRankingData(rankedBooks, allBooks.size(), days, summary);
    }

    private LocalDateTime getLatestActivityTime(Book book, List<Note> notes, LocalDateTime now) {
        LocalDateTime latestNoteTime = notes.stream()
                .map(Note::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        LocalDateTime bookUpdateTime = book.getUpdatedAt();

        if (latestNoteTime != null && latestNoteTime.isAfter(bookUpdateTime)) {
            return latestNoteTime;
        }
        return bookUpdateTime;
    }

    private double calculateNoteScore(List<Note> notes, LocalDateTime now, int days) {
        double score = 0;
        for (Note note : notes) {
            long daysAgo = ChronoUnit.DAYS.between(note.getCreatedAt().toLocalDate(), now.toLocalDate());
            double weight = 1.0 - (daysAgo * 0.5 / days);
            if (weight < 0.1) weight = 0.1;
            score += 10 * weight;
        }
        return score;
    }

    private double calculateProgressScore(Book book, List<Note> notes, LocalDateTime now, int days) {
        if (book.getProgress() == null || book.getProgress() == 0) {
            return 0;
        }

        int progressChange = calculateProgressChange(book, notes);
        if (progressChange <= 0) {
            return 0;
        }

        LocalDateTime latestNoteTime = notes.stream()
                .map(Note::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(book.getUpdatedAt());

        long daysAgo = ChronoUnit.DAYS.between(latestNoteTime.toLocalDate(), now.toLocalDate());
        double weight = 1.0 - (daysAgo * 0.3 / days);
        if (weight < 0.2) weight = 0.2;

        return progressChange * 2 * weight;
    }

    private int calculateProgressChange(Book book, List<Note> notes) {
        if (notes.isEmpty()) {
            return 0;
        }

        List<Integer> pageNumbers = notes.stream()
                .map(Note::getPageNumber)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());

        if (pageNumbers.size() < 2) {
            return 0;
        }

        int minPage = pageNumbers.get(0);
        int maxPage = pageNumbers.get(pageNumbers.size() - 1);

        return Math.max(0, maxPage - minPage);
    }

    private double calculateRecencyScore(long daysSinceLastActivity, int days) {
        if (daysSinceLastActivity < 0) daysSinceLastActivity = 0;
        if (daysSinceLastActivity > days) return 0;

        double ratio = 1.0 - (double) daysSinceLastActivity / days;
        return 30 * ratio * ratio;
    }

    private List<String> generateActivityReasons(int noteCount, int progressChange, long daysSince) {
        List<String> reasons = new ArrayList<>();

        if (noteCount > 0) {
            reasons.add("近期产出 " + noteCount + " 条笔记");
        }

        if (progressChange > 0) {
            reasons.add("阅读进度推进约 " + progressChange + " 页");
        }

        if (daysSince == 0) {
            reasons.add("今日仍在活跃");
        } else if (daysSince <= 3) {
            reasons.add("近 " + daysSince + " 天有更新");
        }

        if (reasons.isEmpty()) {
            reasons.add("近期有阅读活动");
        }

        return reasons;
    }

    private double round(double value, int places) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}

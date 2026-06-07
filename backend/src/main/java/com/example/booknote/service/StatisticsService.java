
package com.example.booknote.service;

import com.example.booknote.dto.statistics.*;
import com.example.booknote.entity.Book;
import com.example.booknote.entity.Note;
import com.example.booknote.repository.BookRepository;
import com.example.booknote.repository.NoteRepository;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
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

    @Autowired
    private Analyzer analyzer;

    private static final Set<String> STOP_WORDS = Set.of(
            "的", "了", "在", "是", "我", "有", "和", "就",
            "不", "人", "都", "一", "一个", "上", "也", "很",
            "到", "说", "要", "去", "你", "会", "着", "没有",
            "看", "好", "自己", "这", "那", "他", "她", "它",
            "们", "这个", "那个", "什么", "怎么", "为什么",
            "因为", "所以", "但是", "而且", "或者", "如果",
            "虽然", "然而", "不过", "就是", "还是", "已经",
            "可以", "能", "能够", "应该", "必须", "需要",
            "可能", "大概", "大约", "几乎", "差不多",
            "一些", "一点", "有些", "有的", "一切",
            "之", "而", "其", "与", "及", "等", "等等",
            "中", "里", "内", "外", "前", "后", "上", "下",
            "左", "右", "高", "低", "大", "小", "多", "少",
            "来", "去", "进", "出", "回", "过", "起", "下",
            "把", "被", "让", "给", "对", "为", "以", "用",
            "从", "由", "于", "向", "往", "朝", "沿", "通过",
            "关于", "对于", "根据", "按照", "通过", "经过",
            "时候", "时间", "地方", "方面", "方式", "方法",
            "东西", "事情", "问题", "情况", "关系", "部分",
            "还", "更", "最", "真", "太", "挺", "蛮",
            "不是", "不会", "不能", "不要", "只是", "只有",
            "现在", "以前", "以后", "将来", "过去", "未来",
            "今天", "明天", "昨天", "今年", "去年", "明年",
            "然后", "接着", "最后", "终于", "首先", "其次",
            "这样", "那样", "如何", "这么", "那么",
            "其实", "当然", "显然", "确实", "的确",
            "也许", "或许", "恐怕", "估计", "据说",
            "觉得", "认为", "以为", "感到", "感觉",
            "知道", "明白", "理解", "了解", "清楚",
            "找到", "发现", "看到", "听到", "想到",
            "得到", "获得", "取得", "达到", "完成",
            "进行", "开展", "实施", "执行", "推动",
            "发展", "提高", "增强", "加强", "改进",
            "非常", "特别", "十分", "极其", "相当",
            "比较", "更加", "越来越", "越",
            "这里", "那里", "哪里", "此处", "彼处",
            "下来", "起来", "出来", "上来", "过去",
            "书", "本书", "这本", "那本"
    );

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

    public List<ConceptTermDTO> getConceptDictionary(int limit, int minFrequency) {
        List<Note> notes = noteRepository.findAll();
        if (notes.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, TermStats> statsMap = new HashMap<>();

        for (Note note : notes) {
            Set<String> termsInNote = new HashSet<>();
            String text = (note.getTitle() != null ? note.getTitle() : "") + " " +
                          (note.getContent() != null ? note.getContent() : "");

            try {
                List<String> tokens = tokenize(text);
                for (String token : tokens) {
                    if (isValidTerm(token)) {
                        statsMap.computeIfAbsent(token, k -> new TermStats())
                                .incrementFrequency();
                        termsInNote.add(token);
                    }
                }
            } catch (IOException e) {
                continue;
            }

            Long bookId = note.getBook() != null ? note.getBook().getId() : null;
            for (String term : termsInNote) {
                TermStats stats = statsMap.get(term);
                if (stats != null) {
                    stats.incrementNoteCount();
                    if (bookId != null) {
                        stats.addBookId(bookId);
                    }
                }
            }
        }

        return statsMap.entrySet().stream()
                .filter(entry -> entry.getValue().getFrequency() >= minFrequency)
                .sorted((a, b) -> Long.compare(b.getValue().getFrequency(), a.getValue().getFrequency()))
                .limit(limit)
                .map(entry -> new ConceptTermDTO(
                        entry.getKey(),
                        entry.getValue().getFrequency(),
                        entry.getValue().getBookCount(),
                        entry.getValue().getNoteCount()
                ))
                .collect(Collectors.toList());
    }

    private List<String> tokenize(String text) throws IOException {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return tokens;
        }

        String plainText = text.replaceAll("<[^>]*>", " ");

        try (TokenStream tokenStream = analyzer.tokenStream(null, new StringReader(plainText))) {
            CharTermAttribute charTermAttr = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                tokens.add(charTermAttr.toString());
            }
            tokenStream.end();
        }

        return tokens;
    }

    private boolean isValidTerm(String term) {
        if (term == null || term.isEmpty()) {
            return false;
        }
        if (term.length() < 2) {
            return false;
        }
        if (STOP_WORDS.contains(term)) {
            return false;
        }
        if (term.matches("\\d+")) {
            return false;
        }
        if (term.matches("[\\p{Punct}\\p{IsPunctuation}]+")) {
            return false;
        }
        return true;
    }

    private static class TermStats {
        private String term;
        private long frequency;
        private long noteCount;
        private Set<Long> bookIds = new HashSet<>();

        public void incrementFrequency() {
            frequency++;
        }

        public void incrementNoteCount() {
            noteCount++;
        }

        public void addBookId(Long bookId) {
            bookIds.add(bookId);
        }

        public long getFrequency() {
            return frequency;
        }

        public long getNoteCount() {
            return noteCount;
        }

        public long getBookCount() {
            return bookIds.size();
        }

        public String getTerm() {
            return term;
        }

        public void setTerm(String term) {
            this.term = term;
        }
    }
}

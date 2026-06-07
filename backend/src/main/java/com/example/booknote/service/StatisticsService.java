
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

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
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
    ));

    private static final Set<String> TERM_SUFFIXES = new HashSet<>(Arrays.asList(
            "学", "主义", "理论", "模型", "效应", "定律", "定理",
            "法则", "原则", "方法", "算法", "模式", "体系", "系统",
            "学派", "学说", "概念", "范畴", "范式", "框架", "结构",
            "机制", "原理", "规律", "周期", "阶段", "层次", "维度",
            "视角", "路径", "策略", "方案", "流程", "标准"
    ));

    private static final List<String> PERSON_TITLES = Arrays.asList(
            "先生", "女士", "小姐", "教授", "博士", "老师",
            "院长", "校长", "主席", "总理", "总统", "国王",
            "女王", "王子", "公主", "公爵", "伯爵", "爵士",
            "大夫", "医生", "律师", "工程师", "作家", "诗人",
            "画家", "音乐家", "科学家", "哲学家", "心理学家",
            "经济学家", "思想家", "教育家", "企业家", "政治家",
            "先生说", "女士说", "教授说", "博士说", "老师说",
            "先生认为", "女士认为", "教授认为", "博士认为",
            "先生指出", "教授指出", "博士指出",
            "先生提出", "教授提出", "博士提出",
            "先生发现", "教授发现", "博士发现"
    );

    private static final Set<String> COMMON_SURNAMES = new HashSet<>(Arrays.asList(
            "李", "王", "张", "刘", "陈", "杨", "黄", "赵", "周", "吴",
            "徐", "孙", "胡", "朱", "高", "林", "何", "郭", "马", "罗",
            "梁", "宋", "郑", "谢", "韩", "唐", "冯", "于", "董", "萧",
            "程", "曹", "袁", "邓", "许", "傅", "沈", "曾", "彭", "吕",
            "苏", "卢", "蒋", "蔡", "贾", "丁", "魏", "薛", "叶", "阎",
            "余", "潘", "杜", "戴", "夏", "钟", "汪", "田", "任", "姜",
            "范", "方", "石", "姚", "谭", "廖", "邹", "熊", "金", "陆",
            "郝", "孔", "白", "崔", "康", "毛", "邱", "秦", "江", "史",
            "顾", "侯", "邵", "孟", "龙", "万", "段", "雷", "钱", "汤",
            "尹", "黎", "易", "常", "武", "乔", "贺", "赖", "龚", "文",
            "欧阳", "司马", "诸葛", "上官", "夏侯", "皇甫", "尉迟",
            "公孙", "慕容", "司徒", "司空", "澹台", "公冶", "宗政",
            "濮阳", "淳于", "单于", "太叔", "申屠", "仲孙", "轩辕",
            "令狐", "钟离", "宇文", "长孙", "慕容", "鲜于", "闾丘",
            "亓官", "司寇", "仉", "督", "子车"
    ));

    private static final Set<String> COMPOUND_SURNAMES = new HashSet<>(Arrays.asList(
            "欧阳", "司马", "诸葛", "上官", "夏侯", "皇甫", "尉迟",
            "公孙", "慕容", "司徒", "司空", "澹台", "公冶", "宗政",
            "濮阳", "淳于", "单于", "太叔", "申屠", "仲孙", "轩辕",
            "令狐", "钟离", "宇文", "长孙", "鲜于", "闾丘",
            "亓官", "司寇", "子车"
    ));

    private static boolean isChineseSurname(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        if (str.length() >= 2) {
            String firstTwo = str.substring(0, 2);
            if (COMPOUND_SURNAMES.contains(firstTwo)) {
                return true;
            }
        }
        return COMMON_SURNAMES.contains(str.substring(0, 1));
    }

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
        return getConceptDictionary(limit, minFrequency, null);
    }

    public List<ConceptTermDTO> getConceptDictionary(int limit, int minFrequency, String typeFilter) {
        List<Note> notes = noteRepository.findAll();
        if (notes.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, TermStats> statsMap = new HashMap<>();
        Set<String> personCandidates = new HashSet<>();

        for (Note note : notes) {
            String text = (note.getTitle() != null ? note.getTitle() : "") + " " +
                          (note.getContent() != null ? note.getContent() : "");

            try {
                List<String> tokens = tokenize(text);
                List<String> validTokens = new ArrayList<>();

                for (String token : tokens) {
                    if (isValidTerm(token)) {
                        validTokens.add(token);
                    }
                }

                for (String token : validTokens) {
                    statsMap.computeIfAbsent(token, k -> new TermStats())
                            .incrementFrequency();
                }

                List<String> bigrams = generateNGrams(validTokens, 2);
                List<String> trigrams = generateNGrams(validTokens, 3);
                for (String gram : bigrams) {
                    statsMap.computeIfAbsent(gram, k -> new TermStats())
                            .incrementFrequency();
                }
                for (String gram : trigrams) {
                    statsMap.computeIfAbsent(gram, k -> new TermStats())
                            .incrementFrequency();
                }

                Set<String> personsInNote = extractPersonNames(text, tokens);
                personCandidates.addAll(personsInNote);
                for (String person : personsInNote) {
                    statsMap.computeIfAbsent(person, k -> new TermStats())
                            .incrementFrequency();
                }

                Set<String> termsInNote = new HashSet<>(validTokens);
                termsInNote.addAll(bigrams);
                termsInNote.addAll(trigrams);
                termsInNote.addAll(personsInNote);

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
            } catch (IOException e) {
                continue;
            }
        }

        long totalNotes = notes.size();

        return statsMap.entrySet().stream()
                .filter(entry -> entry.getValue().getFrequency() >= minFrequency)
                .map(entry -> {
                    String term = entry.getKey();
                    TermStats stats = entry.getValue();
                    String type = classifyTerm(term, personCandidates, stats);
                    double score = calculateScore(term, stats, type, totalNotes);
                    return new ConceptTermDTO(term, type, stats.getFrequency(),
                            stats.getBookCount(), stats.getNoteCount(), score);
                })
                .filter(dto -> typeFilter == null || typeFilter.equalsIgnoreCase(dto.getType()))
                .filter(dto -> !isNoisyTerm(dto))
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<String> generateNGrams(List<String> tokens, int n) {
        List<String> ngrams = new ArrayList<>();
        if (tokens.size() < n) {
            return ngrams;
        }
        for (int i = 0; i <= tokens.size() - n; i++) {
            StringBuilder sb = new StringBuilder();
            boolean valid = true;
            for (int j = 0; j < n; j++) {
                String token = tokens.get(i + j);
                if (!isValidNGramPart(token)) {
                    valid = false;
                    break;
                }
                sb.append(token);
            }
            if (valid) {
                String gram = sb.toString();
                if (gram.length() >= 2 && !STOP_WORDS.contains(gram)) {
                    ngrams.add(gram);
                }
            }
        }
        return ngrams;
    }

    private boolean isValidNGramPart(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        if (STOP_WORDS.contains(token)) {
            return false;
        }
        if (token.length() < 1) {
            return false;
        }
        if (token.matches("\\d+")) {
            return false;
        }
        if (token.matches("[\\p{Punct}\\p{IsPunctuation}]+")) {
            return false;
        }
        return true;
    }

    private Set<String> extractPersonNames(String text, List<String> tokens) {
        Set<String> persons = new HashSet<>();

        for (String title : PERSON_TITLES) {
            int idx = 0;
            while ((idx = text.indexOf(title, idx)) != -1) {
                int start = Math.max(0, idx - 6);
                String prefix = text.substring(start, idx);
                String name = extractNameBeforeTitle(prefix);
                if (name != null && name.length() >= 2 && name.length() <= 4) {
                    persons.add(name);
                }
                idx += title.length();
            }
        }

        for (String token : tokens) {
            if (isLikelyPersonName(token)) {
                persons.add(token);
            }
        }

        return persons;
    }

    private String extractNameBeforeTitle(String prefix) {
        if (prefix.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = prefix.length() - 1; i >= 0; i--) {
            char c = prefix.charAt(i);
            if (isChineseChar(c) || Character.isLetter(c)) {
                sb.insert(0, c);
                if (sb.length() >= 4) {
                    break;
                }
            } else {
                break;
            }
        }

        String name = sb.toString();
        if (name.isEmpty() || name.length() < 2 || name.length() > 4) {
            return null;
        }

        if (isChineseSurname(name)) {
            return name;
        }

        return null;
    }

    private boolean isLikelyPersonName(String token) {
        if (token == null || token.length() < 2 || token.length() > 4) {
            return false;
        }

        if (!isAllChinese(token)) {
            return false;
        }

        if (!isChineseSurname(token)) {
            return false;
        }

        if (STOP_WORDS.contains(token)) {
            return false;
        }

        return true;
    }

    private boolean isChineseChar(char c) {
        return c >= '\u4e00' && c <= '\u9fff';
    }

    private boolean isAllChinese(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (char c : str.toCharArray()) {
            if (!isChineseChar(c)) {
                return false;
            }
        }
        return true;
    }

    private String classifyTerm(String term, Set<String> personCandidates, TermStats stats) {
        if (personCandidates.contains(term)) {
            return "PERSON";
        }

        if (hasTermSuffix(term)) {
            return "TERM";
        }

        if (term.length() >= 4 && isAllChinese(term)) {
            return "TERM";
        }

        if (term.length() >= 3 && stats.getBookCount() >= 2) {
            return "TERM";
        }

        return "CONCEPT";
    }

    private boolean hasTermSuffix(String term) {
        if (term == null || term.length() < 2) {
            return false;
        }
        for (String suffix : TERM_SUFFIXES) {
            if (term.endsWith(suffix) && term.length() > suffix.length()) {
                return true;
            }
        }
        return false;
    }

    private double calculateScore(String term, TermStats stats, String type, long totalNotes) {
        double score = stats.getFrequency();

        double lengthBonus = 1.0;
        if (term.length() >= 4) {
            lengthBonus = 1.5;
        } else if (term.length() >= 3) {
            lengthBonus = 1.2;
        }
        score *= lengthBonus;

        if ("PERSON".equals(type)) {
            score *= 2.0;
        } else if ("TERM".equals(type)) {
            score *= 1.5;
        }

        if (hasTermSuffix(term)) {
            score *= 1.2;
        }

        if (stats.getBookCount() > 0) {
            double idf = Math.log((double) totalNotes / stats.getBookCount() + 1);
            score *= (1 + idf * 0.3);
        }

        return Math.round(score * 100.0) / 100.0;
    }

    private boolean isNoisyTerm(ConceptTermDTO dto) {
        String term = dto.getTerm();
        String type = dto.getType();
        long frequency = dto.getFrequency();
        long bookCount = dto.getBookCount();

        if ("PERSON".equals(type)) {
            return frequency < 2;
        }

        if ("TERM".equals(type)) {
            if (term.length() == 2) {
                return frequency < 3 || bookCount < 1;
            }
            return frequency < 2;
        }

        if (term.length() == 2) {
            return bookCount < 2 || frequency < 3;
        }

        if (term.length() == 3) {
            return frequency < 2 || bookCount < 1;
        }

        return false;
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

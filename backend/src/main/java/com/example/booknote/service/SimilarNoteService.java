
package com.example.booknote.service;

import com.example.booknote.dto.NoteDTO;
import com.example.booknote.dto.SimilarNotePairDTO;
import com.example.booknote.entity.Note;
import com.example.booknote.repository.NoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SimilarNoteService {

    @Autowired
    private NoteRepository noteRepository;

    private static final double TITLE_WEIGHT = 0.4;
    private static final double CONTENT_WEIGHT = 0.6;
    private static final double DEFAULT_THRESHOLD = 0.6;
    private static final int N_GRAM_SIZE = 2;

    public List<SimilarNotePairDTO> findSimilarNotes(Long bookId, Double threshold, Boolean crossBook) {
        double actualThreshold = threshold != null ? threshold : DEFAULT_THRESHOLD;
        boolean isCrossBook = Boolean.TRUE.equals(crossBook);

        if (bookId != null) {
            List<Note> notes = noteRepository.findByBookId(bookId);
            return findSimilarPairsFromList(notes, actualThreshold);
        }

        List<Note> allNotes = noteRepository.findAll();

        if (isCrossBook) {
            return findSimilarPairsFromList(allNotes, actualThreshold);
        }

        Map<Long, List<Note>> notesByBook = allNotes.stream()
                .filter(note -> note.getBook() != null)
                .collect(Collectors.groupingBy(note -> note.getBook().getId()));

        List<SimilarNotePairDTO> allSimilarPairs = new ArrayList<>();
        for (List<Note> bookNotes : notesByBook.values()) {
            allSimilarPairs.addAll(findSimilarPairsFromList(bookNotes, actualThreshold));
        }

        allSimilarPairs.sort((p1, p2) -> Double.compare(p2.getSimilarityScore(), p1.getSimilarityScore()));
        return allSimilarPairs;
    }

    private List<SimilarNotePairDTO> findSimilarPairsFromList(List<Note> notes, double threshold) {
        if (notes == null || notes.size() < 2) {
            return Collections.emptyList();
        }

        List<SimilarNotePairDTO> similarPairs = new ArrayList<>();
        Set<String> processedPairs = new HashSet<>();

        for (int i = 0; i < notes.size(); i++) {
            for (int j = i + 1; j < notes.size(); j++) {
                Note note1 = notes.get(i);
                Note note2 = notes.get(j);

                String pairKey = generatePairKey(note1.getId(), note2.getId());
                if (processedPairs.contains(pairKey)) {
                    continue;
                }

                double titleSimilarity = calculateTitleSimilarity(note1.getTitle(), note2.getTitle());
                double contentSimilarity = calculateContentSimilarity(note1.getContent(), note2.getContent());
                double totalSimilarity = TITLE_WEIGHT * titleSimilarity + CONTENT_WEIGHT * contentSimilarity;

                if (totalSimilarity >= threshold) {
                    SimilarNotePairDTO pairDTO = new SimilarNotePairDTO();
                    pairDTO.setNote1(NoteDTO.fromEntity(note1));
                    pairDTO.setNote2(NoteDTO.fromEntity(note2));
                    pairDTO.setSimilarityScore(Math.round(totalSimilarity * 100.0) / 100.0);
                    pairDTO.setTitleSimilarity(Math.round(titleSimilarity * 100.0) / 100.0);
                    pairDTO.setContentSimilarity(Math.round(contentSimilarity * 100.0) / 100.0);
                    pairDTO.setSimilarityReason(generateSimilarityReason(titleSimilarity, contentSimilarity));
                    similarPairs.add(pairDTO);
                    processedPairs.add(pairKey);
                }
            }
        }

        similarPairs.sort((p1, p2) -> Double.compare(p2.getSimilarityScore(), p1.getSimilarityScore()));
        return similarPairs;
    }

    public List<SimilarNotePairDTO> findSimilarNotesForNote(Long noteId, Double threshold, Boolean crossBook) {
        double actualThreshold = threshold != null ? threshold : DEFAULT_THRESHOLD;
        boolean isCrossBook = Boolean.TRUE.equals(crossBook);
        Note targetNote = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Note not found"));

        List<Note> candidateNotes;
        if (isCrossBook || targetNote.getBook() == null) {
            candidateNotes = noteRepository.findAll();
        } else {
            candidateNotes = noteRepository.findByBookId(targetNote.getBook().getId());
        }

        List<SimilarNotePairDTO> similarPairs = new ArrayList<>();

        for (Note note : candidateNotes) {
            if (note.getId().equals(noteId)) {
                continue;
            }

            double titleSimilarity = calculateTitleSimilarity(targetNote.getTitle(), note.getTitle());
            double contentSimilarity = calculateContentSimilarity(targetNote.getContent(), note.getContent());
            double totalSimilarity = TITLE_WEIGHT * titleSimilarity + CONTENT_WEIGHT * contentSimilarity;

            if (totalSimilarity >= actualThreshold) {
                SimilarNotePairDTO pairDTO = new SimilarNotePairDTO();
                pairDTO.setNote1(NoteDTO.fromEntity(targetNote));
                pairDTO.setNote2(NoteDTO.fromEntity(note));
                pairDTO.setSimilarityScore(Math.round(totalSimilarity * 100.0) / 100.0);
                pairDTO.setTitleSimilarity(Math.round(titleSimilarity * 100.0) / 100.0);
                pairDTO.setContentSimilarity(Math.round(contentSimilarity * 100.0) / 100.0);
                pairDTO.setSimilarityReason(generateSimilarityReason(titleSimilarity, contentSimilarity));
                similarPairs.add(pairDTO);
            }
        }

        similarPairs.sort((p1, p2) -> Double.compare(p2.getSimilarityScore(), p1.getSimilarityScore()));

        return similarPairs;
    }

    public Map<String, Object> getSimilarityStatistics(Long bookId, Boolean crossBook) {
        List<SimilarNotePairDTO> similarPairs = findSimilarNotes(bookId, DEFAULT_THRESHOLD, crossBook);
        Map<String, Object> stats = new HashMap<>();

        long totalNotes;
        if (bookId != null) {
            totalNotes = noteRepository.countByBookId(bookId);
        } else if (Boolean.TRUE.equals(crossBook)) {
            totalNotes = noteRepository.count();
        } else {
            totalNotes = noteRepository.count();
        }
        Set<Long> similarNoteIds = new HashSet<>();

        for (SimilarNotePairDTO pair : similarPairs) {
            similarNoteIds.add(pair.getNote1().getId());
            similarNoteIds.add(pair.getNote2().getId());
        }

        stats.put("totalNotes", totalNotes);
        stats.put("similarPairCount", similarPairs.size());
        stats.put("similarNoteCount", similarNoteIds.size());
        stats.put("duplicateRatio", totalNotes > 0 ? Math.round((double) similarNoteIds.size() / totalNotes * 100.0) / 100.0 : 0);

        if (!similarPairs.isEmpty()) {
            double maxSimilarity = similarPairs.get(0).getSimilarityScore();
            double avgSimilarity = similarPairs.stream()
                    .mapToDouble(SimilarNotePairDTO::getSimilarityScore)
                    .average()
                    .orElse(0);
            stats.put("maxSimilarity", Math.round(maxSimilarity * 100.0) / 100.0);
            stats.put("avgSimilarity", Math.round(avgSimilarity * 100.0) / 100.0);
        } else {
            stats.put("maxSimilarity", 0);
            stats.put("avgSimilarity", 0);
        }

        return stats;
    }

    private String generatePairKey(Long id1, Long id2) {
        if (id1 < id2) {
            return id1 + "-" + id2;
        } else {
            return id2 + "-" + id1;
        }
    }

    private double calculateTitleSimilarity(String title1, String title2) {
        if (title1 == null && title2 == null) return 1.0;
        if (title1 == null || title2 == null) return 0.0;

        title1 = title1.trim().toLowerCase();
        title2 = title2.trim().toLowerCase();

        if (title1.equals(title2)) return 1.0;
        if (title1.isEmpty() && title2.isEmpty()) return 1.0;
        if (title1.isEmpty() || title2.isEmpty()) return 0.0;

        int maxLength = Math.max(title1.length(), title2.length());
        if (maxLength == 0) return 1.0;

        int levenshteinDistance = levenshteinDistance(title1, title2);
        double distanceSimilarity = 1.0 - (double) levenshteinDistance / maxLength;

        double nGramSimilarity = calculateNGramSimilarity(title1, title2, N_GRAM_SIZE);

        return 0.5 * distanceSimilarity + 0.5 * nGramSimilarity;
    }

    private double calculateContentSimilarity(String content1, String content2) {
        if (content1 == null && content2 == null) return 1.0;
        if (content1 == null || content2 == null) return 0.0;

        content1 = content1.trim().toLowerCase();
        content2 = content2.trim().toLowerCase();

        if (content1.equals(content2)) return 1.0;
        if (content1.isEmpty() && content2.isEmpty()) return 1.0;
        if (content1.isEmpty() || content2.isEmpty()) return 0.0;

        double nGramSimilarity = calculateNGramSimilarity(content1, content2, N_GRAM_SIZE);

        double lengthRatio = calculateLengthRatioSimilarity(content1.length(), content2.length());

        double containmentSimilarity = calculateContainmentSimilarity(content1, content2);

        return 0.5 * nGramSimilarity + 0.3 * containmentSimilarity + 0.2 * lengthRatio;
    }

    private double calculateNGramSimilarity(String text1, String text2, int n) {
        Set<String> nGrams1 = generateNGrams(text1, n);
        Set<String> nGrams2 = generateNGrams(text2, n);

        if (nGrams1.isEmpty() && nGrams2.isEmpty()) return 1.0;
        if (nGrams1.isEmpty() || nGrams2.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(nGrams1);
        intersection.retainAll(nGrams2);

        Set<String> union = new HashSet<>(nGrams1);
        union.addAll(nGrams2);

        return (double) intersection.size() / union.size();
    }

    private Set<String> generateNGrams(String text, int n) {
        Set<String> nGrams = new HashSet<>();
        if (text == null || text.length() < n) {
            return nGrams;
        }

        String cleanText = text.replaceAll("\\s+", "");
        for (int i = 0; i <= cleanText.length() - n; i++) {
            nGrams.add(cleanText.substring(i, i + n));
        }
        return nGrams;
    }

    private double calculateLengthRatioSimilarity(int len1, int len2) {
        if (len1 == 0 && len2 == 0) return 1.0;
        if (len1 == 0 || len2 == 0) return 0.0;

        int minLen = Math.min(len1, len2);
        int maxLen = Math.max(len1, len2);

        return (double) minLen / maxLen;
    }

    private double calculateContainmentSimilarity(String text1, String text2) {
        if (text1.isEmpty() && text2.isEmpty()) return 1.0;
        if (text1.isEmpty() || text2.isEmpty()) return 0.0;

        String shorter = text1.length() <= text2.length() ? text1 : text2;
        String longer = text1.length() > text2.length() ? text1 : text2;

        if (longer.contains(shorter)) {
            return (double) shorter.length() / longer.length();
        }

        int maxCommonPrefix = 0;
        int minLen = Math.min(text1.length(), text2.length());
        for (int i = 0; i < minLen; i++) {
            if (text1.charAt(i) == text2.charAt(i)) {
                maxCommonPrefix++;
            } else {
                break;
            }
        }

        return (double) maxCommonPrefix / longer.length();
    }

    private int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();

        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }

        return dp[len1][len2];
    }

    private String generateSimilarityReason(double titleSimilarity, double contentSimilarity) {
        List<String> reasons = new ArrayList<>();

        if (titleSimilarity >= 0.8) {
            reasons.add("标题高度相似");
        } else if (titleSimilarity >= 0.5) {
            reasons.add("标题部分相似");
        }

        if (contentSimilarity >= 0.8) {
            reasons.add("内容高度重复");
        } else if (contentSimilarity >= 0.5) {
            reasons.add("内容部分相似");
        }

        if (reasons.isEmpty()) {
            reasons.add("存在一定相似性");
        }

        return String.join("、", reasons);
    }
}

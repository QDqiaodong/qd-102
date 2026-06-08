
package com.example.booknote.service;

import com.example.booknote.dto.ReadingPlanDTO;
import com.example.booknote.dto.ReadingPlanSegmentDTO;
import com.example.booknote.entity.Book;
import com.example.booknote.entity.ReadingPlan;
import com.example.booknote.entity.ReadingPlanSegment;
import com.example.booknote.repository.BookRepository;
import com.example.booknote.repository.ReadingPlanRepository;
import com.example.booknote.repository.ReadingPlanSegmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReadingPlanService {

    @Autowired
    private ReadingPlanRepository readingPlanRepository;

    @Autowired
    private ReadingPlanSegmentRepository readingPlanSegmentRepository;

    @Autowired
    private BookRepository bookRepository;

    public List<ReadingPlanDTO> getPlansByBookId(Long bookId) {
        List<ReadingPlan> plans = readingPlanRepository.findByBookIdOrderByCreatedAtDesc(bookId);
        return plans.stream()
                .map(ReadingPlanDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public Optional<ReadingPlanDTO> getPlanById(Long planId) {
        return readingPlanRepository.findById(planId)
                .map(ReadingPlanDTO::fromEntity);
    }

    public Optional<ReadingPlanDTO> getLatestPlanByBookId(Long bookId) {
        return readingPlanRepository.findFirstByBookIdOrderByCreatedAtDesc(bookId)
                .map(ReadingPlanDTO::fromEntity);
    }

    @Transactional
    public ReadingPlanDTO createPlan(Long bookId, ReadingPlanDTO planDTO) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        ReadingPlan plan = new ReadingPlan();
        plan.setBook(book);
        plan.setTitle(planDTO.getTitle());
        plan.setTotalPages(planDTO.getTotalPages());

        ReadingPlan savedPlan = readingPlanRepository.save(plan);

        if (planDTO.getSegments() != null && !planDTO.getSegments().isEmpty()) {
            List<ReadingPlanSegment> segments = new ArrayList<>();
            int sortOrder = 0;
            for (ReadingPlanSegmentDTO segmentDTO : planDTO.getSegments()) {
                ReadingPlanSegment segment = new ReadingPlanSegment();
                segment.setReadingPlan(savedPlan);
                segment.setSegmentTitle(segmentDTO.getSegmentTitle());
                segment.setStartPage(segmentDTO.getStartPage());
                segment.setEndPage(segmentDTO.getEndPage());
                segment.setEstimatedCompletionDate(segmentDTO.getEstimatedCompletionDate());
                segment.setCurrentPage(segmentDTO.getCurrentPage());
                segment.setSortOrder(sortOrder++);
                updateSegmentStatus(segment);
                segments.add(segment);
            }
            readingPlanSegmentRepository.saveAll(segments);
            savedPlan.setSegments(segments);
        }

        return ReadingPlanDTO.fromEntity(savedPlan);
    }

    @Transactional
    public ReadingPlanDTO updatePlan(Long planId, ReadingPlanDTO planDTO) {
        ReadingPlan plan = readingPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Reading plan not found"));

        if (planDTO.getTitle() != null) {
            plan.setTitle(planDTO.getTitle());
        }
        if (planDTO.getTotalPages() != null) {
            plan.setTotalPages(planDTO.getTotalPages());
        }

        ReadingPlan savedPlan = readingPlanRepository.save(plan);
        return ReadingPlanDTO.fromEntity(savedPlan);
    }

    @Transactional
    public void deletePlan(Long planId) {
        if (!readingPlanRepository.existsById(planId)) {
            throw new RuntimeException("Reading plan not found");
        }
        readingPlanRepository.deleteById(planId);
    }

    @Transactional
    public ReadingPlanSegmentDTO addSegment(Long planId, ReadingPlanSegmentDTO segmentDTO) {
        ReadingPlan plan = readingPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Reading plan not found"));

        ReadingPlanSegment segment = new ReadingPlanSegment();
        segment.setReadingPlan(plan);
        segment.setSegmentTitle(segmentDTO.getSegmentTitle());
        segment.setStartPage(segmentDTO.getStartPage());
        segment.setEndPage(segmentDTO.getEndPage());
        segment.setEstimatedCompletionDate(segmentDTO.getEstimatedCompletionDate());
        segment.setCurrentPage(segmentDTO.getCurrentPage());

        List<ReadingPlanSegment> existingSegments = readingPlanSegmentRepository.findByReadingPlanIdOrderBySortOrderAsc(planId);
        segment.setSortOrder(existingSegments.size());

        updateSegmentStatus(segment);

        ReadingPlanSegment savedSegment = readingPlanSegmentRepository.save(segment);
        return ReadingPlanSegmentDTO.fromEntity(savedSegment);
    }

    @Transactional
    public ReadingPlanSegmentDTO updateSegment(Long segmentId, ReadingPlanSegmentDTO segmentDTO) {
        ReadingPlanSegment segment = readingPlanSegmentRepository.findById(segmentId)
                .orElseThrow(() -> new RuntimeException("Reading plan segment not found"));

        if (segmentDTO.getSegmentTitle() != null) {
            segment.setSegmentTitle(segmentDTO.getSegmentTitle());
        }
        if (segmentDTO.getStartPage() != null) {
            segment.setStartPage(segmentDTO.getStartPage());
        }
        if (segmentDTO.getEndPage() != null) {
            segment.setEndPage(segmentDTO.getEndPage());
        }
        if (segmentDTO.getEstimatedCompletionDate() != null) {
            segment.setEstimatedCompletionDate(segmentDTO.getEstimatedCompletionDate());
        }
        if (segmentDTO.getCurrentPage() != null) {
            segment.setCurrentPage(segmentDTO.getCurrentPage());
        }
        if (segmentDTO.getSortOrder() != null) {
            segment.setSortOrder(segmentDTO.getSortOrder());
        }

        updateSegmentStatus(segment);

        ReadingPlanSegment savedSegment = readingPlanSegmentRepository.save(segment);
        return ReadingPlanSegmentDTO.fromEntity(savedSegment);
    }

    @Transactional
    public ReadingPlanSegmentDTO updateSegmentProgress(Long segmentId, Integer currentPage) {
        ReadingPlanSegment segment = readingPlanSegmentRepository.findById(segmentId)
                .orElseThrow(() -> new RuntimeException("Reading plan segment not found"));

        segment.setCurrentPage(currentPage);
        updateSegmentStatus(segment);

        if (segment.getStatus() == ReadingPlanSegment.SegmentStatus.COMPLETED && segment.getActualCompletionDate() == null) {
            segment.setActualCompletionDate(LocalDate.now());
        } else if (segment.getStatus() != ReadingPlanSegment.SegmentStatus.COMPLETED) {
            segment.setActualCompletionDate(null);
        }

        ReadingPlanSegment savedSegment = readingPlanSegmentRepository.save(segment);
        syncBookProgress(segment.getReadingPlan().getId());
        return ReadingPlanSegmentDTO.fromEntity(savedSegment);
    }

    @Transactional
    public void deleteSegment(Long segmentId) {
        ReadingPlanSegment segment = readingPlanSegmentRepository.findById(segmentId)
                .orElseThrow(() -> new RuntimeException("Reading plan segment not found"));
        readingPlanSegmentRepository.delete(segment);
    }

    @Transactional
    public ReadingPlanDTO reorderSegments(Long planId, List<Long> segmentIds) {
        ReadingPlan plan = readingPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Reading plan not found"));

        List<ReadingPlanSegment> segments = readingPlanSegmentRepository.findByReadingPlanIdOrderBySortOrderAsc(planId);

        for (int i = 0; i < segmentIds.size(); i++) {
            Long segmentId = segmentIds.get(i);
            final int index = i;
            segments.stream()
                    .filter(s -> s.getId().equals(segmentId))
                    .findFirst()
                    .ifPresent(s -> s.setSortOrder(index));
        }

        readingPlanSegmentRepository.saveAll(segments);

        return ReadingPlanDTO.fromEntity(readingPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Reading plan not found")));
    }

    @Transactional
    public ReadingPlanDTO generatePlanByPageCount(Long bookId, Integer pagesPerSegment, Integer totalPages, String startDate) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        if (totalPages == null || totalPages <= 0) {
            throw new IllegalArgumentException("总页数必须大于0");
        }
        if (pagesPerSegment == null || pagesPerSegment <= 0) {
            throw new IllegalArgumentException("每段页数必须大于0");
        }

        ReadingPlan plan = new ReadingPlan();
        plan.setBook(book);
        plan.setTitle(book.getTitle() + " - 阅读计划");
        plan.setTotalPages(totalPages);

        ReadingPlan savedPlan = readingPlanRepository.save(plan);

        LocalDate date = startDate != null ? LocalDate.parse(startDate) : LocalDate.now();
        List<ReadingPlanSegment> segments = new ArrayList<>();
        int sortOrder = 0;
        int currentPage = 1;
        int segmentNum = 1;

        while (currentPage <= totalPages) {
            int endPage = Math.min(currentPage + pagesPerSegment - 1, totalPages);

            ReadingPlanSegment segment = new ReadingPlanSegment();
            segment.setReadingPlan(savedPlan);
            segment.setSegmentTitle("第" + segmentNum + "部分");
            segment.setStartPage(currentPage);
            segment.setEndPage(endPage);
            segment.setEstimatedCompletionDate(date);
            segment.setSortOrder(sortOrder++);
            updateSegmentStatus(segment);

            segments.add(segment);

            currentPage = endPage + 1;
            segmentNum++;
            date = date.plusDays(7);
        }

        readingPlanSegmentRepository.saveAll(segments);
        savedPlan.setSegments(segments);

        return ReadingPlanDTO.fromEntity(savedPlan);
    }

    private void updateSegmentStatus(ReadingPlanSegment segment) {
        int totalPages = segment.getEndPage() - segment.getStartPage() + 1;
        int readPages = 0;
        if (segment.getCurrentPage() != null && segment.getCurrentPage() >= segment.getStartPage()) {
            readPages = Math.min(totalPages, segment.getCurrentPage() - segment.getStartPage() + 1);
        }

        if (readPages <= 0) {
            segment.setStatus(ReadingPlanSegment.SegmentStatus.NOT_STARTED);
        } else if (readPages >= totalPages) {
            segment.setStatus(ReadingPlanSegment.SegmentStatus.COMPLETED);
        } else {
            segment.setStatus(ReadingPlanSegment.SegmentStatus.IN_PROGRESS);
        }
    }

    private void syncBookProgress(Long planId) {
        ReadingPlan plan = readingPlanRepository.findById(planId).orElse(null);
        if (plan == null || plan.getBook() == null) {
            return;
        }

        List<ReadingPlanSegment> segments = readingPlanSegmentRepository.findByReadingPlanIdOrderBySortOrderAsc(planId);
        if (segments.isEmpty() || plan.getTotalPages() == null || plan.getTotalPages() <= 0) {
            return;
        }

        int maxCurrentPage = segments.stream()
                .map(ReadingPlanSegment::getCurrentPage)
                .filter(p -> p != null)
                .max(Comparator.naturalOrder())
                .orElse(0);

        int progress = (int) Math.min(100, Math.round((maxCurrentPage * 100.0) / plan.getTotalPages()));

        Book book = plan.getBook();
        book.setProgress(progress);
        bookRepository.save(book);
    }
}

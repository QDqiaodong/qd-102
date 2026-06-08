
package com.example.booknote.controller;

import com.example.booknote.dto.ReadingPlanDTO;
import com.example.booknote.dto.ReadingPlanSegmentDTO;
import com.example.booknote.service.ReadingPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reading-plans")
public class ReadingPlanController {

    @Autowired
    private ReadingPlanService readingPlanService;

    @GetMapping("/book/{bookId}")
    public ResponseEntity<List<ReadingPlanDTO>> getPlansByBookId(@PathVariable Long bookId) {
        List<ReadingPlanDTO> plans = readingPlanService.getPlansByBookId(bookId);
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/book/{bookId}/latest")
    public ResponseEntity<ReadingPlanDTO> getLatestPlanByBookId(@PathVariable Long bookId) {
        return readingPlanService.getLatestPlanByBookId(bookId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{planId}")
    public ResponseEntity<ReadingPlanDTO> getPlanById(@PathVariable Long planId) {
        return readingPlanService.getPlanById(planId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/book/{bookId}")
    public ResponseEntity<ReadingPlanDTO> createPlan(@PathVariable Long bookId, @RequestBody ReadingPlanDTO planDTO) {
        ReadingPlanDTO created = readingPlanService.createPlan(bookId, planDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{planId}")
    public ResponseEntity<ReadingPlanDTO> updatePlan(@PathVariable Long planId, @RequestBody ReadingPlanDTO planDTO) {
        ReadingPlanDTO updated = readingPlanService.updatePlan(planId, planDTO);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{planId}")
    public ResponseEntity<Void> deletePlan(@PathVariable Long planId) {
        readingPlanService.deletePlan(planId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{planId}/segments")
    public ResponseEntity<ReadingPlanSegmentDTO> addSegment(@PathVariable Long planId, @RequestBody ReadingPlanSegmentDTO segmentDTO) {
        ReadingPlanSegmentDTO created = readingPlanService.addSegment(planId, segmentDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/segments/{segmentId}")
    public ResponseEntity<ReadingPlanSegmentDTO> updateSegment(@PathVariable Long segmentId, @RequestBody ReadingPlanSegmentDTO segmentDTO) {
        ReadingPlanSegmentDTO updated = readingPlanService.updateSegment(segmentId, segmentDTO);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/segments/{segmentId}/progress")
    public ResponseEntity<ReadingPlanSegmentDTO> updateSegmentProgress(
            @PathVariable Long segmentId,
            @RequestParam Integer currentPage) {
        ReadingPlanSegmentDTO updated = readingPlanService.updateSegmentProgress(segmentId, currentPage);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/segments/{segmentId}")
    public ResponseEntity<Void> deleteSegment(@PathVariable Long segmentId) {
        readingPlanService.deleteSegment(segmentId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{planId}/segments/reorder")
    public ResponseEntity<ReadingPlanDTO> reorderSegments(
            @PathVariable Long planId,
            @RequestBody List<Long> segmentIds) {
        ReadingPlanDTO updated = readingPlanService.reorderSegments(planId, segmentIds);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/book/{bookId}/generate")
    public ResponseEntity<ReadingPlanDTO> generatePlan(
            @PathVariable Long bookId,
            @RequestParam Integer pagesPerSegment,
            @RequestParam Integer totalPages,
            @RequestParam(required = false) String startDate) {
        ReadingPlanDTO plan = readingPlanService.generatePlanByPageCount(bookId, pagesPerSegment, totalPages, startDate);
        return ResponseEntity.status(HttpStatus.CREATED).body(plan);
    }
}

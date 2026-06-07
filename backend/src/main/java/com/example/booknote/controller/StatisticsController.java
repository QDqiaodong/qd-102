
package com.example.booknote.controller;

import com.example.booknote.dto.statistics.*;
import com.example.booknote.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @GetMapping("/overview")
    public ResponseEntity<ReadingOverviewDTO> getReadingOverview(
            @RequestParam(defaultValue = "12") int months) {
        return ResponseEntity.ok(statisticsService.getReadingOverview(months));
    }

    @GetMapping("/status-distribution")
    public ResponseEntity<List<StatusDistributionDTO>> getStatusDistribution() {
        return ResponseEntity.ok(statisticsService.getStatusDistribution());
    }

    @GetMapping("/category-distribution")
    public ResponseEntity<List<CategoryDistributionDTO>> getCategoryDistribution() {
        return ResponseEntity.ok(statisticsService.getCategoryDistribution());
    }

    @GetMapping("/monthly-note-output")
    public ResponseEntity<List<MonthlyNoteOutputDTO>> getMonthlyNoteOutput(
            @RequestParam(defaultValue = "12") int months) {
        return ResponseEntity.ok(statisticsService.getMonthlyNoteOutput(months));
    }

    @GetMapping("/progress-range")
    public ResponseEntity<List<ProgressRangeDTO>> getProgressRange() {
        return ResponseEntity.ok(statisticsService.getProgressRange());
    }

    @GetMapping("/concept-dictionary")
    public ResponseEntity<List<ConceptTermDTO>> getConceptDictionary(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "2") int minFrequency) {
        return ResponseEntity.ok(statisticsService.getConceptDictionary(limit, minFrequency));
    }
}

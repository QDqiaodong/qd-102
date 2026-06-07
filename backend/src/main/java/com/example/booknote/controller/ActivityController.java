package com.example.booknote.controller;

import com.example.booknote.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    @GetMapping("/heatmap")
    public ResponseEntity<ActivityService.HeatmapData> getHeatmapData(
            @RequestParam(defaultValue = "12") int months) {
        return ResponseEntity.ok(activityService.getHeatmapData(months));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<ActivityService.DateDetail> getDateDetail(@PathVariable String date) {
        LocalDate localDate = LocalDate.parse(date);
        return ResponseEntity.ok(activityService.getDateDetail(localDate));
    }

    @GetMapping("/wake-up-list")
    public ResponseEntity<ActivityService.WakeUpListData> getWakeUpList(
            @RequestParam(defaultValue = "30") int daysThreshold) {
        return ResponseEntity.ok(activityService.getWakeUpList(daysThreshold));
    }
}

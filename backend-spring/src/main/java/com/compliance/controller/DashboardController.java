package com.compliance.controller;

import com.compliance.dto.DashboardDtos.*;
import com.compliance.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard/overview")
    public ResponseEntity<OverviewResponse> overview() {
        return ResponseEntity.ok(dashboardService.getOverview());
    }

    @GetMapping("/dashboard/timeline")
    public ResponseEntity<List<TimelineEntry>> timeline() {
        return ResponseEntity.ok(dashboardService.getTimeline());
    }

    @GetMapping("/departments/{slug}/dashboard")
    public ResponseEntity<DepartmentDashboard> departmentDashboard(@PathVariable String slug) {
        return ResponseEntity.ok(dashboardService.getDepartmentDashboard(slug));
    }
}

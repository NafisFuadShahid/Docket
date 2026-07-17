package com.compliance.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class DashboardDtos {

    @Data
    public static class OverviewResponse {
        private long totalCirculars;
        private long pendingReview;
        private long overdueTasks;
        private long evidenceGaps;
        private long totalObligations;
        private long applicableObligations;
        private long totalTasks;
        private long completedTasks;
        private long unreadAlerts;
        private double complianceScore;
        private ObligationDtos.DashboardStats obligationStats;
        private TaskDtos.DashboardStats taskStats;
    }

    @Data
    public static class TimelineEntry {
        private UUID id;
        private String type;
        private String title;
        private String description;
        private Instant timestamp;
        private String entityType;
        private UUID entityId;
    }

    @Data
    public static class DepartmentDashboard {
        private String department;
        private long totalObligations;
        private long pendingTasks;
        private long overdueTasks;
        private long evidenceGaps;
        private List<ObligationDtos.ObligationResponse> obligations;
        private List<TaskDtos.TaskResponse> tasks;
    }
}

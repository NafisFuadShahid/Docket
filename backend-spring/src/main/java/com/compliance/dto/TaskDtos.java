package com.compliance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class TaskDtos {

    @Data
    public static class TaskResponse {
        private UUID id;
        private UUID tenantId;
        private UUID obligationId;
        private UUID circularId;
        private String obligationTitle;
        private String circularTitle;
        private String title;
        private String description;
        private String taskType;
        private UUID ownerId;
        private String ownerName;
        private String department;
        private Instant dueDate;
        private String priority;
        private String status;
        private Boolean evidenceRequired;
        private String approvalStatus;
        private UUID approvedBy;
        private Instant approvedAt;
        private Instant createdAt;
        private Instant updatedAt;
        private List<CommentResponse> comments;
        private int evidenceCount;
    }

    @Data
    public static class CreateTaskRequest {
        @NotBlank
        private String title;
        private String description;
        @NotBlank
        private String taskType;
        private UUID ownerId;
        @NotBlank
        private String department;
        private Instant dueDate;
        private String priority;
        private Boolean evidenceRequired;
        private UUID obligationId;
        private UUID circularId;
    }

    @Data
    public static class UpdateTaskRequest {
        private String title;
        private String description;
        private UUID ownerId;
        private String department;
        private Instant dueDate;
        private String priority;
        private String status;
        private Boolean evidenceRequired;
    }

    @Data
    public static class CommentRequest {
        @NotBlank
        private String content;
    }

    @Data
    public static class CommentResponse {
        private UUID id;
        private UUID userId;
        private String userName;
        private String content;
        private Instant createdAt;
    }

    @Data
    public static class DashboardStats {
        private long totalTasks;
        private long pending;
        private long inProgress;
        private long completed;
        private long overdue;
        private long blocked;
    }
}

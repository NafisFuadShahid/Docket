package com.compliance.controller;

import com.compliance.model.Alert;
import com.compliance.service.AlertService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ResponseEntity<Page<Alert>> list(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(alertService.listAlerts(pageable));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        return ResponseEntity.ok(Map.of("count", alertService.unreadCount()));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id) {
        alertService.markRead(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead() {
        alertService.markAllRead();
        return ResponseEntity.ok().build();
    }
}

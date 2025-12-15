package io.allure.platform.controller;

import io.allure.platform.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ReportService reportService;

    public AdminController(ReportService reportService) {
        this.reportService = reportService;
    }

    // DELETE RUN
    @DeleteMapping("/run/{runId}")
    public ResponseEntity<?> deleteRun(@PathVariable String runId) throws Exception {
        reportService.deleteRun(runId);
        return ResponseEntity.ok(Map.of("status", "deleted", "runId", runId));
    }

    // DELETE RELEASE
    @DeleteMapping("/release")
    public ResponseEntity<?> deleteRelease(
            @RequestParam String appId,
            @RequestParam String release) throws Exception {

        reportService.deleteRelease(appId, release);
        return ResponseEntity.ok(Map.of("status", "deleted", "appId", appId, "release", release));
    }

    // DELETE APP
    @DeleteMapping("/app/{appId}")
    public ResponseEntity<?> deleteApp(@PathVariable String appId) throws Exception {
        reportService.deleteApp(appId);
        return ResponseEntity.ok(Map.of("status", "deleted", "appId", appId));
    }
}

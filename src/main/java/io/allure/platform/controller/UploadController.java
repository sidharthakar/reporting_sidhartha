package io.allure.platform.controller;

import io.allure.platform.repo.RunRepository;
import io.allure.platform.service.ReportService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UploadController {

    private final ReportService reportService;
    private final RunRepository runRepo;
    private final S3Client s3;
    private final String bucket;
    private final String storageMode;

    public UploadController(ReportService reportService,
                            RunRepository runRepo,
                            S3Client s3,
                            @Value("${aws.s3.bucket}") String bucket,
                            @Value("${storage.mode:local}") String storageMode) {
        this.reportService = reportService;
        this.runRepo = runRepo;
        this.s3 = s3;
        this.bucket = bucket;
        this.storageMode = storageMode;
    }

    @PostMapping(value="/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            @RequestParam String appId,
            @RequestParam String release,
            @RequestParam String executionDate,
            @RequestParam("file") MultipartFile file) {

        if (executionDate == null || executionDate.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Execution date is required"));
        }

        try {
            Map<String, Object> resp =
                    reportService.handleUpload(appId, release, executionDate, file);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }



    @GetMapping("/apps")
    public ResponseEntity<List<String>> apps() {
        return ResponseEntity.ok(runRepo.findApps());
    }

    @GetMapping("/releases")
    public ResponseEntity<List<String>> releases(@RequestParam String appId) {
        return ResponseEntity.ok(runRepo.findReleases(appId));
    }

    @GetMapping("/runs")
    public ResponseEntity<?> runs(@RequestParam String appId, @RequestParam String release) {
        return ResponseEntity.ok(runRepo.findByAppAndRelease(appId, release));
    }

    /**
     * Download artifact.
     * When storage.mode=local, 'key' is expected to be a filesystem path.
     * When storage.mode=s3, 'key' is an S3 key (prefix/.../index.html)
     */
    @GetMapping("/download")
    public ResponseEntity<?> download(@RequestParam String key) {
        try {
            if ("s3".equalsIgnoreCase(storageMode)) {
                GetObjectRequest req = GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build();
                InputStream s3is = s3.getObject(req);
                byte[] data = s3is.readAllBytes();
                String filename = key.substring(key.lastIndexOf('/') + 1);
                return ResponseEntity.ok()
                        .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                        .body(data);
            } else {
                File f = new File(key);
                if (!f.exists()) return ResponseEntity.notFound().build();
                org.springframework.core.io.Resource res = new org.springframework.core.io.FileSystemResource(f);
                return ResponseEntity.ok()
                        .header("Content-Disposition", "attachment; filename=\"" + f.getName() + "\"")
                        .body(res);
            }
        } catch (Exception e) {
            return ResponseEntity.status(404).body("Not found: " + e.getMessage());
        }
    }

    /**
     * View report HTML (proxied). UI should call /api/view?key=<key>
     * key = S3 key in s3 mode or filesystem path in local mode
     */
    @GetMapping("/view")
    public ResponseEntity<?> viewHtml(@RequestParam String key) {
        try {
            if ("s3".equalsIgnoreCase(storageMode)) {
                GetObjectRequest req = GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build();
                InputStream s3is = s3.getObject(req);
                byte[] data = s3is.readAllBytes();
                return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(data);
            } else {
                File f = new File(key);
                if (!f.exists()) return ResponseEntity.notFound().build();
                byte[] data = Files.readAllBytes(f.toPath());
                return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(data);
            }
        } catch (IOException e) {
            return ResponseEntity.status(404).body("HTML Not found: " + e.getMessage());
        }
    }
}

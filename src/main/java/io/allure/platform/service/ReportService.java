package io.allure.platform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.allure.platform.model.RunMeta;
import io.allure.platform.repo.RunRepository;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ReportService {

    private final Path storageRoot;
    private final RunRepository repo;
    private final ObjectMapper mapper = new ObjectMapper();
    private final S3Client s3;
    private final String bucket;
    private final String storageMode;

    public ReportService(@Value("${app.storage.root:storage}") String storageRoot,
                         RunRepository repo,
                         S3Client s3,
                         @Value("${aws.s3.bucket:allure-dashboard-prod}") String bucket,
                         @Value("${storage.mode:local}") String storageMode) {
        this.storageRoot = Paths.get(storageRoot);
        this.repo = repo;
        this.s3 = s3;
        this.bucket = bucket;
        this.storageMode = storageMode;
        try {
            Files.createDirectories(this.storageRoot);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // ---------------- MAIN UPLOAD ----------------
    public Map<String, Object> handleUpload(String appId, String release, String executionDate, MultipartFile file) throws Exception {
        String runId = UUID.randomUUID().toString();

        Path runFolder;
        if ("s3".equalsIgnoreCase(storageMode)) {
            runFolder = Files.createTempDirectory("run_" + runId + "_");
        } else {
            runFolder = storageRoot.resolve(appId).resolve(release).resolve(runId);
            Files.createDirectories(runFolder);
        }

        Path zipPath = runFolder.resolve("upload.zip");
        Files.copy(file.getInputStream(), zipPath, StandardCopyOption.REPLACE_EXISTING);

        Path resultsDir = runFolder.resolve("allure-results");
        Files.createDirectories(resultsDir);
        unzip(zipPath, resultsDir);

        // Trend/historical merge
        prepareHistoryForRun(appId, release, runId, runFolder, resultsDir);

        Map<String, Object> stats = parseAllureResults(resultsDir);

        Path htmlPath = runFolder.resolve("index.html");
        boolean generated = generateFinalReports(runFolder, resultsDir, htmlPath);

        RunMeta meta = new RunMeta();
        meta.setRunId(runId);
        meta.setAppId(appId);
        meta.setExecutionDate(LocalDate.parse(executionDate));
        meta.setRelease(release);
        meta.setTimestamp(LocalDateTime.now());
        meta.setPassed((int) stats.getOrDefault("passed", 0));
        meta.setFailed((int) stats.getOrDefault("failed", 0));
        meta.setBroken((int) stats.getOrDefault("broken", 0));
        meta.setSkipped((int) stats.getOrDefault("skipped", 0));
        meta.setTotal((int) stats.getOrDefault("total", 0));
        meta.setDurationMs((long) stats.getOrDefault("duration", 0L));

        if ("s3".equalsIgnoreCase(storageMode)) {
            String prefix = String.format("reports/%s/%s/%s", appId, release, runId);
            if (Files.exists(htmlPath)) uploadFileToS3(htmlPath, prefix + "/index.html");
            uploadDirectoryToS3(runFolder.resolve("history"), prefix + "/history");
            uploadDirectoryToS3(resultsDir, prefix + "/allure-results");
            uploadFileToS3(zipPath, prefix + "/upload.zip");

            meta.setHtmlPath(prefix + "/index.html");
            meta.setHistoryPath(prefix + "/history");
        } else {
            meta.setHtmlPath(htmlPath.toString());
            meta.setHistoryPath(runFolder.resolve("history").toString());
        }

        repo.save(meta);

        if ("s3".equalsIgnoreCase(storageMode)) {
            try { FileUtils.deleteDirectory(runFolder.toFile()); } catch (IOException ignored) {}
        }

        Map<String, Object> response = new HashMap<>();
        response.put("runId", runId);
        response.put("stats", stats);
        response.put("htmlGenerated", generated);
        response.put("htmlPath", meta.getHtmlPath());
        response.put("storagePath", meta.getHistoryPath());
        return response;
    }

    // ---------------- unzip ----------------
    private void unzip(Path zipPath, Path destDir) throws IOException {
        Path tmpRoot = destDir.resolve("tmp");
        Files.createDirectories(tmpRoot);

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path out = tmpRoot.resolve(entry.getName()).normalize();
                if (!out.startsWith(tmpRoot)) continue;

                if (entry.isDirectory()) Files.createDirectories(out);
                else {
                    Files.createDirectories(out.getParent());
                    Files.copy(zis, out, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        Path real = findAllureResults(tmpRoot);
        if (real == null) throw new RuntimeException("ZIP does not contain allure-results folder");

        FileUtils.copyDirectory(real.toFile(), destDir.toFile());
        FileUtils.deleteDirectory(tmpRoot.toFile());
    }

    private Path findAllureResults(Path root) throws IOException {
        try (Stream<Path> s = Files.walk(root)) {
            return s.filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().equals("allure-results"))
                    .findFirst().orElse(null);
        }
    }

    // ---------------- trend/history merge ----------------
    private void prepareHistoryForRun(String appId, String release, String runId, Path runFolder, Path resultsDir) throws Exception {
        RunMeta previous = repo.findLatestBefore(appId, release, runId);

        Path baseReport = runFolder.resolve("baseline-report");
        FileUtils.deleteDirectory(baseReport.toFile());
        Files.createDirectories(baseReport);

        new ProcessBuilder("allure", "generate", resultsDir.toString(), "--clean", "-o", baseReport.toString())
                .inheritIO().start().waitFor();

        Path baselineHistory = baseReport.resolve("history");
        Path runHistory = runFolder.resolve("history");
        mergeHistory(baselineHistory, runHistory);

        if (previous != null) {
            if ("s3".equalsIgnoreCase(storageMode)) {
                // download entire previous history prefix to a temp folder then merge
                String prevPrefix = previous.getHistoryPath();
                if (prevPrefix.startsWith("/")) prevPrefix = prevPrefix.substring(1);
                Path prevTmp = Files.createTempDirectory("prev_history_");
                downloadS3PrefixToLocal(prevPrefix, prevTmp);
                mergeHistory(prevTmp, runHistory);
                FileUtils.deleteDirectory(prevTmp.toFile());
            } else {
                Path prevHistory = Paths.get(previous.getHistoryPath());
                mergeHistory(prevHistory, runHistory);
            }
        }

        // copy merged history back into resultsDir/history so next generate picks it up
        Path resultsHistory = resultsDir.resolve("history");
        mergeHistory(runHistory, resultsHistory);

        FileUtils.deleteDirectory(baseReport.toFile());
    }

    private void mergeHistory(Path from, Path to) throws IOException {
        if (from == null || !Files.exists(from)) return;
        Files.createDirectories(to);
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(from)) {
            for (Path file : ds) {
                Files.copy(file, to.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    // ---------------- parse allure results ----------------
    private Map<String, Object> parseAllureResults(Path resultsDir) throws IOException {
        int passed = 0, failed = 0, broken = 0, skipped = 0, total = 0;
        long duration = 0;

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(resultsDir, "*-result.json")) {
            for (Path p : ds) {
                JsonNode n = mapper.readTree(p.toFile());
                total++;
                String status = n.has("status") ? n.get("status").asText() : "unknown";
                switch (status) {
                    case "passed": passed++; break;
                    case "failed": failed++; break;
                    case "broken": broken++; break;
                    case "skipped": skipped++; break;
                }
                long start = n.has("start") ? n.get("start").asLong() : 0;
                long stop = n.has("stop") ? n.get("stop").asLong() : 0;
                if (start > 0 && stop > start) duration += (stop - start);
            }
        }

        return Map.of("passed", passed, "failed", failed, "broken", broken, "skipped", skipped, "total", total, "duration", duration);
    }

    // ---------------- generate final reports ----------------
    private boolean generateFinalReports(Path runFolder, Path resultsDir, Path htmlOut) {
        try {
            Path fullReport = runFolder.resolve("full-report");
            Path singleReport = runFolder.resolve("single-report");

            FileUtils.deleteDirectory(fullReport.toFile());
            FileUtils.deleteDirectory(singleReport.toFile());
            Files.createDirectories(fullReport);
            Files.createDirectories(singleReport);

            new ProcessBuilder("allure", "generate", resultsDir.toString(), "--clean", "-o", fullReport.toString())
                    .inheritIO().start().waitFor();

            Path updatedHistory = fullReport.resolve("history");
            Path runHistory = runFolder.resolve("history");
            mergeHistory(updatedHistory, runHistory);

            Path resultsHistory = resultsDir.resolve("history");
            mergeHistory(runHistory, resultsHistory);

            new ProcessBuilder("allure", "generate", resultsDir.toString(), "--clean", "--single-file", "-o", singleReport.toString())
                    .inheritIO().start().waitFor();

            Path singleHtml = singleReport.resolve("index.html");
            if (Files.exists(singleHtml)) {
                Files.move(singleHtml, htmlOut, StandardCopyOption.REPLACE_EXISTING);
                FileUtils.deleteDirectory(fullReport.toFile());
                FileUtils.deleteDirectory(singleReport.toFile());
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // ---------------- S3 helpers ----------------
    private void uploadFileToS3(Path file, String s3Key) throws IOException {
        if (file == null || !Files.exists(file)) return;
        PutObjectRequest putReq = PutObjectRequest.builder().bucket(bucket).key(s3Key).build();
        s3.putObject(putReq, RequestBody.fromFile(file));
    }

    private void uploadDirectoryToS3(Path dir, String s3Prefix) throws IOException {
        if (!Files.exists(dir)) return;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path p : ds) {
                if (Files.isDirectory(p)) {
                    uploadDirectoryToS3(p, s3Prefix + "/" + p.getFileName().toString());
                } else {
                    String key = s3Prefix + "/" + p.getFileName().toString();
                    uploadFileToS3(p, key);
                }
            }
        }
    }

    private void downloadS3PrefixToLocal(String prefix, Path destDir) {
        if (prefix == null || prefix.isEmpty()) return;
        try {
            ListObjectsV2Request listReq = ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build();
            ListObjectsV2Response listRes;
            String token = null;
            do {
                ListObjectsV2Request req = (token == null) ? listReq : listReq.toBuilder().continuationToken(token).build();
                listRes = s3.listObjectsV2(req);
                for (S3Object obj : listRes.contents()) {
                    String key = obj.key();
                    // Get filename after last '/'
                    String name = key.substring(key.lastIndexOf('/') + 1);
                    if (name.isEmpty()) continue;
                    GetObjectRequest getReq = GetObjectRequest.builder().bucket(bucket).key(key).build();
                    try (InputStream is = s3.getObject(getReq)) {
                        Path out = destDir.resolve(name);
                        Files.copy(is, out, StandardCopyOption.REPLACE_EXISTING);
                    } catch (Exception ignored) {}
                }
                token = listRes.nextContinuationToken();
            } while (token != null);
        } catch (Exception e) {
            // ignore on failure, trend may still work with partial files
        }
    }

    /*Delete Data */

    public void deleteRun(String runId) throws IOException {
        RunMeta meta = repo.findByRunId(runId);
        if (meta == null) return;

        deleteStorage(meta.getHtmlPath(), meta.getHistoryPath());
        repo.deleteByRunId(runId);
    }

    public void deleteRelease(String appId, String release) throws IOException {
        List<RunMeta> runs = repo.findAllByAppAndRelease(appId, release);
        for (RunMeta r : runs) {
            deleteStorage(r.getHtmlPath(), r.getHistoryPath());
        }
        repo.deleteByAppAndRelease(appId, release);
    }
    private void deleteStorage(String htmlPath, String historyPath) throws IOException {
        if ("s3".equalsIgnoreCase(storageMode)) {
            deleteS3Prefix(parentPrefix(htmlPath));
        } else {
            deleteLocalPath(htmlPath);
            deleteLocalPath(historyPath);
        }
    }
    private void deleteLocalPath(String path) throws IOException {
        if (path == null) return;
        File f = new File(path);
        if (!f.exists()) return;

        if (f.isDirectory()) FileUtils.deleteDirectory(f);
        else f.delete();
    }
    private void deleteS3Prefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) return;

        ListObjectsV2Request listReq =
                ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build();

        ListObjectsV2Response res;
        String token = null;

        do {
            res = s3.listObjectsV2(
                    token == null ? listReq : listReq.toBuilder().continuationToken(token).build()
            );

            for (S3Object obj : res.contents()) {
                s3.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(obj.key())
                        .build());
            }

            token = res.nextContinuationToken();
        } while (token != null);
    }

    private String parentPrefix(String key) {
        return key.substring(0, key.lastIndexOf('/'));
    }

    public void deleteApp(String appId) throws IOException {
        List<RunMeta> runs = repo.findAllByApp(appId);
        for (RunMeta r : runs) {
            deleteStorage(r.getHtmlPath(), r.getHistoryPath());
        }
        repo.deleteByApp(appId);
    }


}

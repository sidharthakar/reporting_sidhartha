package io.allure.platform.model;

import java.time.LocalDateTime;

public class RunMeta {
    private String runId;
    private String appId;
    private String release;
    private LocalDateTime timestamp;
    private int passed;
    private int failed;
    private int broken;
    private int skipped;
    private int total;
    private long durationMs;
    private String htmlPath;
    private String historyPath;

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getRelease() { return release; }
    public void setRelease(String release) { this.release = release; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public int getPassed() { return passed; }
    public void setPassed(int passed) { this.passed = passed; }
    public int getFailed() { return failed; }
    public void setFailed(int failed) { this.failed = failed; }
    public int getBroken() { return broken; }
    public void setBroken(int broken) { this.broken = broken; }
    public int getSkipped() { return skipped; }
    public void setSkipped(int skipped) { this.skipped = skipped; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    public String getHtmlPath() { return htmlPath; }
    public void setHtmlPath(String htmlPath) { this.htmlPath = htmlPath; }
    public String getHistoryPath() { return historyPath; }
    public void setHistoryPath(String historyPath) { this.historyPath = historyPath; }
}

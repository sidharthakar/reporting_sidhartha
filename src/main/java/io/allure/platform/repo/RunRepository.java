package io.allure.platform.repo;

import io.allure.platform.model.RunMeta;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class RunRepository {
    private final JdbcTemplate jdbc;

    public RunRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        init();
    }

    private void init() {
        jdbc.execute("""
    CREATE TABLE IF NOT EXISTS runs (
        run_id TEXT PRIMARY KEY,
        app_id TEXT,
        release TEXT,
        execution_date TEXT NOT NULL,
        timestamp TEXT,
        passed INTEGER,
        failed INTEGER,
        broken INTEGER,
        skipped INTEGER,
        total INTEGER,
        duration_ms INTEGER,
        html_path TEXT,
        history_path TEXT
    )
    """);;
    }

    public void save(RunMeta r) {
        jdbc.update("""
    INSERT INTO runs(
        run_id, app_id, release, execution_date, timestamp,
        passed, failed, broken, skipped, total,
        duration_ms, html_path, history_path
    )
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """,
                r.getRunId(),
                r.getAppId(),
                r.getRelease(),
                r.getExecutionDate().toString(),   // ✅ FIXED
                r.getTimestamp().toString(),
                r.getPassed(),
                r.getFailed(),
                r.getBroken(),
                r.getSkipped(),
                r.getTotal(),
                r.getDurationMs(),
                r.getHtmlPath(),
                r.getHistoryPath()
        );
    }


    public List<RunMeta> findByAppAndRelease(String appId, String release) {
        return jdbc.query(
                "SELECT * FROM runs WHERE app_id=? AND release=? ORDER BY timestamp DESC",
                (rs, i) -> map(rs),
                appId, release
        );
    }

    public List<String> findApps() {
        return jdbc.queryForList(
                "SELECT DISTINCT app_id FROM runs ORDER BY app_id",
                String.class
        );
    }

    public List<String> findReleases(String appId) {
        return jdbc.queryForList(
                "SELECT DISTINCT release FROM runs WHERE app_id=? ORDER BY release",
                String.class, appId
        );
    }

    private RunMeta map(ResultSet rs) throws SQLException {
        RunMeta r = new RunMeta();
        r.setRunId(rs.getString("run_id"));
        r.setAppId(rs.getString("app_id"));
        r.setRelease(rs.getString("release"));
        r.setExecutionDate(LocalDate.parse(rs.getString("execution_date")));
        r.setTimestamp(LocalDateTime.parse(rs.getString("timestamp")));
        r.setPassed(rs.getInt("passed"));
        r.setFailed(rs.getInt("failed"));
        r.setBroken(rs.getInt("broken"));
        r.setSkipped(rs.getInt("skipped"));
        r.setTotal(rs.getInt("total"));
        r.setDurationMs(rs.getLong("duration_ms"));
        r.setHtmlPath(rs.getString("html_path"));
        r.setHistoryPath(rs.getString("history_path"));
        return r;
    }

    public RunMeta findLatestBefore(String appId, String release, String excludeRunId) {
        String sql = "SELECT * FROM runs WHERE app_id = ? AND release = ? AND run_id <> ? ORDER BY timestamp DESC LIMIT 1";
        List<RunMeta> list = jdbc.query(sql, new Object[]{ appId, release, excludeRunId }, (rs, rowNum) -> map(rs));
        return list.isEmpty() ? null : list.get(0);
    }
    // Delete single run
    public void deleteByRunId(String runId) {
        jdbc.update("DELETE FROM runs WHERE run_id = ?", runId);
    }

    // Delete all runs for a release
    public void deleteByAppAndRelease(String appId, String release) {
        jdbc.update("DELETE FROM runs WHERE app_id = ? AND release = ?", appId, release);
    }

    // Delete all runs for an app
    public void deleteByApp(String appId) {
        jdbc.update("DELETE FROM runs WHERE app_id = ?", appId);
    }

    // Fetch runs for cleanup
    public List<RunMeta> findAllByApp(String appId) {
        return jdbc.query("SELECT * FROM runs WHERE app_id = ?", (rs, i) -> map(rs), appId);
    }

    public List<RunMeta> findAllByAppAndRelease(String appId, String release) {
        return jdbc.query("SELECT * FROM runs WHERE app_id = ? AND release = ?", (rs, i) -> map(rs), appId, release);
    }
    public RunMeta findByRunId(String runId) {
        List<RunMeta> list = jdbc.query(
                "SELECT * FROM runs WHERE run_id=?",
                (rs, i) -> map(rs),
                runId
        );
        return list.isEmpty() ? null : list.get(0);
    }

    // ================= APP LEVEL =================
    public List<Map<String, Object>> appOverview(String appId, LocalDate from, LocalDate to) {

        return jdbc.queryForList("""
        SELECT release,
               COUNT(*) AS runs,
               AVG(passed * 100.0 / total) AS passPercent,
               SUM(failed) AS failed,
               SUM(broken) AS broken,
               SUM(skipped) AS skipped
        FROM runs
        WHERE app_id = ?
          AND execution_date BETWEEN ? AND ?
        GROUP BY release
        ORDER BY release
        """, appId, from.toString(), to.toString());
    }

    // ================= RELEASE LEVEL =================
    public List<Map<String, Object>> releaseTrend(String appId, String release, LocalDate from, LocalDate to) {

        return jdbc.queryForList("""
        SELECT execution_date,
               passed, failed, broken, skipped, total, duration_ms
        FROM runs
        WHERE app_id = ?
          AND release = ?
          AND execution_date BETWEEN ? AND ?
        ORDER BY execution_date
        """, appId, release, from.toString(), to.toString());
    }


}

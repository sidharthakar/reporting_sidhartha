package io.allure.platform.controller;


import io.allure.platform.repo.RunRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/charts")
public class ChartController {

    private final RunRepository repo;

    public ChartController(RunRepository repo) {
        this.repo = repo;
    }

    // ================= APP LEVEL =================
    @GetMapping("/app")
    public List<Map<String, Object>> appOverview(
            @RequestParam String appId,
            @RequestParam String from,
            @RequestParam String to) {

        return repo.appOverview(
                appId,
                LocalDate.parse(from),
                LocalDate.parse(to)
        );
    }

    // ================= RELEASE LEVEL =================
    @GetMapping("/release")
    public List<Map<String, Object>> releaseTrend(
            @RequestParam String appId,
            @RequestParam String release,
            @RequestParam String from,
            @RequestParam String to) {

        return repo.releaseTrend(
                appId,
                release,
                LocalDate.parse(from),
                LocalDate.parse(to)
        );
    }

}

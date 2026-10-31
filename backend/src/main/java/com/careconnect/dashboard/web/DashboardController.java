package com.careconnect.dashboard.web;

import com.careconnect.dashboard.dto.DashboardDtos.DashboardDto;
import com.careconnect.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard")
@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Stat cards, charts and work list for the logged-in user's role")
    @GetMapping("/api/dashboard")
    public DashboardDto dashboard() {
        return dashboardService.forCurrentUser();
    }
}

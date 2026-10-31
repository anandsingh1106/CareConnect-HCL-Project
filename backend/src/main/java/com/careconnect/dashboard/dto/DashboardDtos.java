package com.careconnect.dashboard.dto;

import java.util.List;

/**
 * Generic dashboard payload so the Angular app can render any role's dashboard with the same components:
 * stat cards, charts (bar / line / doughnut) and a short work list.
 */
public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record DashboardDto(String role, List<StatCard> cards, List<ChartData> charts, WorkList workList) {
    }

    /** tone: primary | success | warning | danger | info - picked up by the UI for colour. */
    public record StatCard(String key, String label, long value, String icon, String tone, String link) {
    }

    public record ChartData(String key, String title, String type, List<String> labels, List<Series> series) {
    }

    public record Series(String name, List<Long> data) {
    }

    public record WorkList(String title, List<WorkItem> items) {
    }

    public record WorkItem(String title, String subtitle, String meta, String link) {
    }
}

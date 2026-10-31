package com.careconnect.clinical.web;

import com.careconnect.clinical.service.ChartService;
import com.careconnect.clinical.service.ChartService.ChartSummaryDto;
import com.careconnect.security.Authz;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Patient chart")
@RestController
@RequiredArgsConstructor
public class ChartController {

    private final ChartService chartService;

    @Operation(summary = "Chart header: demographics, active allergies, problems, medicines, latest vitals, open encounters")
    @GetMapping("/api/patients/{patientId}/summary")
    @PreAuthorize(Authz.CLINICAL_READ)
    public ChartSummaryDto summary(@PathVariable Long patientId) {
        return chartService.summary(patientId);
    }
}

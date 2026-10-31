package com.careconnect.order.web;

import com.careconnect.common.dto.PageResponse;
import com.careconnect.order.dto.OrderDtos.CancelRequest;
import com.careconnect.order.dto.OrderDtos.CdsCheckRequest;
import com.careconnect.order.dto.OrderDtos.CdsResult;
import com.careconnect.order.dto.OrderDtos.LabResultDto;
import com.careconnect.order.dto.OrderDtos.LabTestDto;
import com.careconnect.order.dto.OrderDtos.LabTestRequest;
import com.careconnect.order.dto.OrderDtos.OrderDetailDto;
import com.careconnect.order.dto.OrderDtos.OrderDto;
import com.careconnect.order.dto.OrderDtos.OrderRequest;
import com.careconnect.order.dto.OrderDtos.PlaceOrderRequest;
import com.careconnect.order.dto.OrderDtos.ResultRequest;
import com.careconnect.order.dto.OrderDtos.StatusRequest;
import com.careconnect.order.model.OrderEnums.OrderStatus;
import com.careconnect.order.model.OrderEnums.OrderType;
import com.careconnect.order.service.LabTestService;
import com.careconnect.order.service.OrderService;
import com.careconnect.security.Authz;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "CPOE - Orders")
@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final LabTestService labTestService;

    @Operation(summary = "Run decision support without saving (allergy, interaction, duplicate checks)")
    @PostMapping("/api/orders/check")
    @PreAuthorize(Authz.DOCTOR)
    public CdsResult check(@Valid @RequestBody CdsCheckRequest request) {
        return orderService.check(request);
    }

    @Operation(summary = "Create an order. Returns 409 CDS_ALERTS (with alerts in 'details') when an override reason is needed")
    @PostMapping("/api/orders")
    @PreAuthorize(Authz.DOCTOR)
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDetailDto create(@Valid @RequestBody OrderRequest request) {
        return orderService.create(request);
    }

    @Operation(summary = "Place a DRAFT order (runs decision support)")
    @PostMapping("/api/orders/{id}/place")
    @PreAuthorize(Authz.DOCTOR)
    public OrderDetailDto place(@PathVariable Long id, @Valid @RequestBody(required = false) PlaceOrderRequest request) {
        return orderService.place(id, request == null ? null : request.overrideReason());
    }

    @Operation(summary = "Order worklist with filters (paged)")
    @GetMapping("/api/orders")
    @PreAuthorize(Authz.CLINICAL_READ)
    public PageResponse<OrderDto> search(@RequestParam(required = false) OrderStatus status,
                                         @RequestParam(required = false) OrderType type,
                                         @RequestParam(required = false) Long patientId,
                                         @RequestParam(defaultValue = "false") boolean mine,
                                         @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return orderService.search(status, type, patientId, mine, pageable);
    }

    @GetMapping("/api/orders/{id}")
    @PreAuthorize(Authz.CLINICAL_READ)
    public OrderDetailDto detail(@PathVariable Long id) {
        return orderService.detail(id);
    }

    @Operation(summary = "Move a lab/imaging/referral order forward (e.g. PLACED -> IN_PROGRESS when the sample is collected)")
    @PatchMapping("/api/orders/{id}/status")
    @PreAuthorize(Authz.DOCTOR_OR_NURSE)
    public OrderDetailDto updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return orderService.updateStatus(id, request.status());
    }

    @PostMapping("/api/orders/{id}/cancel")
    @PreAuthorize(Authz.DOCTOR)
    public OrderDetailDto cancel(@PathVariable Long id, @Valid @RequestBody CancelRequest request) {
        return orderService.cancel(id, request.reason());
    }

    @Operation(summary = "Enter a lab value or imaging report; the value is flagged against normal/critical ranges")
    @PostMapping("/api/orders/{id}/results")
    @PreAuthorize(Authz.DOCTOR_OR_NURSE)
    @ResponseStatus(HttpStatus.CREATED)
    public LabResultDto enterResult(@PathVariable Long id, @Valid @RequestBody ResultRequest request) {
        return orderService.enterResult(id, request);
    }

    @GetMapping("/api/patients/{patientId}/orders")
    @PreAuthorize(Authz.CLINICAL_READ)
    public List<OrderDto> forPatient(@PathVariable Long patientId) {
        return orderService.forPatient(patientId);
    }

    @GetMapping("/api/encounters/{encounterId}/orders")
    @PreAuthorize(Authz.CLINICAL_READ)
    public List<OrderDto> forEncounter(@PathVariable Long encounterId) {
        return orderService.forEncounter(encounterId);
    }

    @GetMapping("/api/patients/{patientId}/lab-results")
    @PreAuthorize(Authz.CLINICAL_READ)
    public List<LabResultDto> labResults(@PathVariable Long patientId) {
        return orderService.resultsForPatient(patientId);
    }

    // ----- lab test catalogue -----

    @GetMapping("/api/lab-tests")
    @PreAuthorize(Authz.MASTER_READ)
    public List<LabTestDto> labTests(@RequestParam(defaultValue = "true") boolean activeOnly) {
        return labTestService.list(activeOnly);
    }

    @PostMapping("/api/lab-tests")
    @PreAuthorize(Authz.ADMIN)
    @ResponseStatus(HttpStatus.CREATED)
    public LabTestDto createLabTest(@Valid @RequestBody LabTestRequest request) {
        return labTestService.create(request);
    }

    @PutMapping("/api/lab-tests/{id}")
    @PreAuthorize(Authz.ADMIN)
    public LabTestDto updateLabTest(@PathVariable Long id, @Valid @RequestBody LabTestRequest request) {
        return labTestService.update(id, request);
    }
}

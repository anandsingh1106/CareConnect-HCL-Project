package com.careconnect.medication.web;

import com.careconnect.common.dto.PageResponse;
import com.careconnect.medication.dto.MedicationDtos.DrugDto;
import com.careconnect.medication.dto.MedicationDtos.DrugRequest;
import com.careconnect.medication.dto.MedicationDtos.InteractionDto;
import com.careconnect.medication.dto.MedicationDtos.InteractionRequest;
import com.careconnect.medication.dto.MedicationDtos.InventoryTransactionDto;
import com.careconnect.medication.dto.MedicationDtos.StockAdjustmentRequest;
import com.careconnect.medication.service.DrugService;
import com.careconnect.security.Authz;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Drug master and inventory")
@RestController
@RequiredArgsConstructor
public class DrugController {

    private final DrugService drugService;

    @GetMapping("/api/drugs")
    @PreAuthorize(Authz.DRUG_READ)
    public PageResponse<DrugDto> search(@RequestParam(required = false) String q,
                                        @RequestParam(defaultValue = "true") boolean activeOnly,
                                        @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return drugService.search(q, activeOnly, pageable);
    }

    @GetMapping("/api/drugs/{id}")
    @PreAuthorize(Authz.DRUG_READ)
    public DrugDto get(@PathVariable Long id) {
        return drugService.get(id);
    }

    @Operation(summary = "Drugs at or below their reorder level")
    @GetMapping("/api/drugs/low-stock")
    @PreAuthorize(Authz.DRUG_MANAGE)
    public List<DrugDto> lowStock() {
        return drugService.lowStock();
    }

    @PostMapping("/api/drugs")
    @PreAuthorize(Authz.DRUG_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    public DrugDto create(@Valid @RequestBody DrugRequest request) {
        return drugService.create(request);
    }

    @PutMapping("/api/drugs/{id}")
    @PreAuthorize(Authz.DRUG_MANAGE)
    public DrugDto update(@PathVariable Long id, @Valid @RequestBody DrugRequest request) {
        return drugService.update(id, request);
    }

    @Operation(summary = "Receive stock or adjust it (writes an inventory ledger entry)")
    @PostMapping("/api/drugs/{id}/stock")
    @PreAuthorize(Authz.DRUG_MANAGE)
    public DrugDto adjustStock(@PathVariable Long id, @Valid @RequestBody StockAdjustmentRequest request) {
        return drugService.adjustStock(id, request);
    }

    @GetMapping("/api/drugs/{id}/transactions")
    @PreAuthorize(Authz.DRUG_MANAGE)
    public List<InventoryTransactionDto> transactions(@PathVariable Long id) {
        return drugService.transactions(id);
    }

    @GetMapping("/api/drug-interactions")
    @PreAuthorize(Authz.DRUG_READ)
    public List<InteractionDto> interactions(@RequestParam(required = false) Long drugId) {
        return drugService.interactions(drugId);
    }

    @PostMapping("/api/drug-interactions")
    @PreAuthorize(Authz.DRUG_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    public InteractionDto addInteraction(@Valid @RequestBody InteractionRequest request) {
        return drugService.addInteraction(request);
    }

    @DeleteMapping("/api/drug-interactions/{id}")
    @PreAuthorize(Authz.ADMIN)
    public ResponseEntity<Void> deleteInteraction(@PathVariable Long id) {
        drugService.deleteInteraction(id);
        return ResponseEntity.noContent().build();
    }
}

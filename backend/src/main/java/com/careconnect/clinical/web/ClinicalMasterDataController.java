package com.careconnect.clinical.web;

import com.careconnect.clinical.dto.ClinicalDtos.Icd10Dto;
import com.careconnect.clinical.dto.ClinicalDtos.Icd10Request;
import com.careconnect.clinical.dto.ClinicalDtos.NoteTemplateDto;
import com.careconnect.clinical.service.ClinicalMasterDataService;
import com.careconnect.common.dto.PageResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Master data - ICD-10 and note templates")
@RestController
@RequiredArgsConstructor
public class ClinicalMasterDataController {

    private final ClinicalMasterDataService service;

    @Operation(summary = "Search ICD-10 codes by code prefix or description")
    @GetMapping("/api/icd10")
    @PreAuthorize(Authz.MASTER_READ)
    public PageResponse<Icd10Dto> search(@RequestParam(required = false) String q,
                                         @RequestParam(defaultValue = "true") boolean activeOnly,
                                         @PageableDefault(size = 20, sort = "code", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.searchIcd10(q, activeOnly, pageable);
    }

    @PostMapping("/api/icd10")
    @PreAuthorize(Authz.ADMIN)
    @ResponseStatus(HttpStatus.CREATED)
    public Icd10Dto create(@Valid @RequestBody Icd10Request request) {
        return service.createIcd10(request);
    }

    @PutMapping("/api/icd10/{code}")
    @PreAuthorize(Authz.ADMIN)
    public Icd10Dto update(@PathVariable String code, @Valid @RequestBody Icd10Request request) {
        return service.updateIcd10(code, request);
    }

    @GetMapping("/api/note-templates")
    @PreAuthorize(Authz.MASTER_READ)
    public List<NoteTemplateDto> templates() {
        return service.templates();
    }
}

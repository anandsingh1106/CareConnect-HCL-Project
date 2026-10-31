package com.careconnect.clinical.service;

import com.careconnect.clinical.dto.ClinicalDtos.Icd10Dto;
import com.careconnect.clinical.dto.ClinicalDtos.Icd10Request;
import com.careconnect.clinical.dto.ClinicalDtos.NoteTemplateDto;
import com.careconnect.clinical.mapper.ClinicalMapper;
import com.careconnect.clinical.model.Icd10Code;
import com.careconnect.clinical.repository.Icd10Repository;
import com.careconnect.clinical.repository.NoteTemplateRepository;
import com.careconnect.common.dto.PageResponse;
import com.careconnect.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** ICD-10 code table and SOAP note templates. */
@Service
@RequiredArgsConstructor
public class ClinicalMasterDataService {

    private final Icd10Repository icd10;
    private final NoteTemplateRepository templates;
    private final ClinicalMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<Icd10Dto> searchIcd10(String q, boolean activeOnly, Pageable pageable) {
        String term = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase() + "%";
        String prefix = term == null ? null : q.trim().toUpperCase() + "%";
        return PageResponse.of(icd10.search(term, prefix, activeOnly ? Boolean.TRUE : null, pageable), mapper::toDto);
    }

    @Transactional
    public Icd10Dto createIcd10(Icd10Request request) {
        if (icd10.existsById(request.code())) {
            throw ApiException.conflict("CODE_EXISTS", "ICD-10 code " + request.code() + " already exists.");
        }
        Icd10Code code = new Icd10Code();
        code.setCode(request.code());
        apply(code, request);
        return mapper.toDto(icd10.save(code));
    }

    @Transactional
    public Icd10Dto updateIcd10(String codeValue, Icd10Request request) {
        Icd10Code code = icd10.findById(codeValue).orElseThrow(() -> ApiException.notFound("ICD-10 code", codeValue));
        apply(code, request);
        return mapper.toDto(code);
    }

    private void apply(Icd10Code code, Icd10Request request) {
        code.setDescription(request.description().trim());
        code.setCategory(request.category());
        code.setActive(request.active());
    }

    @Transactional(readOnly = true)
    public List<NoteTemplateDto> templates() {
        return templates.findByActiveTrueOrderByNameAsc().stream().map(mapper::toDto).toList();
    }
}

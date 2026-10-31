package com.careconnect.order.service;

import com.careconnect.common.exception.ApiException;
import com.careconnect.order.dto.OrderDtos.LabTestDto;
import com.careconnect.order.dto.OrderDtos.LabTestRequest;
import com.careconnect.order.mapper.OrderMapper;
import com.careconnect.order.model.LabTest;
import com.careconnect.order.repository.LabTestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Lab test catalogue maintained by the administrator. */
@Service
@RequiredArgsConstructor
public class LabTestService {

    private final LabTestRepository labTests;
    private final OrderMapper mapper;

    @Transactional(readOnly = true)
    public List<LabTestDto> list(boolean activeOnly) {
        List<LabTest> list = activeOnly ? labTests.findByActiveTrueOrderByNameAsc() : labTests.findAllByOrderByNameAsc();
        return list.stream().map(mapper::toDto).toList();
    }

    @Transactional
    public LabTestDto create(LabTestRequest request) {
        if (labTests.existsByCodeIgnoreCase(request.code())) {
            throw ApiException.conflict("CODE_EXISTS", "Lab test code " + request.code() + " already exists.");
        }
        LabTest test = new LabTest();
        apply(test, request);
        return mapper.toDto(labTests.save(test));
    }

    @Transactional
    public LabTestDto update(Long id, LabTestRequest request) {
        LabTest test = labTests.findById(id).orElseThrow(() -> ApiException.notFound("Lab test", id));
        if (!test.getCode().equalsIgnoreCase(request.code()) && labTests.existsByCodeIgnoreCase(request.code())) {
            throw ApiException.conflict("CODE_EXISTS", "Lab test code " + request.code() + " already exists.");
        }
        apply(test, request);
        return mapper.toDto(test);
    }

    private void apply(LabTest test, LabTestRequest request) {
        if (request.normalLow() != null && request.normalHigh() != null
                && request.normalLow().compareTo(request.normalHigh()) > 0) {
            throw ApiException.badRequest("Normal low must not be above normal high.");
        }
        test.setCode(request.code().trim().toUpperCase());
        test.setName(request.name().trim());
        test.setCategory(request.category());
        test.setSpecimen(request.specimen());
        test.setUnit(request.unit());
        test.setNormalLow(request.normalLow());
        test.setNormalHigh(request.normalHigh());
        test.setCriticalLow(request.criticalLow());
        test.setCriticalHigh(request.criticalHigh());
        test.setTurnaroundHours(request.turnaroundHours());
        test.setActive(request.active());
    }
}

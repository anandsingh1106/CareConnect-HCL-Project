package com.careconnect.order.mapper;

import com.careconnect.order.dto.OrderDtos.LabResultDto;
import com.careconnect.order.dto.OrderDtos.LabTestDto;
import com.careconnect.order.dto.OrderDtos.OrderAlertDto;
import com.careconnect.order.dto.OrderDtos.OrderDto;
import com.careconnect.order.model.ClinicalOrder;
import com.careconnect.order.model.LabResult;
import com.careconnect.order.model.LabTest;
import com.careconnect.order.model.OrderAlert;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface OrderMapper {

    @Mapping(target = "patientId", source = "order.patient.id")
    @Mapping(target = "patientName", expression = "java(order.getPatient().getFullName())")
    @Mapping(target = "patientMrn", source = "order.patient.mrn")
    @Mapping(target = "encounterId", source = "order.encounter.id")
    @Mapping(target = "orderedById", source = "order.orderedBy.id")
    @Mapping(target = "orderedByName", source = "order.orderedBy.fullName")
    @Mapping(target = "labTestId", source = "order.labTest.id")
    @Mapping(target = "labTestName", source = "order.labTest.name")
    @Mapping(target = "summary", source = "summary")
    OrderDto toDto(ClinicalOrder order, String summary);

    OrderAlertDto toDto(OrderAlert alert);

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "orderNo", source = "order.orderNo")
    @Mapping(target = "orderType", source = "order.orderType")
    @Mapping(target = "patientId", source = "order.patient.id")
    @Mapping(target = "labTestId", source = "labTest.id")
    @Mapping(target = "labTestCode", source = "labTest.code")
    @Mapping(target = "testName", expression = "java(result.getLabTest() != null ? result.getLabTest().getName() : result.getOrder().getImagingStudy())")
    @Mapping(target = "resultedByName", source = "resultedBy.fullName")
    LabResultDto toDto(LabResult result);

    LabTestDto toDto(LabTest labTest);
}

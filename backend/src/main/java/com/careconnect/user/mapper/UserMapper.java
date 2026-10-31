package com.careconnect.user.mapper;

import com.careconnect.user.dto.UserDtos.DepartmentDto;
import com.careconnect.user.dto.UserDtos.ProviderDto;
import com.careconnect.user.dto.UserDtos.UserDto;
import com.careconnect.user.model.AppUser;
import com.careconnect.user.model.Department;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface UserMapper {

    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "departmentName", source = "department.name")
    @Mapping(target = "patientId", source = "patient.id")
    UserDto toDto(AppUser user);

    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "departmentName", source = "department.name")
    ProviderDto toProvider(AppUser user);

    DepartmentDto toDto(Department department);
}

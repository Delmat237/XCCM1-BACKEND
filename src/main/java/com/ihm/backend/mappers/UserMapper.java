package com.ihm.backend.mappers;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.ihm.backend.DTO.UserDto;
import com.ihm.backend.entities.User;


@Mapper(componentModel = "spring")
public interface UserMapper {

    

    UserDto toDto(User user);

   
   
    User toEntity(UserDto dto);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "firstName")
    @Mapping(target = "lastName")
    @Mapping(target = "email")
    @Mapping(target = "role")
    @Mapping(target = "photoUrl")
    @Mapping(target = "isActive")
    void updateUserFromDto(UserDto dto, @MappingTarget User user);
}

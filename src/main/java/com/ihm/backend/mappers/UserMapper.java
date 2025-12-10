package com.ihm.backend.mappers;

import java.util.UUID; // Importez UUID
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ObjectFactory; // Import NÉCESSAIRE
import org.mapstruct.ReportingPolicy;
import org.mapstruct.TargetType; // Import NÉCESSAIRE

import com.ihm.backend.dto.UserDto;
import com.ihm.backend.entity.User;
import com.ihm.backend.entity.Student; // Importez la sous-classe
import com.ihm.backend.entity.Teacher; // Importez la sous-classe
// Assurez-vous d'importer UserRole si le DTO utilise ce type
// import com.ihm.backend.enums.UserRole; // A vérifier si nécessaire pour le DTO

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    // --- Fixes précédentes (UUID -> Integer) ---
    default Integer map(UUID uuid) {
        return uuid != null ? uuid.hashCode() : null; 
    }
    default UUID map(Integer integer) {
        return integer != null ? UUID.nameUUIDFromBytes(String.valueOf(integer).getBytes()) : null;
    }
    // ---------------------------------------------


    UserDto toDto(User user);
   
    // Cette méthode utilise l'ObjectFactory pour déterminer la classe concrète à instancier.
    User toEntity(UserDto dto); 

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "firstName")
    @Mapping(target = "lastName")
    @Mapping(target = "email")
    @Mapping(target = "role")
    @Mapping(target = "photoUrl")
    @Mapping(source = "isActive", target = "active") 
    void updateUserFromDto(UserDto dto, @MappingTarget User user);

    
    // NOUVELLE MÉTHODE DE FABRIQUE (OBJECT FACTORY)
    // C'est la solution à l'erreur "The return type User is an abstract class"
    @ObjectFactory
    default User createConcreteUser(UserDto dto, @TargetType Class<?> type) {
        if (dto == null || dto.getRole() == null) {
            throw new IllegalArgumentException("User role is required in DTO for mapping to a concrete User entity.");
        }

        switch (dto.getRole()) {
            case STUDENT:
                return new Student();
            case TEACHER:
                return new Teacher();
            default:
                // S'il y a un rôle non géré
                throw new UnsupportedOperationException("Unsupported user role for entity creation: " + dto.getRole());
        }
    }
}
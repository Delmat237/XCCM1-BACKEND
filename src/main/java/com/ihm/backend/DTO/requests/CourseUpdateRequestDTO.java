package com.ihm.backend.DTO.requests;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CourseUpdateRequestDTO {
    private String title;
    private String category;
    private String description;
    private String content;
    
}

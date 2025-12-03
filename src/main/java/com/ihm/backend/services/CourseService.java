package com.ihm.backend.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ihm.backend.DTO.responses.CourseResponse;
import com.ihm.backend.DTO.requests.CourseCreateRequestDTO;
import com.ihm.backend.DTO.requests.CourseUpdateRequestDTO;
import com.ihm.backend.mappers.CourseMapper;
import com.ihm.backend.repositories.CourseRepository;
import com.ihm.backend.repositories.UserRepository;
import com.ihm.backend.entities.*;
import com.ihm.backend.enums.CourseStatus;
@Service
public class CourseService {
    @Autowired
    private  CourseMapper courseMapper;
    @Autowired
    private  CourseRepository courseRepository;
    @Autowired
    private UserRepository userRepository;
    //create a course
   public CourseResponse createCourse(CourseCreateRequestDTO dto,Integer authorId) throws Exception{
    Course course=courseMapper.toEntity(dto);
    User author=userRepository.findById(authorId).orElseThrow(()->new Exception("Teacher does not exists"));
    course.setAuthor(author);
    course =courseRepository.save(course);
    return courseMapper.toResponse(course);
   }
   //get all courses for a particular author

   public List<CourseResponse> getAllCoursesForTeacher(Integer authorId)throws Exception{
    User author=userRepository.findById(authorId).orElseThrow(()->new Exception("Teacher does not exists"));
    return courseMapper.toResponse(courseRepository.findByAuthor(author));
   }
   //update course
   public CourseResponse updateCourse(Integer courseId,CourseUpdateRequestDTO request) throws Exception{
    Course course=courseRepository.findById(courseId)
                    .orElseThrow(()->new Exception("Course does not exist"));

    courseMapper.updateEntityFromDto(request, course);
    course=courseRepository.save(course);
    return courseMapper.toResponse(course);


   }

   //get all courses
   public List<CourseResponse> getAllCourses(){
    return courseMapper.toResponse(courseRepository.findAll());
   }
   //delete course
   public void deleteCourse(Integer courseId) throws Exception{
    Course course=courseRepository.findById(courseId).orElseThrow(()->new Exception("Course does not exist"));
    courseRepository.delete(course);
   }

   //changeState of Course
   public CourseResponse changeCourseStatus(CourseStatus courseStatus,Integer courseId) throws Exception{
    Course course=courseRepository.findById(courseId)
                    .orElseThrow(()->new Exception("Course does not exist"));
    course.setStatus(courseStatus);
    courseRepository.save(course);
    return courseMapper.toResponse(course);
   }

    
}

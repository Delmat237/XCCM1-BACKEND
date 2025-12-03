package com.ihm.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ihm.backend.entities.*;

public interface UserRepository extends JpaRepository<User,Integer> {
    
}

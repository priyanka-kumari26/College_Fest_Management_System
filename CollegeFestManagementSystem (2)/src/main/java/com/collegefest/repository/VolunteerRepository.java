package com.collegefest.repository;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.collegefest.model.Volunteer;

@Repository
public interface VolunteerRepository extends JpaRepository<Volunteer, Long> {
    
    Volunteer findByUsername(String username);
    
    Volunteer findByUsernameAndPassword(String username, String password);
    
    Volunteer findByEmail(String email);
    
    @Override
    Optional<Volunteer> findById(Long id);
}
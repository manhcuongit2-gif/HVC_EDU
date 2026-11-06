package com.educenter.repository;

import com.educenter.entity.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    List<Registration> findAllByOrderByRegistrationDateDesc();
    List<Registration> findByStatusOrderByRegistrationDateDesc(String status);
    List<Registration> findByParentIdOrderByRegistrationDateDesc(Long parentId);
    List<Registration> findByParentUserIdOrderByRegistrationDateDesc(Long userId);
    long countByStatus(String status);
}

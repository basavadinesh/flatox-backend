package com.flatox.backend.repository;

import com.flatox.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    Optional<User> findByPhone(String phone);

    Optional<User> findByEmail(String email);

    boolean existsByFlatIdAndUserTypeIgnoreCase(Long flatId, String userType);

    boolean existsByFlatIdAndApprovalStatusNot(Long flatId, com.flatox.backend.enums.ApprovalStatus approvalStatus);

    java.util.List<User> findByApartmentIdAndRole(Long apartmentId, com.flatox.backend.enums.Role role);
}
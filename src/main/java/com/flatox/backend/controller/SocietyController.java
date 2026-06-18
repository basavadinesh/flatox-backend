package com.flatox.backend.controller;

import com.flatox.backend.dto.response.UserResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.flatox.backend.service.SocietyService;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/society")
@RequiredArgsConstructor
public class SocietyController {

        private final SocietyService societyService;

        @GetMapping("/approvals")
        public List<UserResponseDTO> getApprovals(
                        Principal principal) {
                return societyService.getApprovals(
                                principal.getName());
        }

        @PutMapping("/approvals/{id}/approve")
        public Map<String, String> approveUser(
                        @PathVariable Long id,
                        Principal principal) {
                return societyService.approveUser(
                                id,
                                principal.getName());
        }

        @PutMapping("/approvals/{id}/reject")
        public Map<String, String> rejectUser(
                        @PathVariable Long id,
                        Principal principal) {
                return societyService.rejectUser(
                                id,
                                principal.getName());
        }

        @GetMapping("/residents")
        public List<UserResponseDTO> getResidents(
                        Principal principal) {
                return societyService.getResidents(
                                principal.getName());
        }

        @DeleteMapping("/residents/{id}")
        public Map<String, String> deleteResident(
                        @PathVariable Long id,
                        Principal principal) {
                return societyService.deleteResident(
                                id,
                                principal.getName());
        }
}


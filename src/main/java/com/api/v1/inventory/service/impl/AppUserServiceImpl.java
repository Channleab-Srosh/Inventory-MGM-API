package com.api.v1.inventory.service.impl;

import com.api.v1.inventory.common.exception.ConflictException;
import com.api.v1.inventory.common.exception.ResourceNotFoundException;
import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.entity.AppUser;
import com.api.v1.inventory.model.enumerate.Role;
import com.api.v1.inventory.model.request.CreateUserRequest;
import com.api.v1.inventory.model.request.UpdateUserRequest;
import com.api.v1.inventory.model.request.UpdateUserStatusRequest;
import com.api.v1.inventory.model.response.UserResponse;
import com.api.v1.inventory.repository.AppUserRepository;
import com.api.v1.inventory.repository.StockTransactionRepository;
import com.api.v1.inventory.security.CurrentUserService;
import com.api.v1.inventory.service.AppUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppUserServiceImpl implements AppUserService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final StockTransactionRepository stockTransactionRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        if (!user.isActive()) {
            throw new UsernameNotFoundException("User account is inactive or disabled: " + username);
        }

        return new User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String normalizedUsername = request.getUsername().trim().toLowerCase();
        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new ConflictException("Username '" + request.getUsername() + "' is already taken");
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String normalizedEmail = request.getEmail().trim().toLowerCase();
            if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
                throw new ConflictException("Email '" + request.getEmail() + "' is already in use");
            }
        }

        AppUser user = AppUser.builder()
                .username(request.getUsername().trim())
                .email(request.getEmail() != null && !request.getEmail().trim().isEmpty() ? request.getEmail().trim().toLowerCase() : null)
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .role(request.getRole())
                .active(true)
                .mustChangePassword(true)
                .build();

        AppUser saved = userRepository.save(user);
        log.info("Created new user: id={}, username={}, role={}", saved.getId(), saved.getUsername(), saved.getRole());
        return UserResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(String search, Pageable pageable) {
        Page<AppUser> page;
        if (search != null && !search.trim().isEmpty()) {
            String keyword = search.trim();
            page = userRepository.findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCase(keyword, keyword, pageable);
        } else {
            page = userRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(UserResponse::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        return UserResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        AppUser currentUser = currentUserService.getCurrentUser();
        if (currentUser.getId().equals(user.getId()) && user.getRole() == Role.ADMIN && request.getRole() != Role.ADMIN) {
            throw new ConflictException("You cannot remove your own administrator role");
        }
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String normalizedEmail = request.getEmail().trim().toLowerCase();
            userRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(existing -> {
                if (!existing.getId().equals(user.getId())) {
                    throw new ConflictException("Email '" + request.getEmail() + "' is already in use by another user");
                }
            });
            user.setEmail(normalizedEmail);
        } else {
            user.setEmail(null);
        }

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        user.setRole(request.getRole());

        AppUser updated = userRepository.save(user);
        log.info("Updated user: id={}, username={}", updated.getId(), updated.getUsername());
        return UserResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(Long id, UpdateUserStatusRequest request) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        AppUser currentUser = currentUserService.getCurrentUser();
        if (currentUser.getId().equals(user.getId()) && !request.getActive()) {
            throw new ConflictException("You cannot deactivate your own account");
        }

        user.setActive(request.getActive());
        AppUser updated = userRepository.save(user);
        log.info("Updated status for user: id={}, active={}", updated.getId(), updated.isActive());
        return UserResponse.fromEntity(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        AppUser currentUser = currentUserService.getCurrentUser();
        return UserResponse.fromEntity(currentUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        // Prevent admin from deleting their own account
        AppUser currentUser = currentUserService.getCurrentUser();
        if (currentUser.getId().equals(user.getId())) {
            throw new ConflictException("You cannot delete your own account");
        }

        // Block delete if user has stock transaction history (data integrity)
        if (stockTransactionRepository.existsByCreatedById(id)) {
            throw new ConflictException(
                    "Cannot delete user '" + user.getUsername() + "' because they have stock transaction records. " +
                    "Deactivate the account instead to preserve data integrity."
            );
        }

        userRepository.delete(user);
        log.info("Deleted user: id={}, username={}", id, user.getUsername());
    }
}

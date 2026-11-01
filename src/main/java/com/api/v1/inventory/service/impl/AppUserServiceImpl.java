package com.api.v1.inventory.service.impl;

import com.api.v1.inventory.common.exception.BadRequestException;
import com.api.v1.inventory.common.exception.ConflictException;
import com.api.v1.inventory.common.exception.ResourceNotFoundException;
import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.entity.AppUser;
import com.api.v1.inventory.model.enumerate.Role;
import com.api.v1.inventory.model.request.CreateUserRequest;
import com.api.v1.inventory.model.request.UpdateProfileRequest;
import com.api.v1.inventory.model.request.UpdateUserRequest;
import com.api.v1.inventory.model.request.UpdateUserStatusRequest;
import com.api.v1.inventory.model.response.UserResponse;
import com.api.v1.inventory.repository.AppUserRepository;
import com.api.v1.inventory.repository.StockTransactionRepository;
import com.api.v1.inventory.security.CurrentUserService;
import com.api.v1.inventory.service.AppUserService;
import com.api.v1.inventory.service.EmailService;
import com.api.v1.inventory.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppUserServiceImpl implements AppUserService {

    public static final int MAX_ADMIN_ACCOUNTS = 3;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String PWD_UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String PWD_LOWER = "abcdefghjkmnpqrstuvwxyz";
    private static final String PWD_DIGITS = "23456789";
    private static final String PWD_SPECIAL = "@#$!%*?&";
    private static final String PWD_ALL = PWD_UPPER + PWD_LOWER + PWD_DIGITS + PWD_SPECIAL;

    public static String generateSecureRandomPassword() {
        StringBuilder sb = new StringBuilder(10);
        sb.append(PWD_UPPER.charAt(SECURE_RANDOM.nextInt(PWD_UPPER.length())));
        sb.append(PWD_LOWER.charAt(SECURE_RANDOM.nextInt(PWD_LOWER.length())));
        sb.append(PWD_DIGITS.charAt(SECURE_RANDOM.nextInt(PWD_DIGITS.length())));
        sb.append(PWD_SPECIAL.charAt(SECURE_RANDOM.nextInt(PWD_SPECIAL.length())));
        for (int i = 4; i < 10; i++) {
            sb.append(PWD_ALL.charAt(SECURE_RANDOM.nextInt(PWD_ALL.length())));
        }
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = SECURE_RANDOM.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final StockTransactionRepository stockTransactionRepository;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        if (!user.isActive()) {
            throw new UsernameNotFoundException("User account is inactive or pending administrator approval: " + username);
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

        String currentUsername = currentUserService.getCurrentUsername();
        if (currentUsername != null) {
            userRepository.findByUsernameIgnoreCase(currentUsername).ifPresent(currentUser -> {
                if (currentUser.getRole() == Role.STAFF) {
                    throw new AccessDeniedException("Staff role is not allowed to create users");
                }
                if (currentUser.getRole() == Role.MANAGER) {
                    if (request.getRole() != Role.STAFF && request.getRole() != Role.ADMIN) {
                        throw new BadRequestException("Management role is only permitted to create Staff and Admin accounts");
                    }
                }
            });
        }

        if (request.getRole() == Role.ADMIN) {
            long adminCount = userRepository.countByRole(Role.ADMIN);
            if (adminCount >= MAX_ADMIN_ACCOUNTS) {
                throw new BadRequestException("Cannot create admin user. Maximum limit of " + MAX_ADMIN_ACCOUNTS + " administrator accounts has been reached.");
            }
        }

        String rawPassword;
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            String p = request.getPassword().trim();
            if (p.length() < 6) {
                throw new BadRequestException("Password must be at least 6 characters");
            }
            rawPassword = p;
        } else {
            rawPassword = generateSecureRandomPassword();
        }

        AppUser user = AppUser.builder()
                .username(request.getUsername().trim())
                .email(request.getEmail() != null && !request.getEmail().trim().isEmpty() ? request.getEmail().trim().toLowerCase() : null)
                .password(passwordEncoder.encode(rawPassword))
                .fullName(request.getFullName().trim())
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .imageUrl(request.getImageUrl() != null && !request.getImageUrl().trim().isEmpty() ? request.getImageUrl().trim() : null)
                .role(request.getRole())
                .active(true)
                .mustChangePassword(true)
                .build();

        AppUser saved = userRepository.save(user);
        log.info("Created new user: id={}, username={}, role={}", saved.getId(), saved.getUsername(), saved.getRole());

        if (saved.getEmail() != null && !saved.getEmail().trim().isEmpty()) {
            try {
                emailService.sendNewUserCredentialsEmail(
                        saved.getEmail(),
                        saved.getFullName(),
                        saved.getUsername(),
                        rawPassword,
                        saved.getRole().name()
                );
            } catch (Exception e) {
                log.warn("Failed to dispatch welcome credentials email to {}: {}", saved.getEmail(), e.getMessage());
            }
        }

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

        if (request.getRole() == Role.ADMIN && user.getRole() != Role.ADMIN) {
            long adminCount = userRepository.countByRole(Role.ADMIN);
            if (adminCount >= MAX_ADMIN_ACCOUNTS) {
                throw new BadRequestException("Cannot update user role to ADMIN. Maximum limit of " + MAX_ADMIN_ACCOUNTS + " administrator accounts has been reached.");
            }
        }
        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            String newUsername = request.getUsername().trim();
            if (!newUsername.equalsIgnoreCase(user.getUsername())) {
                String normalizedUsername = newUsername.toLowerCase();
                userRepository.findByUsernameIgnoreCase(normalizedUsername).ifPresent(existing -> {
                    if (!existing.getId().equals(user.getId())) {
                        throw new ConflictException("Username '" + newUsername + "' is already taken");
                    }
                });
                user.setUsername(newUsername);
            }
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
        if (request.getImageUrl() != null) {
            String trimmed = request.getImageUrl().trim();
            user.setImageUrl(trimmed.isEmpty() ? null : trimmed);
        }
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
    public UserResponse updateCurrentUserProfile(UpdateProfileRequest request) {
        AppUser user = currentUserService.getCurrentUser();

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

        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone() != null && !request.getPhone().trim().isEmpty() ? request.getPhone().trim() : null);

        AppUser updated = userRepository.save(user);
        log.info("Current user profile updated: id={}, username={}", updated.getId(), updated.getUsername());
        return UserResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public UserResponse uploadCurrentUserProfileImage(MultipartFile file) {
        AppUser user = currentUserService.getCurrentUser();
        if (user.getImageUrl() != null) {
            fileStorageService.deleteByUrl(user.getImageUrl());
        }
        String newImageUrl = fileStorageService.storeProfileImage(file);
        user.setImageUrl(newImageUrl);
        AppUser updated = userRepository.save(user);
        log.info("User profile image uploaded: id={}, imageUrl={}", updated.getId(), updated.getImageUrl());
        return UserResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public UserResponse removeCurrentUserProfileImage() {
        AppUser user = currentUserService.getCurrentUser();
        if (user.getImageUrl() != null) {
            fileStorageService.deleteByUrl(user.getImageUrl());
            user.setImageUrl(null);
            user = userRepository.save(user);
            log.info("User profile image removed: id={}", user.getId());
        }
        return UserResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public UserResponse uploadUserProfileImage(Long id, MultipartFile file) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        if (user.getImageUrl() != null) {
            fileStorageService.deleteByUrl(user.getImageUrl());
        }
        String newImageUrl = fileStorageService.storeProfileImage(file);
        user.setImageUrl(newImageUrl);
        AppUser updated = userRepository.save(user);
        log.info("User profile image uploaded by admin: id={}, imageUrl={}", updated.getId(), updated.getImageUrl());
        return UserResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public UserResponse removeUserProfileImage(Long id) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        if (user.getImageUrl() != null) {
            fileStorageService.deleteByUrl(user.getImageUrl());
            user.setImageUrl(null);
            user = userRepository.save(user);
            log.info("User profile image removed by admin: id={}", user.getId());
        }
        return UserResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        deleteUsers(List.of(id));
    }

    @Override
    @Transactional
    public void deleteUsers(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("No user IDs provided for deletion");
        }

        // 1. Single query to load all requested users
        List<AppUser> users = userRepository.findAllById(ids);
        Set<Long> foundIds = users.stream()
                .map(AppUser::getId)
                .collect(Collectors.toSet());

        // 2. Identify missing IDs and reject with 404 listing ALL missing IDs
        List<Long> missingIds = ids.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        if (!missingIds.isEmpty()) {
            String msg = (missingIds.size() == 1)
                    ? "User not found with ID: " + missingIds.get(0) + ". Nothing was deleted."
                    : "Users not found with IDs: " + missingIds + ". Nothing was deleted.";
            throw new ResourceNotFoundException(msg);
        }

        // 3. Prevent admin from deleting their own account
        AppUser currentUser = currentUserService.getCurrentUser();
        if (currentUser != null && ids.contains(currentUser.getId())) {
            throw new ConflictException("You cannot delete your own account. Nothing was deleted.");
        }

        // 4. Block delete if any user has stock transaction history (data integrity)
        List<Long> referencedIds = stockTransactionRepository.findReferencedUserIds(ids);
        if (!referencedIds.isEmpty()) {
            throw new ConflictException(
                    "Cannot delete user because they have stock transaction records. Deactivate the account instead to preserve data integrity. Referenced user IDs: "
                            + referencedIds + ". Nothing was deleted."
            );
        }

        // 5. Clean up profile images
        for (AppUser u : users) {
            if (u.getImageUrl() != null) {
                fileStorageService.deleteByUrl(u.getImageUrl());
            }
        }

        // 6. Atomic delete
        userRepository.deleteAll(users);
        log.info("Deleted {} user(s) with ids: {}", users.size(), ids);
    }
}

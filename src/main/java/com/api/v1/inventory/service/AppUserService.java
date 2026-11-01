package com.api.v1.inventory.service;

import com.api.v1.inventory.common.response.PageResponse;
import com.api.v1.inventory.model.request.CreateUserRequest;
import com.api.v1.inventory.model.request.UpdateProfileRequest;
import com.api.v1.inventory.model.request.UpdateUserRequest;
import com.api.v1.inventory.model.request.UpdateUserStatusRequest;
import com.api.v1.inventory.model.response.UserResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AppUserService extends UserDetailsService {

    UserResponse createUser(CreateUserRequest request);

    PageResponse<UserResponse> getAllUsers(String search, Pageable pageable);

    UserResponse getUserById(Long id);

    UserResponse updateUser(Long id, UpdateUserRequest request);

    UserResponse updateUserStatus(Long id, UpdateUserStatusRequest request);

    UserResponse getCurrentUserProfile();

    UserResponse updateCurrentUserProfile(UpdateProfileRequest request);

    UserResponse uploadCurrentUserProfileImage(MultipartFile file);

    UserResponse removeCurrentUserProfileImage();

    UserResponse uploadUserProfileImage(Long id, MultipartFile file);

    UserResponse removeUserProfileImage(Long id);

    void deleteUser(Long id);

    void deleteUsers(List<Long> ids);
}

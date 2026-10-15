package com.api.v1.inventory.security;

import com.api.v1.inventory.common.exception.ResourceNotFoundException;
import com.api.v1.inventory.model.entity.AppUser;
import com.api.v1.inventory.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final AppUserRepository userRepository;

    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }

    public AppUser getCurrentUser() {
        String username = getCurrentUsername();
        if (username == null) {
            throw new ResourceNotFoundException("No authenticated user found in current context");
        }
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found in database: " + username));
    }
}

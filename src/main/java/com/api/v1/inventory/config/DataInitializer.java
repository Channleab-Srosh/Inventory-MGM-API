package com.api.v1.inventory.config;

import com.api.v1.inventory.model.entity.AppUser;
import com.api.v1.inventory.model.enumerate.Role;
import com.api.v1.inventory.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        userRepository.findByUsernameIgnoreCase("admin").ifPresentOrElse(
                admin -> {
                    admin.setPassword(passwordEncoder.encode("Admin@12345"));
                    admin.setActive(true);
                    userRepository.save(admin);
                },
                () -> {
                    AppUser admin = AppUser.builder()
                            .username("admin")
                            .email("admin@inventory.com")
                            .password(passwordEncoder.encode("Admin@12345"))
                            .fullName("System Administrator")
                            .phone("012345678")
                            .role(Role.ADMIN)
                            .active(true)
                            .build();
                    userRepository.save(admin);
                    log.info("Initialized default ADMIN user: admin / Admin@12345");
                }
        );

        userRepository.findByUsernameIgnoreCase("staff").ifPresentOrElse(
                staff -> {
                    staff.setPassword(passwordEncoder.encode("Staff@12345"));
                    staff.setActive(true);
                    userRepository.save(staff);
                },
                () -> {
                    AppUser staff = AppUser.builder()
                            .username("staff")
                            .email("staff@inventory.com")
                            .password(passwordEncoder.encode("Staff@12345"))
                            .fullName("Inventory Staff")
                            .phone("012345679")
                            .role(Role.STAFF)
                            .active(true)
                            .build();
                    userRepository.save(staff);
                    log.info("Initialized default STAFF user: staff / Staff@12345");
                }
        );
    }
}

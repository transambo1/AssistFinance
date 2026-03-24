package com.financeai.finance_management.config;

import com.financeai.finance_management.entity.User;
import com.financeai.finance_management.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.HashSet;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Configuration
public class ApplicationInitConfig {

    PasswordEncoder passwordEncoder;

    @NonFinal
    static final String ADMIN_USER = "admin";

    @NonFinal
    static final String ADMIN_PASSWORD = "admin";

    @NonFinal
    static final String ADMIN_ID = "0898454043";


    @Bean
    @ConditionalOnProperty(
            prefix = "spring",
            value = "datasource.driverClassName",
            havingValue = "com.mysql.cj.jdbc.Driver"
    )
    ApplicationRunner applicationRunner(UserRepository userRepository) {
        return args -> {
           if(userRepository.findByUsername(ADMIN_USER).isEmpty()) {
               User user = User.builder()
                       .id(ADMIN_ID)
                       .username(ADMIN_USER)
                       .password(passwordEncoder.encode(ADMIN_PASSWORD))
                       .fullName("Trần Tuấn Phát")
                       .displayName("Chris")
                       .email("tuanphat17edu@gmail.com")
                       .phone("098454043")
                       .currency("VND")
                       .build();

               userRepository.save(user);
                log.warn("admin user has been created with default password: admin");
           }
        };
    }
}

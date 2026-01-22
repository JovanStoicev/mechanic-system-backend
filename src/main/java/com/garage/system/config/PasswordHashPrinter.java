package com.garage.system.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class PasswordHashPrinter {

    @Bean
    CommandLineRunner printHash() {
        return args -> {
            var enc = new BCryptPasswordEncoder();
            System.out.println("BOSS_HASH=" + enc.encode("boss123"));
        };
    }
}

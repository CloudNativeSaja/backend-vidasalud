package cl.duoc.vidasalud.bff.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;



@Configuration
@EnableMethodSecurity
public class SecurityConfig {


    private final JwtRoleConverter jwtRoleConverter;


    public SecurityConfig(
            JwtRoleConverter jwtRoleConverter
    ) {

        this.jwtRoleConverter = jwtRoleConverter;

    }



    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {



return http

    .cors(cors -> {})

    .csrf(csrf -> csrf.disable())

    .authorizeHttpRequests(auth -> auth

        .requestMatchers("/actuator/health")
        .permitAll()

        .requestMatchers(HttpMethod.OPTIONS,"/**")
        .permitAll()

        .anyRequest()
        .authenticated()

    )

    .oauth2ResourceServer(
        oauth2 -> oauth2
            .jwt(jwt ->
                jwt.jwtAuthenticationConverter(
                    jwtRoleConverter
                )
            )
    )

    .build();

    }

}
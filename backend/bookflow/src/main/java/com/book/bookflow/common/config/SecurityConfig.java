package com.example.common;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(request -> {
            CorsConfiguration config = new CorsConfiguration();
            config.addAllowedOrigin("*");
            config.addAllowedHeader("*");
            config.addAllowedMethod("*");
            return config;
        }));
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/login", "/register",
                                "/files/download/**", "/orders", "/hello", "/admin").permitAll()
                        // 公开访问的查询接口
                        .requestMatchers(HttpMethod.GET,
                                "/notice/selectAll").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/notice/selectPage").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/activity/selectAll").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/activity/selectPage").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/activity/signUp").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/activity/isSignedUp").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/activity/cancelSignUp").permitAll()
                        // 需要管理员权限的修改操作
                        .requestMatchers(HttpMethod.POST,
                                "/notice/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,
                                "/notice/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE,
                                "/notice/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE,
                                "/activity/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,
                                "/activity/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,
                                "/activity/**").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .sessionManagement(session ->

                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
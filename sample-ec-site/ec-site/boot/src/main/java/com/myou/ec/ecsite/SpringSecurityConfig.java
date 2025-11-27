package com.myou.ec.ecsite;


import com.myou.ec.ecsite.application.auth.sharedservice.PasswordChangeSharedService;
import com.myou.ec.ecsite.presentation.auth.security.AuthAuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
class SpringSecurityConfig {

    @Bean
    @Order(1) // Application security should be processed first
    public SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http,
                                                              AuthAuthenticationSuccessHandler successHandler) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/account/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("username") // Match the parameter name in AuthAuthenticationFailureHandler
                .passwordParameter("password")
                .successHandler(successHandler)
                .failureUrl("/login?error")
                .permitAll()
            )
            .passwordManagement(management -> management
                    .changePasswordPage("/password/change")
                )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll())
            .csrf(csrf -> csrf.disable()); // Temporarily disable CSRF for easier testing
        return http.build();
    }

    @Bean
    @Order(2) // Actuator security processed second
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(EndpointRequest.toAnyEndpoint())
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().permitAll());
        return http.build();
    }

    @Bean
    public AuthAuthenticationSuccessHandler authAuthenticationSuccessHandler(
            PasswordChangeSharedService passwordChangeSharedService,
            @Value("${auth.default-success-url:/menu}") String defaultSuccessUrl
    ) {
        AuthAuthenticationSuccessHandler h = new AuthAuthenticationSuccessHandler(passwordChangeSharedService);
        h.setDefaultTargetUrl(defaultSuccessUrl);

        // SavedRequestがあればそれを優先（デフォルト）。trueにすると常にdefaultへ飛ばす
        h.setAlwaysUseDefaultTargetUrl(false);

        return h;
    }
}

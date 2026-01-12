package com.myou.ec.ecsite.config;


import com.myou.ec.ecsite.application.auth.sharedservice.PasswordChangeSharedService;
import com.myou.ec.ecsite.presentation.auth.security.handler.AuthAuthenticationFailureHandler;
import com.myou.ec.ecsite.presentation.auth.security.handler.AuthAuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
class SpringSecurityConfig {

    @Bean
    public SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http,
                                                              AuthAuthenticationSuccessHandler successHandler,
                                                              AuthAuthenticationFailureHandler failureHandler) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers(PathRequest.toH2Console()).permitAll()
                        .requestMatchers("/login").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("loginId")
                        .passwordParameter("password")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll()
                )
                .passwordManagement(management -> management
                        .changePasswordPage("/account/password/change")
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                .csrf(AbstractHttpConfigurer::disable) // Temporarily disable CSRF for easier testin
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                );
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

    @Bean
    public AuthAuthenticationFailureHandler authAuthenticationFailureHandler(
            @Value("${auth.failure-url:/login?error}") String failureUrl
    ) {
        return new AuthAuthenticationFailureHandler(failureUrl);
    }
}

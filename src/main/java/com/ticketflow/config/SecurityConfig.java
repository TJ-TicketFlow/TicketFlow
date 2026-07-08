package com.ticketflow.config;

import com.ticketflow.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 1️⃣ [핵심] CSRF 보호 조치에서 좌석 예매 API 주소들을 제외시킵니다.
        // POST 요청 시 토큰 검사를 하지 않도록 열어주는 설정입니다.

        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**", "/api/payment/webhook","/concert/*/like","/seat/api/**", "/seat/select", "/seat/cancel", "/ws-seat/**","/seat/api/booking/cancel-ajax", "/seat/api/booking/cancel-ajax") //CSRF 검증 제외 코드 추가
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/login", "/register",
                                "/api/check-userid",         // 아이디 중복확인 API
                                "/api/create-checkout",
                                "/register/send-code",
                                "/register/verify-code",
                                "/find-id", "/find-id/**",
                                "/find-password", "/find-password/**",
                                "/css/**", "/js/**", "/images/**", "/favicon.ico",
                                "/concert/","/concert/**","/concert/{id}/sessions","/search",
                                "/api/booking/webhooks", "/api/payment/webhook", "/booking/payresult","/seat/api/booking/cancel-ajax", "/seat/api/booking/prepare",
                                "/ws-seat/**"
                        ).permitAll()
                        .requestMatchers("/mypage/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("user_id")
                        .passwordParameter("password")
                        // 🌟 [수정] 기존에는 defaultSuccessUrl("/", true) 와, "/"로만 무조건
                        // 리다이렉트하는 커스텀 successHandler가 동시에 설정되어 있었습니다.
                        // successHandler가 등록되면 defaultSuccessUrl은 완전히 무시되므로
                        // (죽은 설정), 실제 동작은 successHandler가 전부 결정하고 있었는데
                        // 그 핸들러가 무조건 "/"로만 보내서, 로그인 안 된 상태로 /mypage 같은
                        // 보호된 페이지에 접근했다가 로그인 화면으로 튕겨나간 경우에도 로그인 후
                        // 원래 가려던 페이지로 못 돌아가고 항상 홈으로만 이동하는 문제가 있었습니다.
                        // alwaysUse=false로 두면 Spring Security가 "원래 가려던 페이지
                        // (SavedRequest)"가 있으면 그곳으로, 없으면 "/"로 보내줍니다.
                        .defaultSuccessUrl("/", false)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
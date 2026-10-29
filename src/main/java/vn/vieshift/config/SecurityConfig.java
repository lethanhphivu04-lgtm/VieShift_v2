package vn.vieshift.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import vn.vieshift.entity.Role;
import vn.vieshift.entity.User;
import vn.vieshift.entity.VerificationStatus;
import vn.vieshift.entity.ViekerProfile;
import vn.vieshift.entity.Wallet;
import vn.vieshift.repository.UserRepository;
import vn.vieshift.repository.ViekerProfileRepository;
import vn.vieshift.repository.WalletRepository;
import vn.vieshift.service.ViekerOnboardingService;

import java.util.UUID;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${vieshift.google.client-id:}")
    private String googleClientId;

    @Value("${vieshift.google.client-secret:}")
    private String googleClientSecret;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return email -> {
            User user = userRepository.findByEmail(email.trim().toLowerCase())
                    .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản với email: " + email));

            if (!user.isActive()) {
                throw new org.springframework.security.authentication.DisabledException("Tài khoản đã bị tạm khóa");
            }

            return org.springframework.security.core.userdetails.User
                    .withUsername(user.getEmail())
                    .password(user.getPassword())
                    .roles(user.getRole().name())
                    .build();
        };
    }

    @Bean
    public AuthenticationSuccessHandler roleBasedSuccessHandler(UserRepository userRepository,
                                                                ViekerProfileRepository profileRepository,
                                                                ViekerOnboardingService onboardingService) {
        return (request, response, authentication) -> {
            boolean isAdmin = false;
            boolean isVieker = false;

            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String role = authority.getAuthority();
                if ("ROLE_ADMIN".equals(role)) {
                    isAdmin = true;
                    break;
                } else if ("ROLE_VIEKER".equals(role)) {
                    isVieker = true;
                }
            }

            if (isAdmin) {
                response.sendRedirect("/admin");
            } else if (isVieker) {
                User user = userRepository.findByEmail(authentication.getName()).orElse(null);
                if (user != null) {
                    ViekerProfile profile = profileRepository.findByUserId(user.getId()).orElse(null);
                    if (profile != null && profile.getVerificationStatus() != VerificationStatus.APPROVED) {
                        String token = onboardingService.ensureToken(profile);
                        response.sendRedirect("/onboarding/" + token);
                        return;
                    }
                }
                response.sendRedirect("/vieker");
            } else {
                jakarta.servlet.http.HttpSession session = request.getSession(false);
                String redirectTarget = (session != null) ? (String) session.getAttribute("REDIRECT_AFTER_LOGIN") : null;
                if (redirectTarget != null && !redirectTarget.isBlank()) {
                    session.removeAttribute("REDIRECT_AFTER_LOGIN");
                    response.sendRedirect(redirectTarget);
                } else {
                    response.sendRedirect("/");
                }
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationSuccessHandler successHandler,
                                                   UserRepository userRepository,
                                                   WalletRepository walletRepository,
                                                   PasswordEncoder passwordEncoder) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/explore",
                                "/book-task",
                                "/login",
                                "/register",
                                "/become-vieker",
                                "/become-vieker/**",
                                "/onboarding/**",
                                "/terms",
                                "/privacy",
                                "/about",
                                "/partners",
                                "/guarantee",
                                "/handbook",
                                "/mobile-app",
                                "/payment/**",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/uploads/**",
                                "/api/viekers/**",
                                "/api/vouchers/**",
                                "/error"
                        ).permitAll()
                        .requestMatchers("/customer/disputes/*/attachment").authenticated()
                        .requestMatchers("/customer/**").hasRole("CUSTOMER")
                        .requestMatchers("/vieker/**").hasRole("VIEKER")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(successHandler)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        if (googleClientId != null && !googleClientId.isBlank()
                && googleClientSecret != null && !googleClientSecret.isBlank()) {
            ClientRegistration googleRegistration = CommonOAuth2Provider.GOOGLE.getBuilder("google")
                    .clientId(googleClientId.trim())
                    .clientSecret(googleClientSecret.trim())
                    .scope("openid", "profile", "email")
                    .build();
            InMemoryClientRegistrationRepository clientRegistrationRepository =
                    new InMemoryClientRegistrationRepository(googleRegistration);

            http.oauth2Login(oauth -> oauth
                    .loginPage("/login")
                    .clientRegistrationRepository(clientRegistrationRepository)
                    .successHandler((request, response, authentication) -> {
                        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
                        String email = oauthUser.getAttribute("email");
                        String name = oauthUser.getAttribute("name");
                        if (email == null || email.isBlank()) {
                            response.sendRedirect("/login?oauth2_error=true");
                            return;
                        }
                        String normalizedEmail = email.trim().toLowerCase();
                        User user = userRepository.findByEmail(normalizedEmail).orElseGet(() -> {
                            String fullName = (name != null && !name.isBlank()) ? name.trim() : normalizedEmail;
                            User newUser = userRepository.save(new User(
                                    normalizedEmail,
                                    passwordEncoder.encode(UUID.randomUUID().toString()),
                                    fullName,
                                    "Chưa cập nhật",
                                    Role.CUSTOMER
                            ));
                            walletRepository.save(new Wallet(newUser));
                            return newUser;
                        });

                        UserDetails principal = org.springframework.security.core.userdetails.User
                                .withUsername(user.getEmail())
                                .password(user.getPassword())
                                .roles(user.getRole().name())
                                .build();
                        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities());
                        org.springframework.security.core.context.SecurityContext context =
                                SecurityContextHolder.createEmptyContext();
                        context.setAuthentication(token);
                        SecurityContextHolder.setContext(context);
                        new org.springframework.security.web.context.HttpSessionSecurityContextRepository()
                                .saveContext(context, request, response);
                        successHandler.onAuthenticationSuccess(request, response, token);
                    })
                    .failureHandler((request, response, exception) -> {
                        String errorMessage = exception != null ? exception.getMessage() : "Xác thực Google bị từ chối";
                        request.getSession().setAttribute("OAUTH2_ERROR", errorMessage);
                        response.sendRedirect("/login?oauth2_error=true");
                    })
            );
        }

        return http.build();
    }
}

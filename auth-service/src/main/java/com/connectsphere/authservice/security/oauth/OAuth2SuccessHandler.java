package com.connectsphere.authservice.security.oauth;

import com.connectsphere.authservice.entity.*;
import com.connectsphere.authservice.repository.UserRepository;
import com.connectsphere.authservice.security.JwtService;
import com.connectsphere.authservice.service.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        if (email == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not found from OAuth provider");
            return;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        // 🔍 Find or Create User
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(normalizedEmail);
                    newUser.setFullName(name);
                    newUser.setUsername(normalizedEmail.split("@")[0]);

                    newUser.setRole(UserRole.USER);
                    newUser.setStatus(UserStatus.ACTIVE);
                    newUser.setIsActive(true);
                    newUser.setProvider(AuthProvider.GOOGLE);

                    return userRepository.save(newUser);
                });

        // 🔐 Generate tokens
        String accessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user).getToken();

        // 🔁 Return tokens (simple JSON response)
        response.setContentType("application/json");
        response.getWriter().write(
                String.format(
                        "{\"token\":\"%s\",\"refreshToken\":\"%s\"}",
                        accessToken,
                        refreshToken
                )
        );
    }
}
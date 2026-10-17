package com.example.booking.service;

import com.example.booking.dto.auth.AuthResponse;
import com.example.booking.dto.auth.LoginRequest;
import com.example.booking.dto.auth.RegisterRequest;
import com.example.booking.entity.Role;
import com.example.booking.entity.User;
import com.example.booking.exception.InvalidCredentialsException;
import com.example.booking.repository.UserRepository;
import com.example.booking.security.LoginRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimiter loginRateLimiter;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(Role.USER)
                .build();

        userRepository.save(user);

        return buildResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        loginRateLimiter.checkNotBlocked(request.getUsername());

        User user = userRepository.findByUsername(request.getUsername()).orElse(null);

        // Одинаковый ответ для «нет пользователя» и «не тот пароль»,
        // иначе перебором можно выяснить, какие аккаунты существуют.
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginRateLimiter.recordFailure(request.getUsername());
            throw new InvalidCredentialsException("Invalid username or password");
        }

        loginRateLimiter.reset(request.getUsername());
        return buildResponse(user);
    }

    private AuthResponse buildResponse(User user) {
        Role role = user.getRole() == null ? Role.USER : user.getRole();

        return AuthResponse.builder()
                .token(jwtService.generateToken(user.getId(), user.getUsername(), role.name()))
                .userId(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(role.name())
                .build();
    }
}

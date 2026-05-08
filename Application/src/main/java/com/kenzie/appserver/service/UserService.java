package com.kenzie.appserver.service;

import com.kenzie.appserver.controller.model.AuthResponse;
import com.kenzie.appserver.controller.model.CreateUserRequest;
import com.kenzie.appserver.controller.model.LoginRequest;
import com.kenzie.appserver.controller.model.UserResponse;
import com.kenzie.appserver.controller.model.UserUpdateRequest;
import com.kenzie.appserver.repositories.UserDao;
import com.kenzie.appserver.repositories.model.UserRecord;
import com.kenzie.appserver.salesforce.SalesforceService;
import com.kenzie.appserver.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/** Business logic for user registration, authentication, and profile management. */
@Service
public class UserService {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SalesforceService salesforceService;

    public UserService(UserDao userDao, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, SalesforceService salesforceService) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.salesforceService = salesforceService;
    }

    /**
     * Returns the user with the given ID, or {@code null} if not found.
     *
     * @param userId the user ID
     * @return the user response, or {@code null}
     */
    public UserResponse getUserById(String userId) {
        return userDao.findById(userId)
                .map(this::recordToResponse)
                .orElse(null);
    }

    /**
     * Registers a new user, hashing the password before persistence.
     * Triggers an async Salesforce Contact sync after the local record is saved.
     *
     * @param request the registration request
     * @return the created user response (password hash is never exposed)
     * @throws org.springframework.web.server.ResponseStatusException 400 if name, email,
     *         or password is missing
     */
    public UserResponse createUser(CreateUserRequest request) {
        if (request.getName() == null || request.getEmail() == null || request.getPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name, email, and password are required");
        }
        UserRecord record = new UserRecord();
        record.setId(UUID.randomUUID().toString());
        record.setName(request.getName());
        record.setEmail(request.getEmail());
        record.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userDao.save(record);
        salesforceService.syncContact(record);

        return recordToResponse(record);
    }

    /**
     * Authenticates a user by email and password and returns a signed JWT.
     * Always uses a generic error message to avoid leaking whether the email exists.
     *
     * @param request the login credentials
     * @return an auth response containing the JWT and the user ID
     * @throws org.springframework.web.server.ResponseStatusException 401 on invalid credentials
     */
    public AuthResponse login(LoginRequest request) {
        UserRecord record = userDao.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), record.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String token = jwtUtil.generateToken(record.getId(), record.getEmail());
        return new AuthResponse(token, record.getId());
    }

    /**
     * Permanently deletes a user account.
     *
     * @param userId the user to delete
     * @throws org.springframework.web.server.ResponseStatusException 400 if ID is blank,
     *         404 if not found
     */
    public void deleteUser(String userId) {
        if (userId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID cannot be empty");
        }
        if (!userDao.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        userDao.deleteById(userId);
    }

    /**
     * Updates a user's name and email.
     *
     * @param request the update request
     * @return the updated user response
     * @throws org.springframework.web.server.ResponseStatusException 404 if not found
     */
    public UserResponse updateUser(UserUpdateRequest request) {
        UserRecord record = userDao.findById(request.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        record.setName(request.getName());
        record.setEmail(request.getEmail());
        userDao.save(record);

        return recordToResponse(record);
    }

    private UserResponse recordToResponse(UserRecord record) {
        UserResponse response = new UserResponse();
        response.setId(record.getId());
        response.setName(record.getName());
        response.setEmail(record.getEmail());
        return response;
    }
}

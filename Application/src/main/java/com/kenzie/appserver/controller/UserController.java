package com.kenzie.appserver.controller;

import com.kenzie.appserver.controller.model.*;
import com.kenzie.appserver.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/** REST controller for user registration, profile reads, updates, and deletion. */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * {@code GET /users/{id}} — returns a user profile by ID.
     * Public endpoint; no authentication required.
     *
     * @param id the user ID
     * @return 200 with the user, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable("id") String id) {
        UserResponse userResponse = userService.getUserById(id);
        if (userResponse == null){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(userResponse);
    }

    /**
     * {@code POST /users} — registers a new user account.
     * Public endpoint. Returns 201 with a Location header.
     *
     * @param createUserRequest the registration payload (name, email, password)
     * @return 201 with the created user (password is never returned)
     */
    @PostMapping
    public ResponseEntity<UserResponse> addNewUser(@Valid @RequestBody CreateUserRequest createUserRequest){
        UserResponse userResponse = userService.createUser(createUserRequest);

        return ResponseEntity.created(URI.create("/user/" + userResponse.getName())).body(userResponse);
    }

    /**
     * {@code PUT /users/{id}} — updates a user's name and email.
     * Requires a valid JWT.
     *
     * @param userUpdateRequest the update payload
     * @return 200 with the updated user, or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@Valid @RequestBody UserUpdateRequest userUpdateRequest) {

        UserResponse userResponse = userService.updateUser(userUpdateRequest);

        return ResponseEntity.ok(userResponse);
    }

    /**
     * {@code DELETE /users/{id}} — permanently deletes a user account.
     * Requires a valid JWT.
     *
     * @param userId the user to delete
     * @return 200 on success, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity deleteUserById(@PathVariable("id") String userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok().build();
    }

}

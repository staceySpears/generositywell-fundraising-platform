package com.kenzie.appserver.controller;

import com.kenzie.appserver.controller.model.*;
import com.kenzie.appserver.service.CampaignService;
import com.kenzie.appserver.service.FundraisingEventService;
import com.kenzie.appserver.service.UserService;
import com.kenzie.appserver.controller.model.DonationSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import java.net.URI;

/** REST controller for user registration, profile reads, updates, and deletion. */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final CampaignService campaignService;
    private final FundraisingEventService eventService;

    UserController(UserService userService,
                   CampaignService campaignService,
                   FundraisingEventService eventService) {
        this.userService = userService;
        this.campaignService = campaignService;
        this.eventService = eventService;
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

        return ResponseEntity.created(URI.create("/users/" + userResponse.getId())).body(userResponse);
    }

    /**
     * {@code PUT /users/{id}} — updates a user's name and email.
     * Requires a valid JWT.
     *
     * @param userUpdateRequest the update payload
     * @return 200 with the updated user, or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable("id") String id,
            @Valid @RequestBody UserUpdateRequest userUpdateRequest) {
        userUpdateRequest.setId(id);
        UserResponse userResponse = userService.updateUser(userUpdateRequest);
        return ResponseEntity.ok(userResponse);
    }

    /**
     * {@code DELETE /users/{id}} — permanently deletes a user account.
     * Requires a valid JWT.
     *
     * @param userId the user to delete
     * @return 204 on success, 400 if ID is blank, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity deleteUserById(@PathVariable("id") String userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * {@code GET /users/{id}/campaigns} — returns all campaigns created by the user.
     *
     * @param id the user ID
     * @return 200 with the list (may be empty)
     */
    @GetMapping("/{id}/campaigns")
    public ResponseEntity<List<CampaignResponse>> getCampaignsByUser(@PathVariable("id") String id) {
        return ResponseEntity.ok(campaignService.getCampaignsByUser(id));
    }

    /**
     * {@code GET /users/{id}/events} — returns all events organized by the user.
     *
     * @param id the user ID
     * @return 200 with the list (may be empty)
     */
    @GetMapping("/{id}/events")
    public ResponseEntity<List<EventResponse>> getEventsByUser(@PathVariable("id") String id) {
        return ResponseEntity.ok(eventService.getEventsByOrganizer(id));
    }

    /**
     * {@code GET /users/{id}/donations} — returns the user's giving history.
     * One entry per campaign the user has donated to, sorted newest-first.
     *
     * @param id the user ID
     * @return 200 with the list (may be empty)
     */
    @GetMapping("/{id}/donations")
    public ResponseEntity<List<DonationSummaryResponse>> getDonationsByUser(@PathVariable("id") String id) {
        return ResponseEntity.ok(campaignService.getDonationsByUser(id));
    }

    /**
     * {@code GET /users/{id}/rsvps} — returns all events the user has RSVPed to.
     * Excludes CANCELLED RSVPs. Sorted by event date ascending.
     *
     * @param id the user ID
     * @return 200 with the list (may be empty)
     */
    @GetMapping("/{id}/rsvps")
    public ResponseEntity<List<EventResponse>> getRsvpsByUser(@PathVariable("id") String id) {
        return ResponseEntity.ok(eventService.getRsvpsByUser(id));
    }

}

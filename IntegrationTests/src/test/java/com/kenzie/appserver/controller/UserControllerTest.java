package com.kenzie.appserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kenzie.appserver.IntegrationTest;
import com.kenzie.appserver.controller.model.*;
import com.kenzie.appserver.security.JwtUtil;
import com.kenzie.appserver.service.UserService;
import net.andreinc.mockneat.MockNeat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    private final MockNeat mockNeat = MockNeat.threadLocal();
    private final ObjectMapper mapper = new ObjectMapper();

    private UserQueryUtility userQueryUtility;

    @BeforeAll
    void setup() {
        userQueryUtility = new UserQueryUtility(mvc);
    }

    /** ------------------------------------------------------------------------
     *  POST /users
     *  ------------------------------------------------------------------------ **/

    @Test
    void createUser_validRequest_isSuccessful() throws Exception {
        CreateUserRequest request = buildCreateRequest();

        userQueryUtility.userControllerClient.addNewUser(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(is(request.getName())))
                .andExpect(jsonPath("$.email").value(is(request.getEmail())));
    }

    @Test
    void createUser_nullBody_returnsBadRequest() throws Exception {
        mvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /** ------------------------------------------------------------------------
     *  POST /auth/login
     *  ------------------------------------------------------------------------ **/

    @Test
    void login_validCredentials_returnsToken() throws Exception {
        String email = mockNeat.emails().get();
        String password = "SecurePass42!";

        CreateUserRequest createRequest = buildCreateRequest();
        createRequest.setEmail(email);
        createRequest.setPassword(password);
        userQueryUtility.userControllerClient.addNewUser(createRequest)
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        userQueryUtility.userControllerClient.login(loginRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(notNullValue()))
                .andExpect(jsonPath("$.userId").value(notNullValue()));
    }

    @Test
    void login_wrongPassword_returnsUnauthorized() throws Exception {
        String email = mockNeat.emails().get();

        CreateUserRequest createRequest = buildCreateRequest();
        createRequest.setEmail(email);
        createRequest.setPassword("CorrectPass1!");
        userQueryUtility.userControllerClient.addNewUser(createRequest);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword("WrongPass999!");

        userQueryUtility.userControllerClient.login(loginRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_unknownEmail_returnsUnauthorized() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("ghost@nowhere.com");
        loginRequest.setPassword("DoesNotMatter1!");

        userQueryUtility.userControllerClient.login(loginRequest)
                .andExpect(status().isUnauthorized());
    }

    /** ------------------------------------------------------------------------
     *  GET /users/{id}
     *  ------------------------------------------------------------------------ **/

    @Test
    void getUserById_validId_isSuccessful() throws Exception {
        CreateUserRequest request = buildCreateRequest();
        UserResponse created = userService.createUser(request);

        userQueryUtility.userControllerClient.getUserById(created.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(is(created.getName())))
                .andExpect(jsonPath("$.email").value(is(created.getEmail())));
    }

    @Test
    void getUserById_notFound_returns404() throws Exception {
        userQueryUtility.userControllerClient.getUserById("does-not-exist-" + randomId())
                .andExpect(status().isNotFound());
    }

    /** ------------------------------------------------------------------------
     *  PUT /users/{id}
     *  ------------------------------------------------------------------------ **/

    @Test
    void updateUser_validRequest_isSuccessful() throws Exception {
        CreateUserRequest createRequest = buildCreateRequest();
        UserResponse created = userService.createUser(createRequest);
        String token = jwtUtil.generateToken(created.getId(), created.getEmail());

        UserUpdateRequest update = new UserUpdateRequest();
        update.setId(created.getId());
        update.setName(mockNeat.names().first().get());
        update.setEmail(mockNeat.emails().get());

        userQueryUtility.userControllerClient.updateUser(update, token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(is(update.getName())))
                .andExpect(jsonPath("$.email").value(is(update.getEmail())));
    }

    /** ------------------------------------------------------------------------
     *  DELETE /users/{id}
     *  ------------------------------------------------------------------------ **/

    @Test
    void deleteUser_validId_isSuccessful() throws Exception {
        CreateUserRequest request = buildCreateRequest();
        UserResponse created = userService.createUser(request);
        String token = jwtUtil.generateToken(created.getId(), created.getEmail());

        userQueryUtility.userControllerClient.deleteUser(created.getId(), token)
                .andExpect(status().isOk());

        userQueryUtility.userControllerClient.getUserById(created.getId())
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private CreateUserRequest buildCreateRequest() {
        CreateUserRequest request = new CreateUserRequest();
        request.setName(mockNeat.names().first().get());
        request.setEmail(mockNeat.emails().get());
        request.setPassword("TestPass123!");
        return request;
    }

    private String randomId() {
        return java.util.UUID.randomUUID().toString();
    }
}

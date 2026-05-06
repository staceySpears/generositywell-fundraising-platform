package com.kenzie.appserver.service;

import com.kenzie.appserver.controller.model.AuthResponse;
import com.kenzie.appserver.controller.model.CreateUserRequest;
import com.kenzie.appserver.controller.model.LoginRequest;
import com.kenzie.appserver.controller.model.UserResponse;
import com.kenzie.appserver.controller.model.UserUpdateRequest;
import com.kenzie.appserver.repositories.UserDao;
import com.kenzie.appserver.repositories.model.UserRecord;
import com.kenzie.appserver.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserDao userDao;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserService userService;

    /** ------------------------------------------------------------------------
     *  UserService.getUserById
     *  ------------------------------------------------------------------------ **/

    @Test
    void getUserById_userExists_returnsResponse() {
        UserRecord record = new UserRecord();
        record.setId("user-1");
        record.setName("Stacey");
        record.setEmail("stacey@example.com");

        when(userDao.findById("user-1")).thenReturn(Optional.of(record));

        UserResponse response = userService.getUserById("user-1");

        assertNotNull(response);
        assertEquals("user-1", response.getId());
        assertEquals("Stacey", response.getName());
        assertEquals("stacey@example.com", response.getEmail());
    }

    @Test
    void getUserById_userDoesNotExist_returnsNull() {
        when(userDao.findById("missing")).thenReturn(Optional.empty());

        assertNull(userService.getUserById("missing"));
    }

    /** ------------------------------------------------------------------------
     *  UserService.createUser
     *  ------------------------------------------------------------------------ **/

    @Test
    void createUser_validRequest_hashesPasswordAndSaves() {
        CreateUserRequest request = new CreateUserRequest();
        request.setName("Jordan");
        request.setEmail("jordan@example.com");
        request.setPassword("secret123");

        when(passwordEncoder.encode("secret123")).thenReturn("hashed");

        ArgumentCaptor<UserRecord> captor = ArgumentCaptor.forClass(UserRecord.class);
        UserResponse response = userService.createUser(request);

        verify(userDao).save(captor.capture());
        UserRecord saved = captor.getValue();

        assertNotNull(response);
        assertEquals("Jordan", response.getName());
        assertEquals("jordan@example.com", response.getEmail());
        assertDoesNotThrow(() -> UUID.fromString(response.getId()));
        assertEquals("hashed", saved.getPasswordHash());
    }

    @Test
    void createUser_nullName_throwsBadRequest() {
        CreateUserRequest request = new CreateUserRequest();
        request.setName(null);
        request.setEmail("jordan@example.com");
        request.setPassword("secret123");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.createUser(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(userDao, never()).save(any());
    }

    @Test
    void createUser_nullEmail_throwsBadRequest() {
        CreateUserRequest request = new CreateUserRequest();
        request.setName("Jordan");
        request.setEmail(null);
        request.setPassword("secret123");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.createUser(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(userDao, never()).save(any());
    }

    @Test
    void createUser_nullPassword_throwsBadRequest() {
        CreateUserRequest request = new CreateUserRequest();
        request.setName("Jordan");
        request.setEmail("jordan@example.com");
        request.setPassword(null);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.createUser(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(userDao, never()).save(any());
    }

    /** ------------------------------------------------------------------------
     *  UserService.login
     *  ------------------------------------------------------------------------ **/

    @Test
    void login_validCredentials_returnsToken() {
        UserRecord record = new UserRecord();
        record.setId("user-1");
        record.setEmail("stacey@example.com");
        record.setPasswordHash("hashed");

        when(userDao.findByEmail("stacey@example.com")).thenReturn(Optional.of(record));
        when(passwordEncoder.matches("secret123", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken("user-1", "stacey@example.com")).thenReturn("jwt-token");

        LoginRequest request = new LoginRequest();
        request.setEmail("stacey@example.com");
        request.setPassword("secret123");

        AuthResponse response = userService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("user-1", response.getUserId());
    }

    @Test
    void login_userNotFound_throwsUnauthorized() {
        when(userDao.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setEmail("ghost@example.com");
        request.setPassword("password");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.login(request)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        UserRecord record = new UserRecord();
        record.setId("user-1");
        record.setEmail("stacey@example.com");
        record.setPasswordHash("hashed");

        when(userDao.findByEmail("stacey@example.com")).thenReturn(Optional.of(record));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setEmail("stacey@example.com");
        request.setPassword("wrong");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.login(request)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }

    /** ------------------------------------------------------------------------
     *  UserService.updateUser
     *  ------------------------------------------------------------------------ **/

    @Test
    void updateUser_userExists_savesAndReturnsUpdatedResponse() {
        UserRecord existing = new UserRecord();
        existing.setId("user-2");
        existing.setName("Old Name");
        existing.setEmail("old@example.com");

        when(userDao.findById("user-2")).thenReturn(Optional.of(existing));
        ArgumentCaptor<UserRecord> captor = ArgumentCaptor.forClass(UserRecord.class);

        UserResponse response = userService.updateUser(new UserUpdateRequest("user-2", "New Name", "new@example.com"));

        verify(userDao).save(captor.capture());
        assertEquals("New Name", captor.getValue().getName());
        assertEquals("New Name", response.getName());
    }

    @Test
    void updateUser_userDoesNotExist_throwsNotFound() {
        when(userDao.findById("ghost")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.updateUser(new UserUpdateRequest("ghost", "Name", "email@example.com"))
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(userDao, never()).save(any());
    }

    /** ------------------------------------------------------------------------
     *  UserService.deleteUser
     *  ------------------------------------------------------------------------ **/

    @Test
    void deleteUser_emptyId_throwsBadRequest() {
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseStatusException.class, () -> userService.deleteUser("")).getStatusCode());
        verify(userDao, never()).deleteById(any());
    }

    @Test
    void deleteUser_userDoesNotExist_throwsNotFound() {
        String id = UUID.randomUUID().toString();
        when(userDao.existsById(id)).thenReturn(false);

        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ResponseStatusException.class, () -> userService.deleteUser(id)).getStatusCode());
        verify(userDao, never()).deleteById(any());
    }

    @Test
    void deleteUser_userExists_callsDeleteById() {
        String id = UUID.randomUUID().toString();
        when(userDao.existsById(id)).thenReturn(true);

        userService.deleteUser(id);

        verify(userDao).deleteById(id);
    }
}

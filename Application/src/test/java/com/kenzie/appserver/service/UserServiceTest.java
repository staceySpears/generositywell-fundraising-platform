package com.kenzie.appserver.service;

import com.kenzie.appserver.controller.model.CreateUserRequest;
import com.kenzie.appserver.controller.model.UserResponse;
import com.kenzie.appserver.controller.model.UserUpdateRequest;
import com.kenzie.appserver.repositories.UserDao;
import com.kenzie.appserver.repositories.model.UserRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserDao userDao;

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

        UserResponse response = userService.getUserById("missing");

        assertNull(response);
    }

    /** ------------------------------------------------------------------------
     *  UserService.createUser
     *  ------------------------------------------------------------------------ **/

    @Test
    void createUser_validRequest_savesAndReturnsResponse() {
        CreateUserRequest request = new CreateUserRequest();
        request.setName("Jordan");
        request.setEmail("jordan@example.com");

        ArgumentCaptor<UserRecord> captor = ArgumentCaptor.forClass(UserRecord.class);

        UserResponse response = userService.createUser(request);

        verify(userDao).save(captor.capture());
        UserRecord saved = captor.getValue();

        assertNotNull(response);
        assertEquals("Jordan", response.getName());
        assertEquals("jordan@example.com", response.getEmail());
        assertDoesNotThrow(() -> UUID.fromString(response.getId()), "ID should be a valid UUID");
        assertEquals(saved.getId(), response.getId());
    }

    @Test
    void createUser_nullName_throwsBadRequest() {
        CreateUserRequest request = new CreateUserRequest();
        request.setName(null);
        request.setEmail("jordan@example.com");

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

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.createUser(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(userDao, never()).save(any());
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
        UserRecord saved = captor.getValue();

        assertEquals("user-2", saved.getId());
        assertEquals("New Name", saved.getName());
        assertEquals("new@example.com", saved.getEmail());
        assertEquals("New Name", response.getName());
        assertEquals("new@example.com", response.getEmail());
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
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.deleteUser("")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(userDao, never()).deleteById(any());
    }

    @Test
    void deleteUser_userDoesNotExist_throwsNotFound() {
        String id = UUID.randomUUID().toString();
        when(userDao.existsById(id)).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.deleteUser(id)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
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

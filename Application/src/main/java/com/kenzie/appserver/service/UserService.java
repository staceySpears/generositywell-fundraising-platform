package com.kenzie.appserver.service;

import com.kenzie.appserver.controller.model.CreateUserRequest;
import com.kenzie.appserver.controller.model.UserResponse;
import com.kenzie.appserver.controller.model.UserUpdateRequest;
import com.kenzie.appserver.repositories.UserDao;
import com.kenzie.appserver.repositories.model.UserRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class UserService {

    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    public UserResponse getUserById(String userId) {
        return userDao.findById(userId)
                .map(this::recordToResponse)
                .orElse(null);
    }

    public UserResponse createUser(CreateUserRequest request) {
        if (request.getName() == null || request.getEmail() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name and email are required");
        }
        UserRecord record = new UserRecord();
        record.setId(UUID.randomUUID().toString());
        record.setName(request.getName());
        record.setEmail(request.getEmail());
        userDao.save(record);

        return recordToResponse(record);
    }

    public void deleteUser(String userId) {
        if (userId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID cannot be empty");
        }
        if (!userDao.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        userDao.deleteById(userId);
    }

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

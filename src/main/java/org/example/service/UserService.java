package org.example.service;

import org.example.exception.UserNotFoundException;
import org.example.model.User;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserService {

    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    public User create(User user) {
        user.setId(idGen.getAndIncrement());
        users.put(user.getId(), user);
        return user;
    }

    public User getById(Long id) {
        return users.get(id);
    }

    public List<User> list() {
        return new ArrayList<>(users.values());
    }

    public String delete(Long id) {
        User removed = users.remove(id);
        if (removed == null) {
            throw new UserNotFoundException("The user doesn't exist: id=" + id);
        }
        return "deleted";
    }

    public User update(Long id, User user) {
        User existing = users.get(id);
        if (existing == null) {
            throw new UserNotFoundException("The user doesn't exist: id=" + id);
        }
        if (user.getName() != null) {
            existing.setName(user.getName());
        }
        if (user.getAge() != null) {
            existing.setAge(user.getAge());
        }
        users.put(id, existing);
        return existing;
    }

    public void reset() {
        users.clear();
        idGen.set(1);
    }
}
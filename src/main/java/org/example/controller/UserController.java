package org.example.controller;

import jakarta.validation.Valid;
import org.example.model.User;
import org.example.exception.UserNotFoundException;
import org.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public User create(@Valid @RequestBody User user) {
        return userService.create(user);
    }

    @GetMapping("/{id}")
    public User getById(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            throw new UserNotFoundException("The user doesn't exist: id=" + id);
        }
        return user;
    }

    @GetMapping
    public List<User> list() {
        return userService.list();
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        return userService.delete(id);
    }

    @PutMapping("/{id}")
    public User update(@PathVariable Long id,  @Valid @RequestBody User user) {
        return userService.update(id, user);
    }
}
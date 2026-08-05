package com.fooddelivery.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserRestController {

    private final UserStore userStore;

    public UserRestController(UserStore userStore) {
        this.userStore = userStore;
    }

    @GetMapping("/api/users/{id}")
    public ResponseEntity<UserStore.User> getUser(@PathVariable String id) {
        UserStore.User user = userStore.find(id);
        return user == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(user);
    }
}

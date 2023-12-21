package com.dyptan.controller;

import com.dyptan.gen.proto.FilterMessage;
import com.dyptan.model.User;
import com.dyptan.repository.UserRepository;
import com.dyptan.service.AuthService;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
public class UserController {

    Logger log = LogManager.getLogger(UserController.class);

    @Autowired
    UserRepository userRepository;

    @Autowired
    AuthService authService;

    @GetMapping("/users")
    public List<User> getAllUsers(){
        return userRepository.findAll();
    }

    @GetMapping("/user/{name}")
    public User getUser(@PathVariable(name="name") String name){
        return userRepository.findByUsername(name).orElseThrow(() -> new UsernameNotFoundException(name));
    }

    @GetMapping("/user/{name}/filter/{id}")
    public FilterMessage getUserFilterById(@PathVariable(name="name") String name,
                                          @PathVariable(name="id") int filterId){
        return userRepository.findByUsername(name)
                .map(user-> user.getFilters().get(filterId))
                .orElseThrow(() -> new UsernameNotFoundException(name));
    }

    @PutMapping("/user/{name}")
    public User updateUserPassword(@RequestBody User newUser, @PathVariable String name) {
        return userRepository.findByUsername(name)
                .map(
                        user -> {
                            user.setPassword(newUser.getPassword());
                            return userRepository.save(user);
                                }
                        )
                .orElseGet(
                        ()-> userRepository.save(newUser)
                        );
    }

    @DeleteMapping("/user/{name}")
    public void deleteUser(@PathVariable String name){
        userRepository.delete(getUser(name));
    }
}

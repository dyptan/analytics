package com.dyptan.controller;

import com.dyptan.model.Role;
import com.dyptan.model.User;
import com.dyptan.repository.UserRepository;
import com.dyptan.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class LoginController {
    Logger log = LogManager.getLogger(LoginController.class);
    @Autowired
    UserRepository userRepository;
    @Autowired
    AuthService authService;

    @GetMapping("/home")
    public String home(Model model, HttpSession httpSession, UsernamePasswordAuthenticationToken principal) {
        model.addAttribute("userName", principal.getName());
        httpSession.setAttribute("userName", principal.getName());
        return "home";
    }

    @GetMapping("/registration")
    public String registration() {
        return "registration";
    }

    @PostMapping("/registration")
    public String registration(@ModelAttribute("userForm") User userForm, BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "registration";
        }

        userForm.addRole(Role.Roles.USER);
        authService.saveEncrypted(userForm);

        return "redirect:/login";
    }


    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }
}


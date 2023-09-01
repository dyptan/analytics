package com.dyptan.controller;

import com.dyptan.model.Filter;
import com.dyptan.model.Role;
import com.dyptan.model.User;
import com.dyptan.repository.UserRepository;
import com.dyptan.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Controller
public class LoginController {
    Logger log = LogManager.getLogger(LoginController.class);
    @Autowired
    UserRepository userRepository;
    @Autowired
    ObjectMapper objectMapper;
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


    @PostMapping(value = "/search", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String searchWithfilter(@ModelAttribute Filter filter,
                                   @RequestParam(name="saveFilter", required=false, defaultValue="false") Boolean saveFilter,
                                   HttpSession session,
                                   Model model,
                                   UsernamePasswordAuthenticationToken principal) {

        log.info("Filter is built: "+filter);

        if (saveFilter) {

            log.info("Saving filter.");
            User user = userRepository
                    .findByUsername(session.getAttribute("userName").toString())
                    .orElseThrow(() -> new UsernameNotFoundException("Username not found"));
            user.addFilter(filter);
            log.debug("filter added: " + user.getFilters());
            userRepository.save(user);
        }

        return "search";
    }
}


package com.dyptan.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.Transient;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Component
@Table(name = "Users")
public class User {
    @Id
    private String username;
    private String password;
    @ElementCollection(fetch = FetchType.EAGER)
    private List<Filter> filters = new ArrayList<>();
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<Role.Roles> roles = new HashSet<>();

    public User() {
    }
    public User(User user) {
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.roles = user.getRoles();
        this.filters = user.getFilters();
    }

    public void addFilter(Filter filter) {
        filters.add(filter);
    }
    public void addRole(Role.Roles role) {
        roles.add(role);
    }
    public void deleteFilter(int id) {
        filters.remove(id);
    }
    public String getUsername() {
        return this.username;
    }
    public String getPassword() {
        return this.password;
    }
    public List<Filter> getFilters() {
        return this.filters;
    }
    public Set<Role.Roles> getRoles() {
        return this.roles;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void setFilters(List<Filter> filters) {
        this.filters = filters;
    }


    public String toString() {
        return "User(username=" + this.getUsername() + ", password=" + this.getPassword() + ", filters=" + this.getFilters() + ", roles=" + this.getRoles() + ")";
    }

    @Transient
    public static class AuthDetails extends User implements UserDetails {

        public AuthDetails(User user) {
            super(user);
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return getRoles()
                    .stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getValue()))
                    .collect(Collectors.toList());
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }
    }


}

package com.dyptan.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Table;

@Embeddable
@Table(name = "Roles")
public class Role {
    private Roles role;
    public Role() {
    }
    public Roles getRole() {
        return this.role;
    }
    public void setRole(Roles role) {
        this.role = role;
    }
    public String toString() {
        return "Role(role=" + this.getRole() + ")";
    }

    public enum Roles {
        ADMIN("ADMIN"),
        USER("USER");
        private final String value;
        Roles(String value) {
            this.value = value;
        }
        public String getValue() {
            return value;
        }
    }

}

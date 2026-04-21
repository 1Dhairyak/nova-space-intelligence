package com.nova.model;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String displayName;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime lastLogin;

    public User() {}

    // ─── Builder ──────────────────────────────────────────────────────────────
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id; private String email, password, displayName;
        private Role role = Role.USER;
        private LocalDateTime createdAt = LocalDateTime.now(), lastLogin;

        public Builder id(Long v) { id = v; return this; }
        public Builder email(String v) { email = v; return this; }
        public Builder password(String v) { password = v; return this; }
        public Builder displayName(String v) { displayName = v; return this; }
        public Builder role(Role v) { role = v; return this; }
        public Builder createdAt(LocalDateTime v) { createdAt = v; return this; }
        public Builder lastLogin(LocalDateTime v) { lastLogin = v; return this; }

        public User build() {
            User u = new User();
            u.id = id; u.email = email; u.password = password;
            u.displayName = displayName; u.role = role;
            u.createdAt = createdAt; u.lastLogin = lastLogin;
            return u;
        }
    }

    // ─── Getters / Setters ────────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { email = v; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String v) { displayName = v; }
    public Role getRole() { return role; }
    public void setRole(Role v) { role = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { createdAt = v; }
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime v) { lastLogin = v; }

    // ─── UserDetails ──────────────────────────────────────────────────────────
    @Override public String getUsername() { return email; }
    @Override public String getPassword() { return password; }
    public void setPassword(String v) { password = v; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }

    public enum Role { USER, ADMIN }
}

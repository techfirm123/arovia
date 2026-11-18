package com.arovia.api.identity.infrastructure;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.arovia.api.identity.application.IdentityAuthenticatedUser;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class IdentityUserPrincipal
        implements UserDetails,
        CredentialsContainer,
        IdentityAuthenticatedUser,
        Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID userAccountId;
    private final String email;
    private final String normalizedEmail;
    private final String displayName;
    private final String accountStatus;
    private final boolean emailVerified;

    /*
     * ProviderManager erases credentials after successful authentication.
     * This prevents the password hash from remaining in the serialized
     * Spring Session security context.
     */
    private String passwordHash;

    public IdentityUserPrincipal(
            UUID userAccountId,
            String email,
            String normalizedEmail,
            String displayName,
            String passwordHash,
            String accountStatus,
            boolean emailVerified
    ) {
        this.userAccountId = userAccountId;
        this.email = email;
        this.normalizedEmail = normalizedEmail;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.accountStatus = accountStatus;
        this.emailVerified = emailVerified;
    }

    @Override
    public UUID getUserAccountId() {
        return userAccountId;
    }

    @Override
    public String getEmail() {
        return email;
    }

    public String getNormalizedEmail() {
        return normalizedEmail;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean isEmailVerified() {
        return emailVerified;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        /*
         * Initial platform-wide authority.
         * Organization-specific permissions will be resolved through
         * OrganizationMembership in AROVIA-002C.
         */
        return List.of(
                new SimpleGrantedAuthority("ROLE_USER")
        );
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return normalizedEmail;
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
        return "ACTIVE".equals(accountStatus)
                && emailVerified;
    }

    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }
}
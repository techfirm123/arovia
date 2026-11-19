package com.arovia.api.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.arovia.api.organization.application.port.OrganizationMembershipQueryPort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationContextResolverTest {

    @Mock
    private OrganizationMembershipQueryPort queryPort;

    private OrganizationMembershipService membershipService;

    private OrganizationContextResolver contextResolver;

    private UUID userId;
    private UUID organizationId;
    private UUID membershipId;

    @BeforeEach
    void setUp() {
        membershipService = new OrganizationMembershipService(queryPort);
        contextResolver = new OrganizationContextResolver(membershipService);

        userId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
        membershipId = UUID.randomUUID();
    }

    @Test
    void shouldResolveContextForActiveMembership() {
        OrganizationMembershipView membership =
                new OrganizationMembershipView(
                        organizationId,
                        "Acme Technologies",
                        "ACTIVE",
                        "Acme Technologies",
                        membershipId,
                        "ACTIVE",
                        Instant.parse("2026-11-19T10:00:00Z"),
                        List.of("OWNER")
                );

        when(queryPort.findActiveOrganization(userId, organizationId))
                .thenReturn(java.util.Optional.of(membership));

        OrganizationContext context = contextResolver.resolve(
                userId,
                organizationId
        );

        assertThat(context.userAccountId()).isEqualTo(userId);
        assertThat(context.organizationId()).isEqualTo(organizationId);
        assertThat(context.membershipId()).isEqualTo(membershipId);
        assertThat(context.organizationName())
                .isEqualTo("Acme Technologies");
        assertThat(context.roles()).containsExactly("OWNER");
        assertThat(context.hasRole("OWNER")).isTrue();

        verify(queryPort).findActiveOrganization(userId, organizationId);
    }

    @Test
    void shouldRejectOrganizationWithoutActiveMembership() {
        when(queryPort.findActiveOrganization(userId, organizationId))
                .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(
                () -> contextResolver.resolve(userId, organizationId)
        ).isInstanceOf(OrganizationMembershipNotFoundException.class);
    }

    @Test
    void shouldRejectMissingUserId() {
        assertThatThrownBy(
                () -> contextResolver.resolve(null, organizationId)
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectMissingOrganizationId() {
        assertThatThrownBy(
                () -> contextResolver.resolve(userId, null)
        ).isInstanceOf(NullPointerException.class);
    }
}
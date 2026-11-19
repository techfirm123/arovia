
package com.arovia.api.organization.application;

import java.time.Instant;
import java.util.UUID;

import com.arovia.api.organization.application.port.OrganizationProvisioningPort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationProvisioningServiceTest {

    @Mock
    private OrganizationProvisioningPort provisioningPort;

    private OrganizationProvisioningService service;

    private UUID authenticatedUserId;

    @BeforeEach
    void setUp() {
        service = new OrganizationProvisioningService(provisioningPort);
        authenticatedUserId = UUID.randomUUID();
    }

    @Test
    void shouldProvisionOrganizationForVerifiedUser() {
        CreateOrganizationCommand command =
                new CreateOrganizationCommand(
                        "  Arovia Demo Store  ",
                        null,
                        null
                );

        UUID organizationId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-11-19T08:00:00Z");

        OrganizationProvisioningResult expected =
                new OrganizationProvisioningResult(
                        organizationId,
                        "Arovia Demo Store",
                        "ACTIVE",
                        membershipId,
                        "ACTIVE",
                        "OWNER",
                        createdAt
                );

        when(provisioningPort.provision(
                authenticatedUserId,
                new CreateOrganizationCommand(
                        "Arovia Demo Store",
                        null,
                        null
                )
        )).thenReturn(expected);

        OrganizationProvisioningResult result =
                service.createOrganization(
                        authenticatedUserId,
                        true,
                        command
                );

        assertEquals(expected, result);

        verify(provisioningPort).provision(
                authenticatedUserId,
                new CreateOrganizationCommand(
                        "Arovia Demo Store",
                        null,
                        null
                )
        );
    }

    @Test
    void shouldRejectUnverifiedUser() {
        CreateOrganizationCommand command =
                new CreateOrganizationCommand(
                        "Arovia Demo Store",
                        null,
                        null
                );

        assertThrows(
                OrganizationProvisioningNotAllowedException.class,
                () -> service.createOrganization(
                        authenticatedUserId,
                        false,
                        command
                )
        );

        verify(provisioningPort, never()).provision(
                authenticatedUserId,
                command
        );
    }

    @Test
    void shouldRejectBlankOrganizationName() {
        CreateOrganizationCommand command =
                new CreateOrganizationCommand(
                        "   ",
                        null,
                        null
                );

        assertThrows(
                InvalidOrganizationRequestException.class,
                () -> service.createOrganization(
                        authenticatedUserId,
                        true,
                        command
                )
        );

        verify(provisioningPort, never()).provision(
                authenticatedUserId,
                command
        );
    }

    @Test
    void shouldRejectOrganizationNameLongerThan160Characters() {
        CreateOrganizationCommand command =
                new CreateOrganizationCommand(
                        "A".repeat(161),
                        null,
                        null
                );

        assertThrows(
                InvalidOrganizationRequestException.class,
                () -> service.createOrganization(
                        authenticatedUserId,
                        true,
                        command
                )
        );

        verify(provisioningPort, never()).provision(
                authenticatedUserId,
                command
        );
    }

    @Test
    void shouldPassAuthenticatedUserIdRatherThanRequestSuppliedIdentity() {
        UUID anotherUserId = UUID.randomUUID();

        CreateOrganizationCommand command =
                new CreateOrganizationCommand(
                        "Arovia Demo Store",
                        null,
                        null
                );

        OrganizationProvisioningResult expected =
                new OrganizationProvisioningResult(
                        UUID.randomUUID(),
                        "Arovia Demo Store",
                        "ACTIVE",
                        UUID.randomUUID(),
                        "ACTIVE",
                        "OWNER",
                        Instant.now()
                );

        when(provisioningPort.provision(
                authenticatedUserId,
                command
        )).thenReturn(expected);

        service.createOrganization(
                authenticatedUserId,
                true,
                command
        );

        ArgumentCaptor<UUID> userIdCaptor =
                ArgumentCaptor.forClass(UUID.class);

        verify(provisioningPort).provision(
                userIdCaptor.capture(),
                org.mockito.ArgumentMatchers.any()
        );

        assertEquals(authenticatedUserId, userIdCaptor.getValue());
        assertTrue(!authenticatedUserId.equals(anotherUserId));
    }

    @Test
    void shouldRejectNullAuthenticatedUserId() {
        CreateOrganizationCommand command =
                new CreateOrganizationCommand(
                        "Arovia Demo Store",
                        null,
                        null
                );

        assertThrows(
                NullPointerException.class,
                () -> service.createOrganization(
                        null,
                        true,
                        command
                )
        );

        verify(provisioningPort, never()).provision(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }
}

package com.arovia.api.security.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;

class ClientAddressResolverTest {

    @Test
    void ignoresForwardedHeaderWhenConnectionIsNotFromTrustedProxy() {
        ClientAddressResolver resolver = createResolver();

        MockHttpServletRequest request = requestFrom("198.51.100.10");

        request.addHeader(
                "X-Forwarded-For",
                "203.0.113.99"
        );

        assertEquals(
                "198.51.100.10",
                resolver.resolve(request)
        );
    }

    @Test
    void resolvesClientAddressFromTrustedProxy() {
        ClientAddressResolver resolver = createResolver(
                "10.0.0.0/24"
        );

        MockHttpServletRequest request = requestFrom("10.0.0.10");

        request.addHeader(
                "X-Forwarded-For",
                "198.51.100.25"
        );

        assertEquals(
                "198.51.100.25",
                resolver.resolve(request)
        );
    }

    @Test
    void ignoresSpoofedAddressPrependedToForwardedChain() {
        ClientAddressResolver resolver = createResolver(
                "10.0.0.0/24"
        );

        MockHttpServletRequest request = requestFrom("10.0.0.10");

        /*
         * The first value may have been supplied by an attacker.
         * The trusted proxy appends the address it received.
         */
        request.addHeader(
                "X-Forwarded-For",
                "203.0.113.99, 198.51.100.25"
        );

        assertEquals(
                "198.51.100.25",
                resolver.resolve(request)
        );
    }

    @Test
    void resolvesClientThroughMultipleTrustedProxies() {
        ClientAddressResolver resolver = createResolver(
                "10.0.0.0/24"
        );

        MockHttpServletRequest request = requestFrom("10.0.0.10");

        request.addHeader(
                "X-Forwarded-For",
                "198.51.100.25, 10.0.0.20"
        );

        assertEquals(
                "198.51.100.25",
                resolver.resolve(request)
        );
    }

    @Test
    void returnsSocketPeerWhenForwardedHeaderIsMissing() {
        ClientAddressResolver resolver = createResolver(
                "10.0.0.0/24"
        );

        MockHttpServletRequest request = requestFrom("10.0.0.10");

        assertEquals(
                "10.0.0.10",
                resolver.resolve(request)
        );
    }

    @Test
    void fallsBackToSocketPeerWhenForwardedHeaderIsMalformed() {
        ClientAddressResolver resolver = createResolver(
                "10.0.0.0/24"
        );

        MockHttpServletRequest request = requestFrom("10.0.0.10");

        request.addHeader(
                "X-Forwarded-For",
                "198.51.100.25, not-an-ip-address"
        );

        assertEquals(
                "10.0.0.10",
                resolver.resolve(request)
        );
    }

    @Test
    void fallsBackToSocketPeerWhenForwardedHeaderContainsHostname() {
        ClientAddressResolver resolver = createResolver(
                "10.0.0.0/24"
        );

        MockHttpServletRequest request = requestFrom("10.0.0.10");

        request.addHeader(
                "X-Forwarded-For",
                "attacker.example.com"
        );

        assertEquals(
                "10.0.0.10",
                resolver.resolve(request)
        );
    }

    @Test
    void fallsBackToSocketPeerWhenForwardedChainIsTooLong() {
        ClientAddressResolver resolver = createResolver(
                "10.0.0.0/24"
        );

        MockHttpServletRequest request = requestFrom("10.0.0.10");

        String forwardedChain = String.join(
                ", ",
                java.util.Collections.nCopies(
                        17,
                        "198.51.100.25"
                )
        );

        request.addHeader(
                "X-Forwarded-For",
                forwardedChain
        );

        assertEquals(
                "10.0.0.10",
                resolver.resolve(request)
        );
    }

    @Test
    void processesMultipleForwardedHeaderValuesInOrder() {
        ClientAddressResolver resolver = createResolver(
                "10.0.0.0/24"
        );

        MockHttpServletRequest request = requestFrom("10.0.0.10");

        request.addHeader(
                "X-Forwarded-For",
                "198.51.100.25"
        );

        request.addHeader(
                "X-Forwarded-For",
                "10.0.0.20"
        );

        assertEquals(
                "198.51.100.25",
                resolver.resolve(request)
        );
    }

    @Test
    void matchesTrustedIpv6ProxyNetwork() throws UnknownHostException {
        ClientAddressResolver resolver = createResolver(
                "2001:db8:1::/64"
        );

        MockHttpServletRequest request =
                requestFrom("2001:db8:1::10");

        request.addHeader(
                "X-Forwarded-For",
                "2001:db8:2::5"
        );

        String expected = InetAddress
                .getByName("2001:db8:2::5")
                .getHostAddress();

        assertEquals(
                expected,
                resolver.resolve(request)
        );
    }

    @Test
    void rejectsInvalidTrustedProxyConfiguration() {
        ClientAddressProperties properties =
                new ClientAddressProperties();

        properties.setTrustedProxies(
                List.of("not-a-valid-cidr")
        );

        assertThrows(
                IllegalStateException.class,
                () -> new ClientAddressResolver(properties)
        );
    }

    @Test
    void rejectsTrustedProxyPrefixOutsideAddressRange() {
        ClientAddressProperties properties =
                new ClientAddressProperties();

        properties.setTrustedProxies(
                List.of("10.0.0.0/33")
        );

        assertThrows(
                IllegalStateException.class,
                () -> new ClientAddressResolver(properties)
        );
    }

    @Test
    void rejectsTooManyConfiguredTrustedNetworks() {
        ClientAddressProperties properties =
                new ClientAddressProperties();

        properties.setTrustedProxies(
                java.util.Collections.nCopies(
                        129,
                        "10.0.0.0/24"
                )
        );

        assertThrows(
                IllegalStateException.class,
                () -> new ClientAddressResolver(properties)
        );
    }

    private ClientAddressResolver createResolver(
            String... trustedProxies
    ) {
        ClientAddressProperties properties =
                new ClientAddressProperties();

        properties.setTrustedProxies(
                List.of(trustedProxies)
        );

        return new ClientAddressResolver(properties);
    }

    private MockHttpServletRequest requestFrom(String remoteAddress) {
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setRemoteAddr(remoteAddress);

        return request;
    }
}
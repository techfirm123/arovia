package com.arovia.api.security.client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

@Component
public class ClientAddressResolver {

    private static final String X_FORWARDED_FOR_HEADER =
            "X-Forwarded-For";

    private static final int MAX_FORWARDED_HEADER_LENGTH = 2_048;

    private static final int MAX_FORWARDED_HOPS = 16;

    private static final int MAX_CONFIGURED_TRUSTED_NETWORKS = 128;

    private static final Pattern IPV6_CHARACTERS =
            Pattern.compile("[0-9A-Fa-f:.]+");

    private final List<IpNetwork> trustedProxyNetworks;

    public ClientAddressResolver(
            ClientAddressProperties properties
    ) {
        if (properties.getTrustedProxies() == null) {
            throw new IllegalStateException(
                    "Trusted proxy configuration cannot be null."
            );
        }

        if (properties.getTrustedProxies().size()
                > MAX_CONFIGURED_TRUSTED_NETWORKS) {
            throw new IllegalStateException(
                    "Too many trusted proxy networks are configured."
            );
        }

        List<IpNetwork> networks = new ArrayList<>();

        for (String configuredNetwork : properties.getTrustedProxies()) {
            if (configuredNetwork == null
                    || configuredNetwork.isBlank()) {
                continue;
            }

            networks.add(parseNetwork(configuredNetwork));
        }

        this.trustedProxyNetworks = List.copyOf(networks);
    }

    /**
     * Resolves the client address using the immediate socket peer
     * and, only when that peer is trusted, its X-Forwarded-For chain.
     *
     * The application server must be configured with
     * server.forward-headers-strategy=none so getRemoteAddr()
     * continues to represent the immediate network peer.
     */
    public String resolve(HttpServletRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "HTTP servlet request is required."
            );
        }

        String remoteAddress = request.getRemoteAddr();

        if (remoteAddress == null || remoteAddress.isBlank()) {
            return "unknown";
        }

        IpAddress socketPeer = parseAddress(remoteAddress);

        /*
         * If the immediate peer cannot be parsed, retain the value
         * supplied by the servlet container rather than trusting
         * any forwarding header.
         */
        if (socketPeer == null) {
            return remoteAddress;
        }

        /*
         * Forwarded headers are ignored unless the immediate
         * network peer is explicitly trusted.
         */
        if (!isTrustedProxy(socketPeer)) {
            return socketPeer.toText();
        }

        List<IpAddress> forwardedAddresses =
                readForwardedAddresses(request);

        /*
         * Missing or malformed forwarding information causes a
         * conservative fallback to the immediate peer.
         */
        if (forwardedAddresses == null
                || forwardedAddresses.isEmpty()) {
            return socketPeer.toText();
        }

        /*
         * X-Forwarded-For ordering:
         *
         * client, proxy-1, proxy-2
         *
         * The direct socket peer is proxy-3.
         *
         * Walk from right to left, trusting only addresses
         * that belong to configured proxy networks.
         */
        IpAddress candidate = socketPeer;

        for (int index = forwardedAddresses.size() - 1;
             index >= 0;
             index--) {

            /*
             * Once an untrusted address is reached, it is the
             * first untrusted hop from the application outward.
             *
             * Anything to its left may have been supplied by
             * an untrusted client and must be ignored.
             */
            if (!isTrustedProxy(candidate)) {
                return candidate.toText();
            }

            candidate = forwardedAddresses.get(index);
        }

        /*
         * Handle the final forwarded address.
         */
        if (!isTrustedProxy(candidate)) {
            return candidate.toText();
        }

        /*
         * If every reported hop is trusted, there is no
         * demonstrably untrusted client address in the chain.
         * Use the immediate peer conservatively.
         */
        return socketPeer.toText();
    }

    private List<IpAddress> readForwardedAddresses(
            HttpServletRequest request
    ) {
        Enumeration<String> headerValues =
                request.getHeaders(X_FORWARDED_FOR_HEADER);

        if (headerValues == null || !headerValues.hasMoreElements()) {
            return List.of();
        }

        List<IpAddress> addresses = new ArrayList<>();

        int totalHeaderLength = 0;

        while (headerValues.hasMoreElements()) {
            String headerValue = headerValues.nextElement();

            if (headerValue == null) {
                return null;
            }

            totalHeaderLength += headerValue.length();

            if (totalHeaderLength > MAX_FORWARDED_HEADER_LENGTH) {
                return null;
            }

            String[] values = headerValue.split(",", -1);

            for (String value : values) {
                String addressValue = value.trim();

                if (addressValue.isEmpty()) {
                    return null;
                }

                IpAddress address = parseAddress(addressValue);

                if (address == null) {
                    return null;
                }

                addresses.add(address);

                if (addresses.size() > MAX_FORWARDED_HOPS) {
                    return null;
                }
            }
        }

        return addresses;
    }

    private boolean isTrustedProxy(IpAddress address) {
        for (IpNetwork network : trustedProxyNetworks) {
            if (network.contains(address)) {
                return true;
            }
        }

        return false;
    }

    private static IpNetwork parseNetwork(String configuredValue) {
        String value = configuredValue.trim();

        int separator = value.indexOf('/');

        if (separator != value.lastIndexOf('/')) {
            throw invalidTrustedProxy(value);
        }

        String addressValue;
        String prefixValue = null;

        if (separator >= 0) {
            addressValue = value.substring(0, separator);
            prefixValue = value.substring(separator + 1);
        } else {
            addressValue = value;
        }

        IpAddress address = parseAddress(addressValue);

        if (address == null) {
            throw invalidTrustedProxy(value);
        }

        int addressBits = address.bytes().length * 8;

        int prefixLength;

        if (prefixValue == null) {
            prefixLength = addressBits;
        } else {
            if (prefixValue.isEmpty()
                    || !prefixValue.chars().allMatch(Character::isDigit)) {
                throw invalidTrustedProxy(value);
            }

            try {
                prefixLength = Integer.parseInt(prefixValue);
            } catch (NumberFormatException exception) {
                throw invalidTrustedProxy(value);
            }
        }

        if (prefixLength < 0 || prefixLength > addressBits) {
            throw invalidTrustedProxy(value);
        }

        byte[] networkAddress = address.bytes().clone();

        applyNetworkMask(networkAddress, prefixLength);

        return new IpNetwork(networkAddress, prefixLength);
    }

    private static void applyNetworkMask(
            byte[] address,
            int prefixLength
    ) {
        for (int index = 0; index < address.length; index++) {
            int remainingBits = prefixLength - index * 8;

            if (remainingBits >= 8) {
                continue;
            }

            if (remainingBits <= 0) {
                address[index] = 0;
                continue;
            }

            int mask = 0xFF << (8 - remainingBits);

            address[index] = (byte) (address[index] & mask);
        }
    }

    private static IpAddress parseAddress(String suppliedAddress) {
        if (suppliedAddress == null || suppliedAddress.isBlank()) {
            return null;
        }

        String value = suppliedAddress.trim();

        if (value.indexOf(':') < 0) {
            return parseIpv4(value);
        }

        /*
         * Restrict IPv6 input to numeric characters.
         * This prevents DNS names or zone identifiers from being
         * accepted by InetAddress.getByName().
         */
        if (!IPV6_CHARACTERS.matcher(value).matches()) {
            return null;
        }

        /*
         * Validate an embedded IPv4 suffix explicitly.
         */
        if (value.indexOf('.') >= 0) {
            int lastColon = value.lastIndexOf(':');

            if (lastColon < 0
                    || parseIpv4(value.substring(lastColon + 1)) == null) {
                return null;
            }
        }

        try {
            byte[] addressBytes = InetAddress.getByName(value).getAddress();

            if (addressBytes.length != 4 && addressBytes.length != 16) {
                return null;
            }

            /*
             * Normalize IPv4-mapped IPv6 addresses to IPv4 so they
             * match the same IPv4 trusted network definitions.
             */
            if (isIpv4MappedIpv6(addressBytes)) {
                byte[] ipv4Address = new byte[4];

                System.arraycopy(
                        addressBytes,
                        12,
                        ipv4Address,
                        0,
                        4
                );

                addressBytes = ipv4Address;
            }

            return new IpAddress(addressBytes);

        } catch (UnknownHostException exception) {
            return null;
        }
    }

    private static boolean isIpv4MappedIpv6(byte[] address) {
        if (address.length != 16) {
            return false;
        }

        for (int index = 0; index < 10; index++) {
            if (address[index] != 0) {
                return false;
            }
        }

        return (address[10] & 0xFF) == 0xFF
                && (address[11] & 0xFF) == 0xFF;
    }

    private static IpAddress parseIpv4(String value) {
        String[] parts = value.split("\\.", -1);

        if (parts.length != 4) {
            return null;
        }

        byte[] address = new byte[4];

        for (int index = 0; index < 4; index++) {
            String part = parts[index];

            if (part.isEmpty() || part.length() > 3) {
                return null;
            }

            if (!part.chars().allMatch(Character::isDigit)) {
                return null;
            }

            int octet;

            try {
                octet = Integer.parseInt(part);
            } catch (NumberFormatException exception) {
                return null;
            }

            if (octet < 0 || octet > 255) {
                return null;
            }

            address[index] = (byte) octet;
        }

        return new IpAddress(address);
    }

    private static IllegalStateException invalidTrustedProxy(
            String configuredValue
    ) {
        return new IllegalStateException(
                "Invalid trusted proxy CIDR configured: "
                        + configuredValue
        );
    }

    private record IpAddress(byte[] bytes) {

        private IpAddress {
            bytes = bytes.clone();
        }

        private String toText() {
            try {
                return InetAddress.getByAddress(bytes).getHostAddress();
            } catch (UnknownHostException exception) {
                throw new IllegalStateException(
                        "Unable to format parsed IP address.",
                        exception
                );
            }
        }
    }

    private static final class IpNetwork {

        private final byte[] networkAddress;
        private final int prefixLength;

        private IpNetwork(
                byte[] networkAddress,
                int prefixLength
        ) {
            this.networkAddress = networkAddress.clone();
            this.prefixLength = prefixLength;
        }

        private boolean contains(IpAddress candidate) {
            byte[] candidateAddress = candidate.bytes();

            if (candidateAddress.length != networkAddress.length) {
                return false;
            }

            int completeBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;

            for (int index = 0; index < completeBytes; index++) {
                if (candidateAddress[index] != networkAddress[index]) {
                    return false;
                }
            }

            if (remainingBits == 0) {
                return true;
            }

            int mask = 0xFF << (8 - remainingBits);

            return (candidateAddress[completeBytes] & mask)
                    == (networkAddress[completeBytes] & mask);
        }
    }
}
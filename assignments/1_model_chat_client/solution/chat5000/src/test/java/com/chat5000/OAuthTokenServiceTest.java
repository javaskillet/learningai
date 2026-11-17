package com.chat5000;

import com.chat5000.config.SecurityProperties;
import com.chat5000.security.OAuthTokenService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OAuthTokenServiceTest {

    @Test
    void issuesAndVerifiesValidBearerToken() {
        SecurityProperties props = new SecurityProperties();
        OAuthTokenService service = new OAuthTokenService(props);

        assertTrue(service.validateClientCredentials("chat5000-client", "chat5000-secret"));
        assertFalse(service.validateClientCredentials("chat5000-client", "wrong"));

        String token = service.issueToken("chat5000-client", "chat:read chat:write mcp:tools");
        Optional<OAuthTokenService.AuthenticatedPrincipal> principal = service.verifyBearerToken(token);

        assertTrue(principal.isPresent());
        assertEquals("chat5000-client", principal.get().subject());
        assertTrue(principal.get().hasScope("mcp:tools"));
    }

    @Test
    void rejectsTamperedOrInvalidToken() {
        SecurityProperties props = new SecurityProperties();
        OAuthTokenService service = new OAuthTokenService(props);

        String token = service.issueToken("chat5000-client", "chat:read");
        String tampered = token.substring(0, token.length() - 4) + "xxxx";

        assertTrue(service.verifyBearerToken(tampered).isEmpty());
        assertTrue(service.verifyBearerToken("not-a-jwt").isEmpty());
        assertTrue(service.verifyBearerToken("").isEmpty());
    }
}

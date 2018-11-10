package org.athento.nuxeo.security.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator.Builder;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Payload;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/**
 * JWT helper.
 */
public final class JWTHelper {

    /** Log. */
    private static final Log LOG = LogFactory.getLog(JWTHelper.class);

    private static final String JWT_SECRET_PARAM = "athento.jwt.secret";

    private static final String DEFAULT_SECRET = "lolailolaaflamencado";
    private static final String DEFAULT_ISSUER = "athento";

    /**
     * Sing token.
     *
     * @param issuer
     * @param subject
     * @return
     */
    public static String signToken(String issuer, String subject, Date expiration, Pair<String, Object> claim) {
        Builder builder = JWT.create();
        if (issuer == null) {
            issuer = DEFAULT_ISSUER;
        }
        builder.withIssuer(issuer);
        if (subject != null) {
            builder.withSubject(subject);
        }
        if (expiration != null) {
            builder.withExpiresAt(expiration);
        }
        if (claim != null) {
            String name = claim.getKey();
            Object value = claim.getValue();
            if (value instanceof Boolean) {
                builder.withClaim(name, (Boolean) value);
            } else if (value instanceof Date) {
                builder.withClaim(name, (Date) value);
            } else if (value instanceof Double) {
                builder.withClaim(name, (Double) value);
            } else if (value instanceof Integer) {
                builder.withClaim(name, (Integer) value);
            } else if (value instanceof Long) {
                builder.withClaim(name, (Long) value);
            } else if (value instanceof String) {
                builder.withClaim(name, (String) value);
            } else if (value instanceof Integer[]) {
                builder.withArrayClaim(name, (Integer[]) value);
            } else if (value instanceof Long[]) {
                builder.withArrayClaim(name, (Long[]) value);
            } else if (value instanceof String[]) {
                builder.withArrayClaim(name, (String[]) value);
            }
        }
        return builder.sign(getHmacAlgorithm());
    }

    /**
     * Verify a token.
     *
     * @param token
     * @param issuer
     * @return
     */
    public static Map<String, Claim> verifyToken(String token, String issuer) {
        Objects.requireNonNull(token);
        Algorithm algorithm = getHmacAlgorithm();
        if (algorithm == null) {
            LOG.warn("Configure param " + JWT_SECRET_PARAM + " in your nuxeo.conf file");
            return null;
        }
        if (issuer == null) {
            issuer = DEFAULT_ISSUER;
        }
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .build();
        DecodedJWT jwt;
        try {
            jwt = verifier.verify(token);
            Payload payload = getFieldValue(jwt, "payload");
            return payload.getClaims();
        } catch (JWTVerificationException e) {
            LOG.warn("JWT Token verification failed: " + e.toString());
            return null;
        }
    }

    protected static <T> T getFieldValue(Object object, String name) {
        try {
            Field field = object.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(object);
        } catch (ReflectiveOperationException | SecurityException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get JWT Algorithm.
     *
     * @return
     */
    private static Algorithm getHmacAlgorithm() {
        String secret = Framework.getProperty(JWT_SECRET_PARAM, DEFAULT_SECRET);
        if (secret == null) {
            return null;
        }
        return Algorithm.HMAC512(secret);
    }


}

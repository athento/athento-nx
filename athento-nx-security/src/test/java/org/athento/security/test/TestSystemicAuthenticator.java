package org.athento.security.test;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.security.authenticator.SystemicDelegatedAuthenticator;
import org.athento.nuxeo.security.util.JWTHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.core.api.local.LoginStack;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestSystemicAuthenticator {

    private static final Log LOG = LogFactory.getLog(TestSystemicAuthenticator.class);

    protected static final String USERNAME = "paco";

    protected static final String BEARER_SP = "Bearer";


    @Before
    public void setUp() throws Exception {
        LoginStack loginStack = ClientLoginModule.getThreadLocalLogin();
        loginStack.push(new UserPrincipal(USERNAME, Collections.emptyList(), false, false), null, null);
    }

    @After
    public void teardown() throws Exception {
        LoginStack loginStack = ClientLoginModule.getThreadLocalLogin();
        loginStack.pop();
    }

    @Test
    public void testValidateTicket() throws Exception {
        SystemicDelegatedAuthenticator auth = new SystemicDelegatedAuthenticator();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        // test with a valid ticket
        String ticket = JWTHelper.signToken("athento", "user", null, Pair.of("delegatedUser", USERNAME));
        when(request.getHeader(eq(AUTHORIZATION))).thenReturn(BEARER_SP + " " + ticket);

        UserIdentificationInfo uii = auth.handleRetrieveIdentity(request, response);

        // auth plugin succeeds
        assertNotNull(uii);
        assertEquals(USERNAME, uii.getUserName());
    }

}

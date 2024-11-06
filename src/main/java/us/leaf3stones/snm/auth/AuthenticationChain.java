package us.leaf3stones.snm.auth;

import us.leaf3stones.snm.common.HttpSecPeer;
import us.leaf3stones.snm.server.HttpSecServer;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class AuthenticationChain {
    private final ArrayList<Constructor<? extends Authenticator>> activatedAuthenticators;

    @SafeVarargs
    public AuthenticationChain(Class<? extends Authenticator>... authenticators) throws NoSuchMethodException {
        activatedAuthenticators = new ArrayList<>();
        for (Class<? extends Authenticator> authClass : authenticators) {
            activatedAuthenticators.add(authClass.getConstructor(HttpSecServer.class));
        }
    }

    public void authenticate(HttpSecPeer client) throws Authenticator.AuthenticationException, IOException {
        for (Constructor<? extends Authenticator> authConstructor : activatedAuthenticators) {
            try {
                Authenticator authenticator = authConstructor.newInstance(client);
                authenticator.authenticate();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new Authenticator.AuthenticationException("failed to construct authenticator. " + e);
            }
        }
    }

}

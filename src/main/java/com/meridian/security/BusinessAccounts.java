package com.meridian.security;

import com.meridian.library.domain.Business;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BusinessAccounts {

    static final Map<String, Business> USERS = Map.of(
            "nordlicht", new Business("nordlicht", "Nordlicht Versand (sample business)"),
            "weserkontor", new Business("weserkontor", "Weserkontor Handel (sample business)"));

    public Business businessOf(String user) {
        Business business = USERS.get(user);
        if (business == null) {
            throw new IllegalStateException("no business for user " + user);
        }
        return business;
    }
}

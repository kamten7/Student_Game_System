package org.jingxu.pojo;

import java.util.Set;

public record Actor(long id, String username, Set<String> roles) {
    public boolean has(String role) {
        return roles.contains(role);
    }

    public boolean managesEvents() {
        return has("ADMIN") || has("ORGANIZER");
    }

    public String home() {
        if (managesEvents()) return "/admin";
        if (has("JUDGE")) return "/judge";
        if (has("STUDENT")) return "/student";
        return "/";
    }
}

package org.jingxu.pojo;

import java.util.Set;

/** 保存当前账号的编号、登录名和有效角色。 */
public record Actor(long id, String username, Set<String> roles) {
    /** 判断账号是否持有指定角色。 */
    public boolean has(String role) {
        return roles.contains(role);
    }

    /** 判断账号能否进入赛事管理。 */
    public boolean managesEvents() {
        return has("ADMIN") || has("ORGANIZER");
    }

    /** 按角色优先级选择登录后的工作台。 */
    public String home() {
        if (managesEvents()) return "/admin";
        if (has("JUDGE")) return "/judge";
        if (has("STUDENT")) return "/student";
        return "/";
    }
}

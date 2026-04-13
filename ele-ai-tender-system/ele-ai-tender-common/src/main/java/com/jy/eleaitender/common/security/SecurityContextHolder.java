package com.jy.eleaitender.common.security;

public class SecurityContextHolder {

    private static final ThreadLocal<LoginUser> CONTEXT = new ThreadLocal<>();

    private SecurityContextHolder() {
    }

    public static void setLoginUser(LoginUser loginUser) {
        CONTEXT.set(loginUser);
    }

    public static LoginUser getLoginUser() {
        return CONTEXT.get();
    }

    public static Long getUserId() {
        LoginUser user = getLoginUser();
        return user != null ? user.getUserId() : null;
    }

    public static String getUsername() {
        LoginUser user = getLoginUser();
        return user != null ? user.getUsername() : null;
    }

    public static String getRealName() {
        LoginUser user = getLoginUser();
        return user != null ? user.getRealName() : null;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}

package com.compliance.security;

import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<UUID> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<UUID> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_ROLE = new ThreadLocal<>();

    private TenantContext() {}

    public static UUID getTenantId() { return TENANT_ID.get(); }
    public static void setTenantId(UUID id) { TENANT_ID.set(id); }

    public static UUID getUserId() { return USER_ID.get(); }
    public static void setUserId(UUID id) { USER_ID.set(id); }

    public static String getUserRole() { return USER_ROLE.get(); }
    public static void setUserRole(String role) { USER_ROLE.set(role); }

    public static void clear() {
        TENANT_ID.remove();
        USER_ID.remove();
        USER_ROLE.remove();
    }
}

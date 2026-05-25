package dev.iamrat.auth.account.domain;

public enum AccountRole {
    USER,
    ADMIN,
    MANAGER;
    
    public String getValue() {
        return "ROLE_" + this.name();
    }
}

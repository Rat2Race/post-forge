package dev.iamrat.auth.account.entity;

public enum Role {
    USER, ADMIN, MANAGER;
    
    public String getValue() {
        return "ROLE_" + this.name();
    }
}

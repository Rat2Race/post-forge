package dev.iamrat.member.entity;

public enum Role {
    USER, ADMIN, MANAGER;
    
    public String getValue() {
        return "ROLE_" + this.name();
    }
}
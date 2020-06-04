package ru.hse.cs.java2020.task03;

import java.util.Objects;

public class UserInfo {
    public UserInfo(String newToken, String newOrg, String newLogin) {
        this.token = newToken;
        this.org = newOrg;
        this.login = newLogin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserInfo userInfo = (UserInfo) o;
        return token.equals(userInfo.token) && org.equals(userInfo.org) && login.equals(userInfo.login);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token, org, login);
    }

    public String getToken() {
        return token;
    }

    public void setToken(String newToken) {
        this.token = newToken;
    }

    public String getOrg() {
        return org;
    }

    public void setOrg(String newOrg) {
        this.org = newOrg;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String newLogin) {
        this.login = newLogin;
    }

    private String token;
    private String org;
    private String login;
}

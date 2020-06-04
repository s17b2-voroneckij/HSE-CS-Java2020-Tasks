package ru.hse.cs.java2020.task03;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;

public class UsersDatabase {
    UsersDatabase(String path) {
        url = new String("jdbc:sqlite:" + path);
    }

    void start() {
        try {
            connection = DriverManager.getConnection(url);
            createTable();
            System.err.println("connected successfully");
        } catch (SQLException exc) {
            System.err.println(exc.getMessage());
        }
    }

    void stop() {
        try {
            connection.close();
        } catch (SQLException exc) {
            exc.printStackTrace();
        }
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS users (\n"
                + " chatId integer PRIMARY KEY,\n"
                + " token text NOT NULL,\n"
                + " org text NOT NULL,\n"
                + " login text NOT NULL\n"
                + ");";
        try {
            var stmt = connection.createStatement();
            stmt.execute(sql);
        } catch (SQLException sqlException) {
            System.err.println("Error in CreateTable:");
            System.err.println(sqlException.getMessage());
        }
    }

    Optional<UserInfo> get(long chatId) {
        String sql = "SELECT token, org, login FROM users WHERE chatId=" + chatId;
        try {
            var statement = connection.createStatement();
            var result = statement.executeQuery(sql);
            return Optional.of(new UserInfo(result.getString("token"), result.getString("org"),
                                result.getString("login")));
        } catch (SQLException sqlException) {
            System.err.println(sqlException.getMessage());
            return Optional.empty();
        }
    }

    void insert(long chatId, UserInfo info) {
        String sqlInsert = "INSERT INTO users(chatId, token, org, login) VALUES(?,?,?,?)";
        String sqlDelete = "DELETE FROM users WHERE chatId=" + chatId;
        try {
            var deleteStatement = connection.createStatement();
            deleteStatement.execute(sqlDelete);
            var statement = connection.prepareStatement(sqlInsert);
            statement.setInt(1, (int) chatId);
            statement.setString(2, info.getToken());
            statement.setString(3, info.getOrg());
            statement.setString(4, info.getLogin());
            statement.executeUpdate();
        } catch (SQLException sqlException) {
            System.err.println(sqlException.getMessage());
        }
    }

    private Connection connection;
    private final String url;
}

package org.example.handler;

import java.sql.*;

public class AuthService {
    private static Connection connection;
    public static Statement stmt;

//    public static Connection getConnection() {
//        return connection;
//    }

    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:users.db");
            stmt = connection.createStatement();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getID(String login, String pass) {
        try {
            ResultSet rs = stmt.executeQuery("SELECT id, login, password FROM users WHERE login = '" + login + "'");
            int myHash = pass.hashCode();
            // 106438208
            if (rs.next()) {
                String nick = rs.getString(2);
                int dbHash = rs.getInt(3);
                if (myHash == dbHash) {
                    return rs.getString(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

}

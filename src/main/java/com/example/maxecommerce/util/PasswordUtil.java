package com.example.maxecommerce.util;

import com.fasterxml.jackson.databind.ser.Serializers;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class PasswordUtil {

    public static String hashPassword(String password) {
        try{
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not found",e);
        }
    }

    public static boolean validatePassword(String password, String hashedPassword) {
        String newHashed = hashPassword(password);
        return newHashed.equals(hashedPassword);
    }
}

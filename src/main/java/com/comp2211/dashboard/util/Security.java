package com.comp2211.dashboard.util;

import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Security {

  public static byte[] generateSalt() {
    SecureRandom random = new SecureRandom();
    byte[] salt = new byte[16];
    random.nextBytes(salt);
    return salt;
  }

  /**
   * Hash password with random salt using PBKDF2 and SHA-512
   * @param passwordToHash password to hash to array of bytes
   * @return hashed password in array of bytes
   */
  public static byte[] encodePassword(String passwordToHash, byte[] salt) {
    KeySpec spec = new PBEKeySpec(passwordToHash.toCharArray(), salt, 128000, 512);
    try {
      SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
      byte[] hash = factory.generateSecret(spec).getEncoded();
      return hash;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Match the password with the hash provided
   * @param pw the password to match
   * @param dbHash the hash to match
   * @return true if they match, false otherwise
   */
  public static boolean matchPassword(String pw, byte[] dbHash, byte[] dbSalt) {
    byte[] hash = encodePassword(pw, dbSalt);
    if(hash != null && dbHash != null && Arrays.equals(hash, dbHash)) {
      return true;
    }
    return false;
  }

  /**
   *
   * @param username
   * @return boolean indicating if validation was successful
   */
  public static boolean validateText(String username) {
    boolean pass = true;

    // Check length is 4 or greater, and less than or equal to 20
    if (username.length() < 4 || username.length() > 20) {
      pass = false;
    }

    // Check username only contains alphanumeric characters or the symbols '_', '.', '-'
    if (!Pattern.matches("^[0-9a-zA-Z_.-]+$", username)) {
      pass = false;
    }

    return pass;
  }
}

package com.book.bookflow.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class WxDecryptUtil {

    public static String decrypt(String encryptedData, String sessionKey, String iv) throws Exception {
        // Base64解码
        byte[] data = Base64.getDecoder().decode(encryptedData);
        byte[] key = Base64.getDecoder().decode(sessionKey);
        byte[] ivBytes = Base64.getDecoder().decode(iv);

        // AES解密
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        byte[] decrypted = cipher.doFinal(data);
        return new String(decrypted, "UTF-8");
    }
}
package com.appchina.pay.common.util;

import sun.misc.BASE64Encoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;


public class KeyGenerator {

    public static final String KEY_ALGORTHM="RSA";

    public static final String PUBLIC_KEY = "RSAPublicKey";//公钥
    public static final String PRIVATE_KEY = "RSAPrivateKey";//私钥

    /**
     * 初始化密钥
     * @return
     * @throws Exception
     */
    public static Map<String,String> getKey()throws Exception{
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORTHM);
        keyPairGenerator.initialize(1024);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        //公钥
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        //私钥
        RSAPrivateKey privateKey =  (RSAPrivateKey) keyPair.getPrivate();

        Map<String,String> keyMap = new HashMap<String, String>(2);

        keyMap.put(PUBLIC_KEY,encryptBASE64(publicKey.getEncoded()));
        keyMap.put(PRIVATE_KEY, encryptBASE64(privateKey.getEncoded()));

        return keyMap;
    }

    /**
     * BASE64加密
     * @param key
     * @return
     * @throws Exception
     */
    public static String encryptBASE64(byte[] key)throws Exception{
        return (new BASE64Encoder()).encodeBuffer(key);
    }

    public static void main(String[] args) throws Exception {
        String publicKey = KeyGenerator.getKey().get(PUBLIC_KEY);
        String privateKey = KeyGenerator.getKey().get(PRIVATE_KEY);
        System.out.println("==================开发者公钥 填写到开发者后台=============");
        System.out.println(publicKey);
        System.out.println("==================开发者私钥 务必保存好，请勿泄露=============");
        System.out.println(privateKey);
    }
}

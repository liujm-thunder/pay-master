package com.appchina.pay.common.util;


import org.apache.tomcat.util.codec.binary.Base64;

import javax.crypto.Cipher;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by chenyu on 17-8-23.
 */
public class RsaEncryptor {

    public static final String KEY_ALGORTHM="RSA";//
    public static final String SIGNATURE_ALGORITHM="MD5withRSA";

    public static final String PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDKUpyYDT6Jdi61fJXnFRT+QgRH" +
            "3rD9Hr/pqtAl1mtGEHgfEaoKhQ1MB/7oUi48+bB9ENe73ApVyOQtyHODIiD1l88V" +
            "fXvPIoP4JmSatbOcBWBs4yQnrjZb9AN/dDLjYozimN4dDKTxfE+KPPNRjL+NoPQL" +
            "DIBKSkE89/J1DvvDPQIDAQAB";//公钥
    public static final String PRIVATE_KEY = "MIICXAIBAAKBgQDKUpyYDT6Jdi61fJXnFRT+QgRH3rD9Hr/pqtAl1mtGEHgfEaoK" +
            "hQ1MB/7oUi48+bB9ENe73ApVyOQtyHODIiD1l88VfXvPIoP4JmSatbOcBWBs4yQn" +
            "rjZb9AN/dDLjYozimN4dDKTxfE+KPPNRjL+NoPQLDIBKSkE89/J1DvvDPQIDAQAB" +
            "AoGAGlCcZXpzg9WHRbuqk5++V0Om4uIoCwQQ2geJgyJcWYSS2xelEjE0BYuUsArg" +
            "ULX5KosiRCDeh0HVy86il4+80XlgD8/ykpgflfaU63EOEOth2L9pdYkp/9LDaque" +
            "9geEllFmYJW/xcRBrv+2vn8YuoAoh6OEofyalrVSPvoyYlECQQDXxWUmaaIyIKGY" +
            "up28F/2g8MEnv8ZcHYZ60pOCnPw1ehLLmK6jUnRxaEJ43g/KysMIM2o2FrSVsSHW" +
            "X5voQo/bAkEA8AtVwsHZ96aMjCLHk8+ySmeePU7ZIg70FYE/GoBZljrhZsc71RHA" +
            "jjk7eHfRf+g/8Dy655VEdZjGrnWG99TQxwJAAz1bf3Aml0oWIwzqQWC61ifHPqmK" +
            "eOeYlU+EF3nz73mdvqDUPm+GgA1oshfjdNAGjaD3/0fn/jdioq1c0eft8QJBAONT" +
            "yL8teG/ZUYcxNNKdwppGMQycjHN9t9hZ01oUrULhti7whZBORI4sU07OIiBA4bRw" +
            "vwysRu4bZiU39j7kU2cCQCq97JfU8iM2PTv65S2+EDcGOnTQnHNR9kadswyrC6BW" +
            "BqPDs8VdHW5+dhYjHMyJWmBMzN9c8aVERObv19N4c+U=";//私钥


    /**
     * 初始化密钥
     * @return
     * @throws Exception
     */
    public  Map<String,String> getKey()throws Exception{
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
     * 用私钥加密
     * @param data	加密数据
     * @param key	密钥
     * @return
     * @throws Exception
     */
    public byte[] encryptByPrivateKey(byte[] data,String key)throws Exception{
        //解密密钥
        byte[] keyBytes = decryptBASE64(key);
        //取私钥
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORTHM);
        Key privateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);

        //对数据加密
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);

        return cipher.doFinal(data);
    }

    /**
     * 用私钥解密 * @param data 	加密数据
     * @param key	密钥
     * @return
     * @throws Exception
     */
    public byte[] decryptByPrivateKey(byte[] data,String key)throws Exception{
        //对私钥解密
        byte[] keyBytes = decryptBASE64(key);

        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORTHM);
        Key privateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
        //对数据解密
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        return cipher.doFinal(data);
    }

    /**
     * 用公钥加密
     * @param data	加密数据
     * @param key	密钥
     * @return
     * @throws Exception
     */
    public byte[] encryptByPublicKey(byte[] data,String key)throws Exception{
        //对公钥解密
        byte[] keyBytes = decryptBASE64(key);
        //取公钥
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORTHM);
        Key publicKey = keyFactory.generatePublic(x509EncodedKeySpec);

        //对数据解密
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        return cipher.doFinal(data);
    }

    /**
     * 用公钥解密
     * @param data	加密数据
     * @param key	密钥
     * @return
     * @throws Exception
     */
    public byte[] decryptByPublicKey(byte[] data,String key)throws Exception{
        //对私钥解密
        byte[] keyBytes = decryptBASE64(key);
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORTHM);
        Key publicKey = keyFactory.generatePublic(x509EncodedKeySpec);

        //对数据解密
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
        cipher.init(Cipher.DECRYPT_MODE, publicKey);

        return cipher.doFinal(data);
    }

    /**
     *	用私钥对信息生成数字签名
     * @param data	//加密数据
     * @param privateKey	//私钥
     * @return
     * @throws Exception
     */
    public String sign(byte[] data,String privateKey)throws Exception{
        //解密私钥
        byte[] keyBytes = decryptBASE64(privateKey);
        //构造PKCS8EncodedKeySpec对象
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(keyBytes);
        //指定加密算法
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORTHM);
        //取私钥匙对象
        PrivateKey privateKey2 = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
        //用私钥对信息生成数字签名
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(privateKey2);
        signature.update(data);

        return encryptBASE64(signature.sign());
    }


    /**
     * 校验数字签名
     * @param data	加密数据
     * @param publicKey	公钥
     * @param sign	数字签名
     * @return
     * @throws Exception
     */
    public boolean verify(byte[] data,String publicKey,String sign)throws Exception{
        //解密公钥
        byte[] keyBytes = decryptBASE64(publicKey);
        //构造X509EncodedKeySpec对象
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(keyBytes);
        //指定加密算法
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORTHM);
        //取公钥匙对象
        PublicKey publicKey2 = keyFactory.generatePublic(x509EncodedKeySpec);

        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initVerify(publicKey2);
        signature.update(data);
        //验证签名是否正常
        return signature.verify(decryptBASE64(sign));

    }

    /**
     * BASE64解密
     * @param key
     * @return
     * @throws Exception
     */
    public byte[] decryptBASE64(String key) throws Exception{
        return Base64.decodeBase64(key);
    }

    /**
     * BASE64加密
     * @param key
     * @return
     * @throws Exception
     */
    public String encryptBASE64(byte[] key)throws Exception{
        return Base64.encodeBase64String(key);
    }


//
    public static void main(String[] args) throws Exception {
        RsaEncryptor rsaEncryptor = new RsaEncryptor();

        String publicKey = rsaEncryptor.getKey().get(PUBLIC_KEY);
        String privateKey = rsaEncryptor.getKey().get(PRIVATE_KEY);

        String testData = "orderId=05301415314809209132";
        byte[] data = testData.getBytes();

        String signStr = rsaEncryptor.sign(data, privateKey);
        System.out.println(signStr);

        System.out.println(rsaEncryptor.verify(data,publicKey, signStr));

    }

}

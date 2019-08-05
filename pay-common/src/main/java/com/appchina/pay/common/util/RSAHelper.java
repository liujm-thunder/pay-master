package com.appchina.pay.common.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;


public class RSAHelper {
    private static final String ALGORITHM = "RSA";
    private static final String ALGORITHM_SIGN = "MD5withRSA";

    public static final String DEFAULT = "RSA";
    public static final String ECB_PKCS1 = "RSA/ECB/PKCS1Padding";
    public static final String ECB_OAEP = "RSA/ECB/OAEPPadding";

    private RSAHelper() {
    }

    /**
     * Create a pair of RSA keys  创建一对 RSA 密钥
     *
     * @param keySize Key length, usually a multiple of 1024
     */

    public static KeyPair createKey(int keySize) {
        KeyPairGenerator keyPairGen;
        try {
            keyPairGen = KeyPairGenerator.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        keyPairGen.initialize(keySize);
        return keyPairGen.generateKeyPair();
    }

    /**
     * Parse the Base 64 string into a RSA public key   字符串中加载公钥
     *
     * @throws InvalidKeySpecException Invalid public key
     */

    public static PublicKey pubKeyFromBase64(String base64PublicKeyText) throws InvalidKeySpecException {
        byte[] buffer = Base64.decode(base64PublicKeyText.getBytes());
        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);
        return keyFactory.generatePublic(keySpec);
    }

    /**
     * Parse the Base 64 string into a RSA private key  字符串中加载私钥
     *
     * @throws InvalidKeySpecException Private key is invalid
     */

    public static PrivateKey priKeyFromBase64(String base64PrivateKeyText) throws InvalidKeySpecException {
        byte[] buffer = Base64.decode(base64PrivateKeyText.getBytes());
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(buffer);
        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return keyFactory.generatePrivate(keySpec);
    }


    /**
     * Generate a RSA digital signature for the information with the private key 用私钥对数据生成数字签名
     *
     * @param textBytes Original data
     * @param priKey    Private key
     * @throws InvalidKeyException Private key is invalid
     * @throws SignatureException  Signature exception
     */

    public static byte[] sign(byte[] textBytes, PrivateKey priKey)
            throws InvalidKeyException, SignatureException {
        Signature signature;
        try {
            signature = Signature.getInstance(ALGORITHM_SIGN);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        signature.initSign(priKey);
        signature.update(textBytes);

        return signature.sign();
    }

    /**
     * Generate a RSA digital signature for the information with the private key
     *
     * @param text   Original text
     * @param priKey Private key
     * @throws InvalidKeyException Private key is invalid
     * @throws SignatureException  Signature exception
     */

    public static byte[] sign(String text, PrivateKey priKey)
            throws InvalidKeyException, SignatureException {
        return sign(text.getBytes(), priKey);
    }

    /**
     * Generate a RSA digital signature of the information with a private key and return a Base64 string
     * 用私钥对数据生成数字签名并返回 BASE64 字符串
     *
     * @param textBytes Original data
     * @param priKey    Private key
     * @throws InvalidKeyException Private key is invalid
     * @throws SignatureException  Signature exception
     */

    public static String signToBase64(byte[] textBytes, PrivateKey priKey)
            throws InvalidKeyException, SignatureException {
        return Base64.encodeToString(sign(textBytes, priKey));
    }

    /**
     * Generate a RSA digital signature of the information with a private key and return a Base64 string
     *
     * @param text   Original text
     * @param priKey Private key
     * @throws InvalidKeyException Private key is invalid
     * @throws SignatureException  Signature exception
     */

    public static String signToBase64(String text, PrivateKey priKey)
            throws InvalidKeyException, SignatureException {
        return Base64.encodeToString(sign(text.getBytes(), priKey));
    }


    /**
     * Verify the RSA signature with the public key 用公钥校验签名
     *
     * @param signBytes signature
     * @param data      Original data
     * @param pubKey    Public key
     * @throws InvalidKeyException Invalid public key
     * @throws SignatureException  Signature exception
     */
    public static boolean verify(byte[] signBytes, byte[] data, PublicKey pubKey)
            throws InvalidKeyException, SignatureException {
        Signature signature;
        try {
            signature = Signature.getInstance(ALGORITHM_SIGN);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        signature.initVerify(pubKey);
        signature.update(data);
        return signature.verify(signBytes);
    }

    /**
     * Verify the RSA signature with the public key
     *
     * @param signBytes signature
     * @param text      Original text
     * @param pubKey    Public key
     * @throws InvalidKeyException Invalid public key
     * @throws SignatureException  Signature exception
     */
    public static boolean verify(byte[] signBytes, String text, PublicKey pubKey)
            throws InvalidKeyException, SignatureException {
        return verify(signBytes, text.getBytes(), pubKey);
    }

    /**
     * Verify the RSA signature with the public key
     *
     * @param base64Sign Base64 encoded signature
     * @param data       Original data
     * @param pubKey     Public key
     * @throws InvalidKeyException Invalid public key
     * @throws SignatureException  Signature exception
     */
    public static boolean verifyFromBase64(String base64Sign, byte[] data, PublicKey pubKey)
            throws InvalidKeyException, SignatureException {
        return verify(Base64.decode(base64Sign.getBytes()), data, pubKey);
    }

    /**
     * Verify the RSA signature with the public key
     *
     * @param base64Sign Base64 encoded signature
     * @param text       Original text
     * @param pubKey     Public key
     * @throws InvalidKeyException Invalid public key
     * @throws SignatureException  Signature exception
     */
    public static boolean verifyFromBase64(String base64Sign, String text, PublicKey pubKey)
            throws InvalidKeyException, SignatureException {
        return verify(Base64.decode(base64Sign), text.getBytes(), pubKey);
    }


    /**
     * Encrypt raw data using the RSA algorithm 加密
     *
     * @param rawData   Raw data to be encrypted
     * @param algorithm RSA encryption algorithm, The following values ​​are available: {@link #DEFAULT},{@link #ECB_PKCS1},{@link #ECB_OAEP}
     * @param key       Secret key
     * @throws InvalidKeyException       Invalid key
     * @throws BadPaddingException       Padding error
     * @throws IllegalBlockSizeException Block size error
     */

    public static byte[] encrypt(byte[] rawData, String algorithm, Key key)
            throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = createCipher(Cipher.ENCRYPT_MODE, algorithm, key);
        return blockDoCipher(cipher, Cipher.ENCRYPT_MODE, rawData);
    }

    /**
     * Encrypt raw text using the RSA algorithm 加密
     *
     * @param rawText   Raw text to be encrypted
     * @param algorithm RSA encryption algorithm, The following values ​​are available: {@link #DEFAULT},{@link #ECB_PKCS1},{@link #ECB_OAEP}
     * @param key       Secret key
     * @throws InvalidKeyException       Invalid key
     * @throws BadPaddingException       Padding error
     * @throws IllegalBlockSizeException Block size error
     */

    public static byte[] encrypt(String rawText, String algorithm, Key key)
            throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        return encrypt(rawText.getBytes(), algorithm, key);
    }

    /**
     * Encrypt the raw data using the RSA algorithm and convert the encrypted result to Base64 encoding
     *
     * @param rawData   Raw data to be encrypted
     * @param algorithm RSA encryption algorithm, The following values ​​are available: {@link #DEFAULT},{@link #ECB_PKCS1},{@link #ECB_OAEP}
     * @param key       Secret key
     * @throws InvalidKeyException       Invalid key
     * @throws BadPaddingException       Padding error
     * @throws IllegalBlockSizeException Block size error
     */

    public static String encryptToBase64(byte[] rawData, String algorithm, Key key)
            throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        return Base64.encodeToString(encrypt(rawData, algorithm, key));
    }

    /**
     * Encrypt the raw text using the RSA algorithm and convert the encrypted result to Base64 encoding
     *
     * @param rawText   Raw text to be encrypted
     * @param algorithm RSA encryption algorithm, The following values ​​are available: {@link #DEFAULT},{@link #ECB_PKCS1},{@link #ECB_OAEP}
     * @param key       Secret key
     * @throws InvalidKeyException       Invalid key
     * @throws BadPaddingException       Padding error
     * @throws IllegalBlockSizeException Block size error
     */

    public static String encryptToBase64(String rawText, String algorithm, Key key)
            throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        return Base64.encodeToString(encrypt(rawText.getBytes(), algorithm, key));
    }


    /**
     * Decrypt ciphertext encrypted using the RSA algorithm 解密
     *
     * @param cipherData Ciphertext to be decrypted
     * @param algorithm  RSA encryption algorithm, The following values ​​are available: {@link #DEFAULT},{@link #ECB_PKCS1},{@link #ECB_OAEP}
     * @param key        Secret key
     * @throws InvalidKeyException       Invalid key
     * @throws BadPaddingException       Padding error
     * @throws IllegalBlockSizeException Block size error
     */

    public static byte[] decrypt(byte[] cipherData, String algorithm, Key key)
            throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = createCipher(Cipher.DECRYPT_MODE, algorithm, key);
        return blockDoCipher(cipher, Cipher.DECRYPT_MODE, cipherData);
    }

    /**
     * Decryption uses the RSA algorithm to encrypt and then use Base64 encoded ciphertext  解密
     *
     * @param baseCipherText Ciphertext to be decrypted
     * @param algorithm      RSA encryption algorithm, The following values ​​are available: {@link #DEFAULT},{@link #ECB_PKCS1},{@link #ECB_OAEP}
     * @param key            Secret key
     * @throws InvalidKeyException       Invalid key
     * @throws BadPaddingException       Padding error
     * @throws IllegalBlockSizeException Block size error
     */

    public static byte[] decryptFromBase64(String baseCipherText, String algorithm, Key key)
            throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        return decrypt(Base64.decode(baseCipherText.getBytes()), algorithm, key);
    }

    /**
     * Decrypt ciphertext encrypted using the RSA algorithm
     *
     * @param cipherData Ciphertext to be decrypted
     * @param algorithm  RSA encryption algorithm, The following values ​​are available: {@link #DEFAULT},{@link #ECB_PKCS1},{@link #ECB_OAEP}
     * @param key        Secret key
     * @throws InvalidKeyException       Invalid key
     * @throws BadPaddingException       Padding error
     * @throws IllegalBlockSizeException Block size error
     */

    public static String decryptToString(byte[] cipherData, String algorithm, Key key)
            throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        return new String(decrypt(cipherData, algorithm, key));
    }

    /**
     * Decryption uses the RSA algorithm to encrypt and then use Base64 encoded ciphertext 解密
     *
     * @param baseCipherText Ciphertext to be decrypted
     * @param algorithm      RSA encryption algorithm, The following values ​​are available: {@link #DEFAULT},{@link #ECB_PKCS1},{@link #ECB_OAEP}
     * @param key            Secret key
     * @throws InvalidKeyException       Invalid key
     * @throws BadPaddingException       Padding error
     * @throws IllegalBlockSizeException Block size error
     */

    public static String decryptToStringFromBase64(String baseCipherText, String algorithm, Key key)
            throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        return new String(decryptFromBase64(baseCipherText, algorithm, key));
    }


    private static Cipher createCipher(int opMode, String algorithm, Key key) throws InvalidKeyException {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(algorithm);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new IllegalArgumentException(e);
        }

        cipher.init(opMode, key);

        return cipher;
    }

    private static byte[] blockDoCipher(Cipher cipher, int opMode, byte[] data) throws BadPaddingException, IllegalBlockSizeException {
        int dataLength = data.length;
        int blockSize = cipher.getBlockSize();
        if (blockSize <= 0) blockSize = opMode == Cipher.ENCRYPT_MODE ? 117 : 128;
        if (dataLength > blockSize) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int offset = 0;
            while (offset < dataLength) {
                int length = offset + blockSize <= dataLength ? blockSize : dataLength - offset;
                byte[] cache = cipher.doFinal(data, offset, length);
                try {
                    outputStream.write(cache);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                offset += length;
            }
            return outputStream.toByteArray();
        } else {
            return cipher.doFinal(data);
        }
    }


    public static void main(String[] args) throws Exception {


        Map<String,String> map = KeyGenerator.getKey();

        String PUBLIC_KEY  = map.get(KeyGenerator.PUBLIC_KEY).trim();
        String pri  = map.get(KeyGenerator.PRIVATE_KEY).trim();


//         String PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDsJGt7HfAOzfjCzE6iIgBazjvv" +
//                 "/hRwfnVwIUGIl39h+/FvBVIrIvLBUenCgFGf09vDClkigNHGfzKRo019V6Mqm1E1" +
//                 "D8B+m92t1lEnBnjVU6e+6P5zT6ml/ahHgWHw3XW6RVy+NabJF26VN2ogG1SBuqDa" +
//                 "JFQ4kL0xr7piJbtUbQIDAQAB";
        String PRIVATE_KEY = "MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAOwka3sd8A7N+MLM" +
                "TqIiAFrOO+/+FHB+dXAhQYiXf2H78W8FUisi8sFR6cKAUZ/T28MKWSKA0cZ/MpGj" +
                "TX1XoyqbUTUPwH6b3a3WUScGeNVTp77o/nNPqaX9qEeBYfDddbpFXL41pskXbpU3" +
                "aiAbVIG6oNokVDiQvTGvumIlu1RtAgMBAAECgYEAj7Ll+QjR0aCDtb7wRveb8aY4" +
                "kSWzuHUr7+083OscKDtRw3agdwGQahX3w1Wk1jbtL7Y3Yai0fy9eTYPrns/ayOJ1" +
                "rUKmnFjnD0YgitMu+JNWdg/B2lX+ucHvucVK0mkbnmlY3TQFSRU6o/dDjgIdwIl4" +
                "hdxaheA+fnwAMdUXWcECQQD3blLkss7nfVBXgDiKaXri0BsBvA8gHGNfLjTfEIdP" +
                "lZsxmXzrgtn5Fu30aWfkcQn8IzDBPER47Fjp6xhVEf2RAkEA9FIDPmHXOUMPl453" +
                "84o5BwRSLjDff2EZMuPCOM9YFPVxOqDqbv1AfXgBHeqVtRvZUcHlAIT6+c3dFu4O" +
                "X1ZrHQJBAM+/T9Y508NF0llFjTOZ0NXziVlxfvmlHEJkV3wbMqE9qeqBRwOvADlG" +
                "aVDX16VUy99p5Ju6cHtfZmAxRmLXEiECQCryP5+3kx19rD/3yx4ELgINwGReMusx" +
                "JjzLzFgwGkuU2VJ09sCLw8pKTef0VFyBiLHWY2qz9WnzxelB70TS7AECQQCH+Uo4" +
                "WpRumb1PNnLc5yrjxXnNBSTRzxdjqArayBN2hq9R7O1iTPY1vrqCW3pGiI2J4Gqc" +
                "aPf0pfK7EZtkk06Y";//私钥

//        String pri = "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAMoEoGvROPrWi5IY" +
//                "iedcVY1BmjAW/c73uzgvTXlZeAQdv7vaLdEYDS3ZDtxRwYtPYFhnEStoHRWYWE50" +
//                "jgxH0+e9DoPbWw5sVjx6vybad0utBkSRFCxb3ssp0wh+xA6vxtgTXe+xn/Lxpy/y" +
//                "M3ESIebrvLpaNUb0FKRhp6g8UIydAgMBAAECgYABnX/a3NVjzAtZo7CWlPpqIrgk" +
//                "4kotOXXZwScRbVG0VriNu/TZ0yNn1nBz+oNdpcjTbB+LBU4WOh2aovvvxTNAh3Gd" +
//                "zWm0ZcqpMd+rULJH/RuoX4Pti+HlK2o7i2FoD4aZTndfO8mxnHFUbA8Py457KOcy" +
//                "301VUR51i2ye5ydiAQJBAPI+PVRMMvAMz55/6NvYvRxAqFlRwbyiiNVvFsBgYd5T" +
//                "V5UQKzIm4auJIUKqpOBjdhOJeK5SpzbHZZTq4PPbUn0CQQDVfZcfufE6TdQr8nKU" +
//                "UA3jBWAKqnKIDsxwCo8+5uEgRiYUchEfQ57i+IFOE5339ckVlROAyt8Hq7Mhqg2+" +
//                "jByhAkEAwAwx6QPkGkW90AXOIYVKH/zuuqlDc/5ThwpkOi3vSSg/tjC0XjVPEgRM" +
//                "dyL8Rdz0fnatU165rIcWdKJlp07IrQJAfFJKcvtA8oboCz+AYcXMkGtM5mkjkP+t" +
//                "JYHAsQyaBMVU34sdVWt3Vw0Hn4Pk9cR3eM37MYDyJ/FguzXgExpcgQJBAPBVDcSq" +
//                "m6s35J4zrY7cgeozasOy9f5nhe9bEXmAIV9i4hVFAbMwV5uvtevpk5O0AAf251sa" +
//                "9pq1DdKS2+xS1sY=";

        //生成签名私钥
        String makeSignPriKey = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAMSCw3NmZ9k+0U1a" +
                "cCqmzpMaKoQ3Av6Fb3+sqAFJAK7VgufBdm/gLMCBCtYXOgv9RwCkqc/HSH2wYdI/" +
                "KR9H3Fu51/nkKglS1e4TPafxPo/lGC7GG/BgdV1++xld86EqE1Dwm1l7nrjsoMGO" +
                "VAE+Ghd2VcvBxaq7y5ipEkTFKsIXAgMBAAECgYEAsowFPiL7lF4JGflkFLy+0NVj" +
                "cAHzzII8zop3k8NaxX/lkuEq1Xef8cDNsbwk16PnEWSLjegJq3nJR5hvqqZGRdGU" +
                "L6xGDBrprV3ngU0eh+T74TAU7SNyYubs697CZvBpDnkh6U8D7YbZIo7hH9GSasz1" +
                "D18C7W82Nim7/4hGzCECQQDmxY8WDnWbfzKbDY77XoZF7h1h666qGO8MeUzyACKu" +
                "gu3sfaTqq+L4tBJYgNOKUFgoaVwRKWhCS982KWek35avAkEA2f5edLG4din1khUe" +
                "gPR0VHQvc5Z15yrnNvbCUefK/wz5Ra+MpgJjTp4Y/wmNQNXKwpiF7M50hneBzN+6" +
                "Eb5lGQJAZ9IBaX7f0jELZ05WQShpaBSUC3WogsXs5cO8pjMBZ1loCLkN9LWXyyPY" +
                "DREIGnXC84tS7DWgvhK8PPWrtzUP6QJAeVosNOQWXtle1lKhZ4IuHDGNlNgGjIiK" +
                "rENTy4qwq6kKPyvJrUSZCdPi8F7d3mDlfcywiTIpFg4DGQzWpTgLSQJAdu6WXyoj" +
                "V7XGgrPkkGmCvM5yBbJPpXcjkZYQR+EYk9jk1ExEU1iFpUsh/bskO+VaCkmgTvBZ" +
                "OcV7ed0F/KvVxw==";
        PrivateKey priKey = null;
        PublicKey pubKey = null;
        try {
            priKey = RSAHelper.priKeyFromBase64(pri);
            pubKey = RSAHelper.pubKeyFromBase64(PUBLIC_KEY);
        }catch (Exception e){
            e.getMessage();
        }

        String sign = null;
        String orderId = "P0020190530173855000003";
        try {
            sign = RSAHelper.signToBase64(String.format("payOrderId=%s", orderId), priKey);
            System.out.println(sign);
        } catch (InvalidKeyException e) {
        } catch (SignatureException e) {
        }

        Boolean isSucess;
        try {
            isSucess = RSAHelper.verifyFromBase64(sign, String.format("payOrderId=%s", orderId), pubKey);
            System.out.println(isSucess);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        }
    }
}

    

package cn.edu.uestc.graduationproject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

import sun.misc.BASE64Decoder;

public class IntegrityVerifier {
	private static Cipher cipher;

	static {
		try {
			cipher = Cipher.getInstance("RSA");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		}
	}

	public static PrivateKey getPrivateKey(String key) throws Exception {
		byte[] keyBytes;
		keyBytes = (new BASE64Decoder()).decodeBuffer(key);
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
		return privateKey;
	}

	protected static boolean IntegrityVerify(String Ro, String privateKeystore,
			String evidence) throws Exception {
		String eStrSec = readFileStr(Ro);
		String eStr = decrypt(privateKeystore, eStrSec);
		System.err.println(evidence + "over\n" + eStrSec + "over\n" + eStr
				+ "over\n");
		return evidence.equals(eStr.substring((eStr.length() - evidence
				.length())));
	}

	protected static String getFileHash(File file, String mothed)
			throws Exception {
		MessageDigest digest = null;
		FileInputStream in = null;
		byte buffer[] = new byte[8192];
		int len;
		digest = MessageDigest.getInstance(mothed);
		in = new FileInputStream(file);
		while ((len = in.read(buffer)) != -1) {
			digest.update(buffer, 0, len);
		}
		BigInteger bigInt = new BigInteger(1, digest.digest());
		in.close();
		return bigInt.toString();
	}

	protected static String decrypt(String privateKeystore, String enStr)
			throws Exception {
		String privateKeyString = readFileStr(privateKeystore);
		cipher.init(Cipher.DECRYPT_MODE, getPrivateKey(privateKeyString));
		byte[] deBytes = cipher.doFinal((new BASE64Decoder())
				.decodeBuffer(enStr));
		return new String(deBytes);
	}

	protected static String readFileStr(String file) throws Exception {
		String fileStr = "";
		FileReader fr = new FileReader(file);
		BufferedReader br = new BufferedReader(fr);
		String str;
		while ((str = br.readLine()) != null) {
			fileStr += str;
		}
		br.close();
		fr.close();
		return fileStr;
	}
}

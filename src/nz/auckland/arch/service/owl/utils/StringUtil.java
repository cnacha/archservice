package nz.auckland.arch.service.owl.utils;

import java.util.Random;

public class StringUtil {

	public static String randomNumber(int length) {
		char[] chars = "0123456789".toCharArray();
		Random rnd = new Random();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++)
		    sb.append(chars[rnd.nextInt(chars.length)]);

		return sb.toString();
	}
}

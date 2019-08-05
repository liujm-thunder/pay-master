package com.appchina.pay.common.util;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

public class IpHelper {

	public static String getIpAddr(HttpServletRequest request) {
		String ip = request.getHeader("X-Real-IP");

		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("X-Forwarded-For");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("x-forwarded-for");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		// 如取ip失败
		if (ip == null || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
			ip = "127.0.0.1";
		}
		// 两个ip地址拼在一起。一个公网一个内网。
		// 如：119.56.21.5, 192.168.230.10
		if (ip.indexOf(",") > 0) {
			ip = ip.substring(0, ip.indexOf(","));
		}

		return ip;
	}

	public static long getIpNum(HttpServletRequest req) {
		return addrToNum(getIpAddr(req));
	}

	public static long addrToNum(String ip) {
		if (ip == null || ip.trim().length() <= 0)
			return 0;

		if (!isIp(ip))
			return 0;

		long[] ipNum = new long[4];
		try {
			// 先找到IP地址字符串中.的位置
			int position1 = ip.indexOf(".");
			int position2 = ip.indexOf(".", position1 + 1);
			int position3 = ip.indexOf(".", position2 + 1);

			// 将每个.之间的字符串转换成整型
			ipNum[0] = Long.parseLong(ip.substring(0, position1));
			ipNum[1] = Long.parseLong(ip.substring(position1 + 1, position2));
			ipNum[2] = Long.parseLong(ip.substring(position2 + 1, position3));
			ipNum[3] = Long.parseLong(ip.substring(position3 + 1));
		} catch (Exception e) {
			Arrays.fill(ipNum, 0);
		}
		return (ipNum[0] << 24) + (ipNum[1] << 16) + (ipNum[2] << 8) + ipNum[3];
	}

	/**
	 * 将长整转换成127.0.0.1形式的点分十进制地址
	 * 
	 * @param ipNum
	 * @return 点分十进制地址
	 */
	public static String numToAddr(long ipNum) {
		StringBuffer ip = new StringBuffer("");
		try {
			// 直接右移24位
			ip.append(String.valueOf(ipNum >>> 24));
			ip.append(".");
			// 将高8位置0，然后右移16位
			ip.append(String.valueOf((ipNum & 0x00FFFFFF) >>> 16));
			ip.append(".");
			ip.append(String.valueOf((ipNum & 0x0000FFFF) >>> 8));
			ip.append(".");
			ip.append(String.valueOf(ipNum & 0x000000FF));
		} catch (Exception e) {
			ip.append("");
		}
		return ip.toString();
	}

	private static boolean isIp(String ip) {
		return ip.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
	}
}

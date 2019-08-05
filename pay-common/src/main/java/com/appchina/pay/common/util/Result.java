package com.appchina.pay.common.util;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;


public final class Result implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private static final int CODE_SUCCESS = 1;
	private static final int CODE_ERROR = -1;
	
	private static final int ALL_SERVICE_ERROR = -100;

	private int resultid;
	private String message;
	private String sign;
	private Object data;
	
	private Result(){ }

	public int getResultid() {
		return resultid;
	}
	public void setResultid(int resultid) {
		this.resultid = resultid;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public Object getData() {
		return data;
	}
	public void setData(Object data) {
		this.data = data;
	}

	public String getSign() {
		return sign;
	}

	public void setSign(String sign) {
		this.sign = sign;
	}

	public static Result success(){
		Result result = new Result();
		result.setResultid(CODE_SUCCESS);
		return result;
	}
	
	public static Result success(Object data){
		Result result = success();
		result.setData(data);
		return result;
	}
	
	public static Result error(){
		Result result = new Result();
		result.setResultid(CODE_ERROR);
		result.setMessage("操作失败");
		return result;
	}
	
	public static Result error(String msg){
		Result result = error();
		result.setMessage(msg);
		return result;
	}
	
	public static Result allServiceError(){
		Result result = new Result();
		result.setResultid(ALL_SERVICE_ERROR);
		result.setMessage("服务不可用，请稍后再试");
		return result;
	}
	
	
	public static Result condition(boolean success){
		return success ? success() : error();
	}
	
	public static Result condition(boolean success, Object msgOrData){
		Result result = condition(success);
		if(success){
			result.setData(msgOrData);
		}else if(msgOrData != null) {
			result.setMessage(msgOrData.toString());
		}
		return result;
	}

	public static Result condition(boolean success, Object data, String msg){
		Result result = condition(success);
		if(data != null) {
			result.setData(data);
		}
		if(StringUtils.isNotEmpty(msg)){
			result.setMessage(msg);
		}

		return result;
	}


	public static Result error(Exception e) {
		Result result = error();
		if(e instanceof IllegalArgumentException){
			result.setMessage(e.getMessage());
		}
		return result;
	}

	public static Result error(int code, String msg) {
		Result result = error(msg);
		result.setResultid(code);
		return result;
	}
	
	public static Result successWithMsg(String msg){
		Result result = new Result();
		result.setResultid(CODE_SUCCESS);
		result.setMessage(msg);
		return result;
	}

	@Override
	public String toString() {
		return "Result{" +
				"resultid=" + resultid +
				", message='" + message + '\'' +
				", sign='" + sign + '\'' +
				", data=" + data +
				'}';
	}
}

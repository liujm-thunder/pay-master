package com.appchina.pay.common.model;

import java.io.Serializable;

public class Result implements Serializable{

    private static final long serialVersionUID = 7937522581108262010L;

    private static final int CODE_SUCCESS = 0;
    private static final int CODE_ERROR = -1;

    private static final int ALL_SERVICE_ERROR = -100;

    private int resultId;
    private String message;
    private Object data;

    public Result() {

    }

    public static Result success(){
        Result result = new Result();
        result.setResultId(CODE_SUCCESS);
        return result;
    }

    public static Result success(Object data){
        Result result = success();
        result.setData(data);
        return result;
    }
    public static Result success(String message){
        Result result = success();
        result.setMessage(message);
        return result;
    }

    public static Result error(){
        Result result = new Result();
        result.setResultId(CODE_ERROR);
        result.setMessage("操作失败");
        return result;
    }

    public static Result error(String msg){
        Result result = error();
        result.setMessage(msg);
        return result;
    }
    public static Result error(String msg,Object data){
        Result result = error();
        result.setMessage(msg);
        result.setData(data);
        return result;
    }

    public int getResultId() {
        return resultId;
    }

    public void setResultId(int resultId) {
        this.resultId = resultId;
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
}

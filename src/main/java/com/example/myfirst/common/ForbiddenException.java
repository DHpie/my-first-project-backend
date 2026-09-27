package com.example.myfirst.common;

/**
 * 权限不足异常 —— 当用户尝试访问不属于自己的资源时抛出
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}

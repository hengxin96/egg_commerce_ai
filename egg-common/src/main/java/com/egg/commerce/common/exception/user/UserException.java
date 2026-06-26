package com.egg.commerce.common.exception.user;

import com.egg.commerce.common.exception.base.BaseException;

/**
 * 用户信息异常类
 * 
 * @author egg
 */
public class UserException extends BaseException
{
    private static final long serialVersionUID = 1L;

    public UserException(String code, Object[] args)
    {
        super("user", code, args, null);
    }
}

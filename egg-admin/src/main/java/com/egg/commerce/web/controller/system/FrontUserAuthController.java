package com.egg.commerce.web.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.egg.commerce.common.constant.Constants;
import com.egg.commerce.common.core.controller.BaseController;
import com.egg.commerce.common.core.domain.AjaxResult;
import com.egg.commerce.common.core.domain.entity.SysUser;
import com.egg.commerce.common.core.domain.model.LoginUser;
import com.egg.commerce.framework.web.service.FrontUserAuthService;
import com.egg.commerce.framework.web.service.FrontUserAuthService.AuthResult;

/**
 * 前台用户认证接口。
 * 与后台管理员登录分离：第一版只支持手机号/密码，不要求验证码。
 */
@RestController
@RequestMapping({"/user/auth", "/api/user/auth"})
public class FrontUserAuthController extends BaseController
{
    @Autowired
    private FrontUserAuthService frontUserAuthService;

    /**
     * 手机号已存在则登录，不存在则自动注册前台用户后登录。
     */
    @PostMapping("/login-or-register")
    public AjaxResult loginOrRegister(@RequestBody PhonePasswordBody body)
    {
        AuthResult result = frontUserAuthService.loginOrRegister(body.getPhonenumber(), body.getPassword());
        AjaxResult ajax = success();
        ajax.put(Constants.TOKEN, result.getToken());
        ajax.put("registered", result.isRegistered());
        ajax.put("userName", result.getUserName());
        ajax.put("nickName", result.getNickName());
        ajax.put("phonenumber", result.getPhonenumber());
        return ajax;
    }

    /**
     * 返回当前登录前台用户资料，用于刷新后或旧会话补齐头像悬浮信息。
     */
    @GetMapping("/profile")
    public AjaxResult profile()
    {
        LoginUser loginUser = getLoginUser();
        SysUser user = loginUser.getUser();
        AjaxResult ajax = success();
        ajax.put("userName", user != null ? user.getUserName() : loginUser.getUsername());
        ajax.put("nickName", user != null ? user.getNickName() : loginUser.getUsername());
        ajax.put("phonenumber", user != null ? user.getPhonenumber() : "");
        return ajax;
    }

    /**
     * 忘记密码第一版：手机号命中账号后直接写入新密码。
     */
    @PostMapping("/reset-password")
    public AjaxResult resetPassword(@RequestBody ResetPasswordBody body)
    {
        frontUserAuthService.resetPassword(body.getPhonenumber(), body.getPassword(), body.getConfirmPassword());
        return success();
    }

    /** 手机号密码登录请求体。 */
    public static class PhonePasswordBody
    {
        private String phonenumber;
        private String password;

        public String getPhonenumber()
        {
            return phonenumber;
        }

        public void setPhonenumber(String phonenumber)
        {
            this.phonenumber = phonenumber;
        }

        public String getPassword()
        {
            return password;
        }

        public void setPassword(String password)
        {
            this.password = password;
        }
    }

    /** 忘记密码请求体。 */
    public static class ResetPasswordBody extends PhonePasswordBody
    {
        private String confirmPassword;

        public String getConfirmPassword()
        {
            return confirmPassword;
        }

        public void setConfirmPassword(String confirmPassword)
        {
            this.confirmPassword = confirmPassword;
        }
    }
}
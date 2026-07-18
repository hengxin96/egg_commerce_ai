package com.egg.commerce.framework.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.egg.commerce.common.core.domain.entity.SysRole;
import org.junit.jupiter.api.Test;
import com.egg.commerce.common.core.domain.entity.SysUser;
import com.egg.commerce.common.core.domain.model.LoginUser;
import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.system.service.ISysRoleService;
import com.egg.commerce.system.service.ISysUserService;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 前台手机号登录服务测试，锁定第一版自动注册和忘记密码的字段规则。
 */
class FrontUserAuthServiceTest
{
    /**
     * 新前台用户必须按约定写入 sys_user 的用户类型、手机号和 egg_ 昵称账号。
     */
    @Test
    void buildFrontStageUserStoresPhoneAndEggNickname()
    {
        FrontUserAuthService service = new FrontUserAuthService(null, null, null, null);

        SysUser user = service.buildFrontStageUser("13800138000", "12345");

        assertEquals("03", user.getUserType());
        assertEquals("13800138000", user.getPhonenumber());
        assertTrue(user.getUserName().startsWith("egg_"));
        assertEquals(user.getUserName(), user.getNickName());
        assertTrue(user.getPassword().startsWith("$2"));
    }

    /**
     * 忘记密码第一版只校验两次新密码一致，不一致时不能落库。
     */
    @Test
    void validateResetPasswordRejectsMismatch()
    {
        FrontUserAuthService service = new FrontUserAuthService(null, null, null, null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.validateResetPassword("13800138000", "12345", "54321"));

        assertEquals("两次输入的新密码不一致", exception.getMessage());
    }

    /**
     * 新手机号自动注册后必须绑定 common 角色，供知识库检索按普通用户权限过滤。
     */
    @Test
    void loginOrRegisterBindsCommonRoleWhenAutoRegisteringFrontUser()
    {
        TokenService tokenService = mock(TokenService.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        ISysUserService userService = mock(ISysUserService.class);
        ISysRoleService roleService = mock(ISysRoleService.class);
        SysRole commonRole = new SysRole();
        commonRole.setRoleId(2L);
        commonRole.setRoleKey("common");
        Authentication authentication = mock(Authentication.class);
        LoginUser loginUser = new LoginUser();
        SysUser authenticatedUser = new SysUser();
        authenticatedUser.setUserName("egg_test");
        authenticatedUser.setPassword("encoded");
        loginUser.setUserId(10L);
        loginUser.setUser(authenticatedUser);

        when(userService.selectUserByPhonenumber("13800138000")).thenReturn(null);
        when(userService.checkUserNameUnique(any(SysUser.class))).thenReturn(true);
        when(userService.registerUser(any(SysUser.class))).thenAnswer(invocation -> {
            SysUser registeredUser = invocation.getArgument(0);
            registeredUser.setUserId(10L);
            return true;
        });
        when(roleService.selectRoleList(any(SysRole.class))).thenReturn(List.of(commonRole));
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(loginUser);
        when(tokenService.createToken(loginUser)).thenReturn("token");
        FrontUserAuthService service = new FrontUserAuthService(tokenService, authenticationManager, userService,
                roleService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "JUnit");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        FrontUserAuthService.AuthResult result;
        try
        {
            result = service.loginOrRegister("13800138000", "12345");
        }
        finally
        {
            RequestContextHolder.resetRequestAttributes();
        }

        assertTrue(result.isRegistered());
        verify(userService).insertUserAuth(eq(10L), argThat(roleIds -> Arrays.equals(roleIds, new Long[] { 2L })));
    }
}

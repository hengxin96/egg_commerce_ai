package com.egg.commerce.framework.web.service;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.egg.commerce.common.constant.UserConstants;
import com.egg.commerce.common.core.domain.entity.SysRole;
import com.egg.commerce.common.core.domain.entity.SysUser;
import com.egg.commerce.common.core.domain.model.LoginUser;
import com.egg.commerce.common.exception.ServiceException;
import com.egg.commerce.common.utils.DateUtils;
import com.egg.commerce.common.utils.SecurityUtils;
import com.egg.commerce.common.utils.StringUtils;
import com.egg.commerce.common.utils.ip.IpUtils;
import com.egg.commerce.framework.security.context.AuthenticationContextHolder;
import com.egg.commerce.system.service.ISysRoleService;
import com.egg.commerce.system.service.ISysUserService;
import java.util.List;

/**
 * 前台用户手机号认证服务。
 * 第一版不走验证码：手机号已存在时登录，不存在时创建 egg_ 前缀账号后立即登录。
 */
@Component
public class FrontUserAuthService
{
    private static final String FRONT_USER_TYPE = "03";
    private static final String COMMON_ROLE_KEY = "common";
    private static final int PHONE_LENGTH = 11;
    private static final int EGG_SUFFIX_LENGTH = 12;

    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final ISysUserService userService;
    private final ISysRoleService roleService;

    @Autowired
    public FrontUserAuthService(TokenService tokenService, AuthenticationManager authenticationManager,
            ISysUserService userService, ISysRoleService roleService)
    {
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.roleService = roleService;
    }

    /**
     * 手机号已注册时登录；未注册时自动创建前台用户并登录。
     */
    public AuthResult loginOrRegister(String phonenumber, String password)
    {
        String phone = normalizePhone(phonenumber);
        validatePhoneAndPassword(phone, password);

        SysUser user = userService.selectUserByPhonenumber(phone);
        boolean registered = false;
        if (StringUtils.isNull(user))
        {
            user = registerFrontStageUser(phone, password);
            registered = true;
        }

        String token = authenticateAndCreateToken(user.getUserName(), password);
        return new AuthResult(token, registered, user.getUserName(), user.getNickName(), user.getPhonenumber());
    }

    /**
     * 忘记密码第一版只用手机号和两次新密码，后续再补短信验证码等安全校验。
     */
    public void resetPassword(String phonenumber, String password, String confirmPassword)
    {
        String phone = normalizePhone(phonenumber);
        validateResetPassword(phone, password, confirmPassword);

        SysUser user = userService.selectUserByPhonenumber(phone);
        if (StringUtils.isNull(user))
        {
            throw new ServiceException("手机号未注册");
        }
        userService.resetUserPwd(user.getUserId(), SecurityUtils.encryptPassword(password));
    }

    /**
     * 构造待入库的前台用户，保证 sys_user 的核心字段与产品约定一致。
     */
    SysUser buildFrontStageUser(String phonenumber, String rawPassword)
    {
        String eggName = generateEggUserName();
        SysUser user = new SysUser();
        user.setUserName(eggName);
        user.setNickName(eggName);
        user.setUserType(FRONT_USER_TYPE);
        user.setPhonenumber(phonenumber);
        user.setPwdUpdateDate(DateUtils.getNowDate());
        user.setPassword(SecurityUtils.encryptPassword(rawPassword));
        return user;
    }

    /**
     * 校验忘记密码的第一版输入规则。
     */
    void validateResetPassword(String phonenumber, String password, String confirmPassword)
    {
        validatePhoneAndPassword(phonenumber, password);
        if (!StringUtils.equals(password, confirmPassword))
        {
            throw new ServiceException("两次输入的新密码不一致");
        }
    }

    private SysUser registerFrontStageUser(String phonenumber, String password)
    {
        SysUser user = null;
        for (int i = 0; i < 8; i++)
        {
            user = buildFrontStageUser(phonenumber, password);
            if (userService.checkUserNameUnique(user))
            {
                break;
            }
        }
        if (StringUtils.isNull(user) || !userService.checkUserNameUnique(user))
        {
            throw new ServiceException("生成前台用户账号失败，请重试");
        }
        if (!userService.registerUser(user))
        {
            throw new ServiceException("注册失败，请联系系统管理人员");
        }
        bindCommonRole(user);
        return user;
    }

    private void bindCommonRole(SysUser user)
    {
        if (StringUtils.isNull(user.getUserId()))
        {
            throw new ServiceException("注册用户缺少用户ID，请联系系统管理人员");
        }
        SysRole commonRole = selectCommonRole();
        userService.insertUserAuth(user.getUserId(), new Long[] { commonRole.getRoleId() });
    }

    private SysRole selectCommonRole()
    {
        SysRole query = new SysRole();
        query.setRoleKey(COMMON_ROLE_KEY);
        List<SysRole> roles = roleService.selectRoleList(query);
        for (SysRole role : roles)
        {
            if (role != null && StringUtils.equals(COMMON_ROLE_KEY, role.getRoleKey())
                    && StringUtils.isNotNull(role.getRoleId()))
            {
                return role;
            }
        }
        throw new ServiceException("common角色未配置，请联系系统管理人员");
    }

    private String authenticateAndCreateToken(String username, String password)
    {
        Authentication authentication;
        try
        {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
            AuthenticationContextHolder.setContext(authenticationToken);
            authentication = authenticationManager.authenticate(authenticationToken);
        }
        catch (BadCredentialsException e)
        {
            throw new ServiceException("密码错误");
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException(e.getMessage());
        }
        finally
        {
            AuthenticationContextHolder.clearContext();
        }

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        userService.updateLoginInfo(loginUser.getUserId(), IpUtils.getIpAddr(), DateUtils.getNowDate());
        return tokenService.createToken(loginUser);
    }

    private void validatePhoneAndPassword(String phonenumber, String password)
    {
        if (!isValidPhone(phonenumber))
        {
            throw new ServiceException("手机号格式不正确");
        }
        if (StringUtils.isEmpty(password)
                || password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            throw new ServiceException("密码长度必须在5到20个字符之间");
        }
    }

    private boolean isValidPhone(String phonenumber)
    {
        return StringUtils.isNotEmpty(phonenumber)
                && phonenumber.length() == PHONE_LENGTH
                && phonenumber.chars().allMatch(Character::isDigit);
    }

    private String normalizePhone(String phonenumber)
    {
        return phonenumber == null ? "" : phonenumber.trim();
    }

    private String generateEggUserName()
    {
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, EGG_SUFFIX_LENGTH);
        return "egg_" + uuid;
    }

    /**
     * 前台登录响应，只返回前端需要保存和展示的最小信息。
     */
    public static class AuthResult
    {
        private final String token;
        private final boolean registered;
        private final String userName;
        private final String nickName;
        private final String phonenumber;

        public AuthResult(String token, boolean registered, String userName, String nickName, String phonenumber)
        {
            this.token = token;
            this.registered = registered;
            this.userName = userName;
            this.nickName = nickName;
            this.phonenumber = phonenumber;
        }

        public String getToken()
        {
            return token;
        }

        public boolean isRegistered()
        {
            return registered;
        }

        public String getUserName()
        {
            return userName;
        }

        public String getNickName()
        {
            return nickName;
        }

        public String getPhonenumber()
        {
            return phonenumber;
        }
    }
}

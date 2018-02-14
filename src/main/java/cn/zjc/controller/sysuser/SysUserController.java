package cn.zjc.controller.sysuser;

import cn.zjc.model.sysUser.SysUser;
import cn.zjc.server.sysUser.SysUserService;
import cn.zjc.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/**
 * Created by zhangjiacheng on 2018/2/2.
 */
@RestController
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public JsonResult login(@AuthenticationPrincipal SysUser sysUser,
                            @RequestParam(value = "logout", required = false) String logout) {
        if (Objects.nonNull(logout)) {
            SecurityContextHolder.getContext().getAuthentication().isAuthenticated();
            return JsonResult.success("退出成功");
        }
        if (Objects.nonNull(sysUser)) {
            return JsonResult.success("登录成功");
        }
        return JsonResult.success("登录失败");
    }
    @RequestMapping(value = "/login333", method = RequestMethod.GET)
    public JsonResult login333(@AuthenticationPrincipal SysUser sysUser,
                            @RequestParam(value = "logout", required = false) String logout) {
        if (Objects.nonNull(logout)) {
            SecurityContextHolder.getContext().getAuthentication().isAuthenticated();
            return JsonResult.success("退出成功");
        }
        if (Objects.nonNull(sysUser)) {
            return JsonResult.success("登录成功");
        }
        return JsonResult.success("登录失败");
    }
    @RequestMapping(value = "/403", method = RequestMethod.GET)
    public JsonResult accessDenied() {
        //检查用户是否已经登录
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof AnonymousAuthenticationToken)) {
            UserDetails userDetail = (UserDetails) auth.getPrincipal();
        }
        return JsonResult.EMPTY_SUCCESS;
    }

    @RequestMapping(value = "/register" , method = RequestMethod.POST)
    public JsonResult register(@RequestParam("username") String username, @RequestParam("password") String password){
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(16);
        SysUser sysUser = new SysUser();
        sysUser.setLoginName(username);
        sysUser.setUsername(username);
        sysUser.setPassword(passwordEncoder.encode(password));
        sysUser.setSalt(UUID.randomUUID().toString());
        sysUserService.save(sysUser);
        return JsonResult.success(sysUser.getUsername(),"注册成功");
    }
}

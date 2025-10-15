package com.zzx.yunpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zzx.yunpicturebackend.model.dto.user.UserQueryRequest;
import com.zzx.yunpicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zzx.yunpicturebackend.model.vo.LoginUserVO;
import com.zzx.yunpicturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author zzx
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-08-17 15:33:00
*/
public interface UserService extends IService<User> {
    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    User getLoginUser(HttpServletRequest request);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    String getEncryptPassword(String userPassword);

    /**
     * 获取脱敏后的用户信息
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    boolean userLogout(HttpServletRequest request);

    UserVO getUserVO(User user);

    List<UserVO> getUserVOList(List<User> userList);

    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(User user);

}

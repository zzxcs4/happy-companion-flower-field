package com.zzx.flowerfieldfrienddisk.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zzx.flowerfieldfrienddisk.common.BaseResponse;
import com.zzx.flowerfieldfrienddisk.model.dto.user.UserAvatarUploadRequest;
import com.zzx.flowerfieldfrienddisk.model.dto.user.UserQueryRequest;
import com.zzx.flowerfieldfrienddisk.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zzx.flowerfieldfrienddisk.model.vo.LoginUserVO;
import com.zzx.flowerfieldfrienddisk.model.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

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

    BaseResponse<Boolean> sign(HttpServletRequest request, Long spaceId);

    BaseResponse<Integer> signCount(HttpServletRequest request,Long spaceId);
    
    /**
     * 访问社区时自动签到
     *
     * @param userId  用户id
     * @param spaceId 社区空间id
     */
    void autoSignOnVisit(Long userId, Long spaceId);
    
    /**
     * 检查用户今日是否已签到
     *
     * @param userId  用户id
     * @param spaceId 空间id
     * @return true=已签到
     */
    boolean hasSignedToday(Long userId, Long spaceId);

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

     List<UserVO> searchUsersByTags(List<String> tagNameList,String username);

    List<UserVO> matchUsers(long num, User loginUser);

    String uploadUserAvatar(MultipartFile multipartFile, UserAvatarUploadRequest userAvatarUploadRequest,HttpServletRequest request);
}

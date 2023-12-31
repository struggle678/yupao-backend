package com.struggle.yupao.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.struggle.yupao.common.BaseResponse;
import com.struggle.yupao.common.ErrorCode;
import com.struggle.yupao.common.ResultUtils;
import com.struggle.yupao.exception.BusinessException;
import com.struggle.yupao.model.domain.User;
import com.struggle.yupao.model.request.UserLoginRequest;
import com.struggle.yupao.model.request.UserRegisterRequest;
import com.struggle.yupao.model.vo.UserVO;
import com.struggle.yupao.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

import static com.struggle.yupao.contant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户接口
 * @author Mr.Chen
 */
@RestController
@RequestMapping("/user")
//默认允许所有的域名连接（解决前后端跨域问题）,这里设置了只允许前端（"http://localhost:5173"）访问
//allowCredentials: 指定是否允许携带凭据（如 cookies、HTTP 认证）进行跨域请求。设置为 true 表示允许，设置为 false 表示不允许。
//@CrossOrigin(origins = {"http://localhost:3000"}, allowCredentials = "true")
@Slf4j
public class UserController {
    @Resource
    private UserService userService;
    @Resource
    private RedisTemplate<String,Object> redisTemplate;
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest){
        if(userRegisterRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        if(StringUtils.isAnyBlank(userAccount,userPassword,checkPassword)){
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest,HttpServletRequest request){
        if(userLoginRequest == null){
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if(StringUtils.isAnyBlank(userAccount,userPassword)){
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(user);
    }
    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request){
        if(request == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int result = userService.userLogout(request);
        return ResultUtils.success(result);
    }
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request){
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if(currentUser ==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        long userId = currentUser.getId();
        //todo 校验用户是否合法
        User user = userService.getById(userId);
        User safetyUser = userService.getSafetyUser(user);
        return ResultUtils.success(safetyUser);

    }
    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String username,HttpServletRequest request){
        //鉴权，仅管理员可查询
        if(!userService.isAdmin(request)){
           throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if(StringUtils.isNotBlank(username)){
            queryWrapper.like("username",username);
        }
        List<User> userList = userService.list(queryWrapper);
        List<User> list = userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(list);
    }
    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUsersByTags(@RequestParam(required = false) List<String> tagNameList,HttpServletRequest request){
        //判断用户是否登录
        User loginUser = userService.getLoginUser(request);
        if(loginUser == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        if(CollectionUtils.isEmpty(tagNameList)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<User> userList = userService.searchUserByTags(tagNameList);
        return ResultUtils.success(userList);

    }
    //todo 推荐多个，未实现
    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommendUsers(long pageSize,long pageNum,HttpServletRequest request){
//        User loginUser = userService.getLoginUser(request);

//        String redisKey = String.format("yupao:user:recommend:%s",loginUser.getId());
//        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
//        //如果有缓存，直接读缓存
//        Page<User> userPage =(Page<User>)redisTemplate.opsForValue().get(redisKey);
//        if(userPage!=null){
//            return ResultUtils.success(userPage);
//        }
        //无缓存，查数据库
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        Page<User> userPage = userService.page(new Page<>(pageNum,pageSize),queryWrapper);
        //写缓存
//        try {
//            valueOperations.set(redisKey,userPage,30000, TimeUnit.MINUTES);
//        } catch (Exception e) {
//            log.error("redis set key error",e);
//        }
        return ResultUtils.success(userPage);
    }

    /**
     * 查询所有脱敏后的用户
     * @return
     */
    @GetMapping("/list")
    public BaseResponse<List<UserVO>> userList(HttpServletRequest request){
        //判断用户是否登录
        User loginUser = userService.getLoginUser(request);
        if(loginUser == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        List<UserVO> userList = userService.userList();
        return ResultUtils.success(userList);
    }
    @PostMapping("/update")
    public BaseResponse<Integer> updateUser(@RequestBody User user,HttpServletRequest request){
        //校验参数是否为空
        if(user == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);

        int result = userService.updateUser(user,loginUser);
        return ResultUtils.success(result);
    }
    @PostMapping("/delete")
//    @DeleteMapping("/delete/{id}")
    public BaseResponse<Boolean> deleteUsers(@RequestBody long id,HttpServletRequest request){
        //鉴权，仅管理员可查询
       if(!userService.isAdmin(request)){
           throw new BusinessException(ErrorCode.NO_AUTH);
       }
        if (id<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 获取最匹配的用户
     * @param num
     * @param request
     * @return
     */
    @GetMapping("/match")
    public BaseResponse<List<User>> matchUsers(long num, HttpServletRequest request){
        if(num <=0 || num > 20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.matchUsers(num,user));

    }

}

package top.longmarch.sys.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.longmarch.core.annotation.Log;
import top.longmarch.core.common.Result;
import top.longmarch.core.utils.PasswordUtil;
import top.longmarch.sys.entity.User;
import top.longmarch.sys.entity.vo.ChangeStatusDTO;
import top.longmarch.sys.entity.vo.ChangeUserPasswordDTO;
import top.longmarch.sys.service.IUserService;

import java.util.Arrays;
import java.util.Map;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 用户信息 前端控制器
 * </p>
 *
 * @author YuYue
 * @since 2020-01-12
*/
@Api(value = "用户信息模块", tags = "用户信息模块接口")
@RestController
@RequestMapping("/sys/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    @Autowired
    private IUserService userService;

    @ApiOperation(value="搜索用户信息")
    @PostMapping("/search")
    public Result search(@RequestBody(required = false) Map<String, Object> params) {
        IPage<User> userPage = userService.search(params);
        return Result.ok().add(userPage);
    }

    @ApiOperation(value="查看用户信息")
    @RequiresPermissions("sys:user:show")
    @GetMapping("/show/{id}")
    public Result show(@PathVariable("id")Long id) {
        User user = userService.getById(id);
        return Result.ok().add(user);
    }

    @Log
    @ApiOperation(value="修改用户密码")
    @RequiresPermissions("sys:user:change:password")
    @PostMapping("/changePassword")
    public Result changePassword(@RequestBody ChangeUserPasswordDTO changeUserPasswordDTO) {
        log.info("修改用户密码, 入参：{}", changeUserPasswordDTO);
        User user = new User();
        changeUserPasswordDTO.setPassword(PasswordUtil.password(changeUserPasswordDTO.getPassword()));
        BeanUtils.copyProperties(changeUserPasswordDTO, user);
        userService.updateById(user);
        return Result.ok().add(user);
    }

    @Log
    @ApiOperation(value="修改用户状态")
    @RequiresPermissions("sys:user:update")
    @PostMapping("/changeStatus")
    public Result changeStatus(@RequestBody ChangeStatusDTO changeStatusDTO) {
        log.info("修改用户状态, 入参：{}", changeStatusDTO);
        User user = new User();
        BeanUtils.copyProperties(changeStatusDTO, user);
        userService.updateById(user);
        return Result.ok().add(user);
    }

    @Log
    @ApiOperation(value="创建用户信息")
    @RequiresPermissions("sys:user:create")
    @PostMapping("/create")
    public Result create(@Validated @RequestBody User user) {
        log.info("创建用户信息, 入参：{}", user);
        userService.saveUser(user);
        return Result.ok().add(user);
    }

    @Log
    @ApiOperation(value="更新用户信息")
    @RequiresPermissions("sys:user:update")
    @PostMapping("/update")
    public Result update(@Validated @RequestBody User user) {
        log.info("更新用户信息, 入参：{}", user);
        userService.updateUser(user);
        return Result.ok().add(user);
    }

    @Log
    @ApiOperation(value="删除用户信息")
    @RequiresPermissions("sys:user:delete")
    @PostMapping("/delete")
    public Result delete(@RequestBody Long[] ids) {
        log.info("删除用户信息, ids={}", ids);
        userService.removeUser(Arrays.asList(ids));
        return Result.ok();
    }

}

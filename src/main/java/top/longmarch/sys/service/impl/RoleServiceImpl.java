package top.longmarch.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.longmarch.core.shiro.realm.CustomRealm;
import top.longmarch.core.utils.tree.TreeUtil;
import top.longmarch.sys.dao.RoleDao;
import top.longmarch.sys.entity.Role;
import top.longmarch.sys.entity.RolePermissionRel;
import top.longmarch.sys.entity.vo.PermissionTree;
import top.longmarch.sys.service.IRolePermissionRelService;
import top.longmarch.sys.service.IRoleService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 角色信息 服务实现类
 * </p>
 *
 * @author YuYue
 * @since 2020-01-12
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleDao, Role> implements IRoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleServiceImpl.class);
    private static String CREATE_MESSAGE = "创建角色[{}]权限{}完成";
    private static String DELETE_MESSAGE = "删除角色[{}]权限{}完成";
    private static String DELETE_ALL_MESSAGE = "删除角色[{}]所有权限完成";
    @Autowired
    private RoleDao roleDao;
    @Autowired
    private IRolePermissionRelService rolePermissionRelService;
    @Autowired
    private CustomRealm customRealm;

    @Override
    public Map<String, Object> selectRolePermissionById(Long roleId) {
        List<PermissionTree> permissionTrees = roleDao.selectRolePermissionById(roleId);
        Set<Long> checkedKeys = permissionTrees.stream().filter(permissionTree -> permissionTree.getChecked()).map(PermissionTree::getId).collect(Collectors.toSet());
        Map<String, Object> map = new HashMap<>();
        map.put("permissionTree", TreeUtil.list2Tree(permissionTrees));
        map.put("checkedKeys", checkedKeys);
        return map;
    }

    @Transactional
    @Override
    public void updateRole(Role role) {
        this.updateById(role);
        createRolePermissionsRel(role.getId(), role.getCheckedKeys());
        // 清除所有用户权限
        customRealm.clearCache();
    }

    @Transactional
    @Override
    public void saveRole(Role role) {
        this.save(role);
        createRolePermissionsRel(role.getId(), role.getCheckedKeys());
    }

    @Transactional
    @Override
    public void removeRole(List<Long> roleIds) {
        this.removeByIds(roleIds);
        rolePermissionRelService.remove(new LambdaQueryWrapper<RolePermissionRel>().in(RolePermissionRel::getRoleId, roleIds));
        // 清除所有用户权限
        customRealm.clearCache();
    }

    public void createRolePermissionsRel(Long roleId, List<Long> permissionIdList) {
        if (null == permissionIdList) {
            return;
        }
        // 查询角色与权限的关联集合
        List<RolePermissionRel> dbRolePermissionRelList = rolePermissionRelService.list(new LambdaQueryWrapper<RolePermissionRel>().eq(RolePermissionRel::getRoleId, roleId));
        List<RolePermissionRel> insertRolePermissionRelList = null;
        // 传参没有选择权限，直接删除角色已有的所有权限
        if (permissionIdList.size() == 0) {
            rolePermissionRelService.remove(new LambdaQueryWrapper<RolePermissionRel>().eq(RolePermissionRel::getRoleId, roleId));
            log.info(DELETE_ALL_MESSAGE, roleId);
        } else if (null == dbRolePermissionRelList || dbRolePermissionRelList.size() == 0) {
            // 如果数据库中没有角色和权限的关联关系，可以直接批量插入角色与权限的关联关系
            insertRolePermissionRelList = buildInsertRolePermissionRelList(roleId, permissionIdList);
            rolePermissionRelService.saveBatch(insertRolePermissionRelList);
            log.info(CREATE_MESSAGE, roleId, permissionIdList);
        } else {
            // 数据库中角色关联的权限ID集合
            List<Long> dbPermissionIdList = dbRolePermissionRelList.stream().map(RolePermissionRel::getPermissionId).collect(Collectors.toList());
            // 准备删除的权限ID集合
            List<Long> deletePermissionIdList = dbPermissionIdList.stream().filter(id -> !permissionIdList.contains(id)).collect(Collectors.toList());
            // 准备新插入的权限ID集合
            List<Long> insertPermissionIdList = permissionIdList.stream().filter(id -> !dbPermissionIdList.contains(id)).collect(Collectors.toList());
            if (deletePermissionIdList != null && deletePermissionIdList.size() > 0) {
                rolePermissionRelService.remove(new LambdaQueryWrapper<RolePermissionRel>().eq(RolePermissionRel::getRoleId, roleId).in(RolePermissionRel::getPermissionId, deletePermissionIdList));
                log.info(DELETE_MESSAGE, roleId, deletePermissionIdList);
            }
            if (insertPermissionIdList != null && insertPermissionIdList.size() > 0) {
                insertRolePermissionRelList = buildInsertRolePermissionRelList(roleId, insertPermissionIdList);
                rolePermissionRelService.saveBatch(insertRolePermissionRelList);
                log.info(CREATE_MESSAGE, roleId, insertPermissionIdList);
            }
        }
    }

    /**
     * 构造角色和权限的关联对象集合
     *
     * @param roleId
     * @param permissionIdList
     * @return
     */
    private List<RolePermissionRel> buildInsertRolePermissionRelList(
            Long roleId, List<Long> permissionIdList) {
        return permissionIdList.stream().map(e -> {
            RolePermissionRel rolePermissionRel = new RolePermissionRel();
            rolePermissionRel.setRoleId(roleId);
            rolePermissionRel.setPermissionId(e);
            return rolePermissionRel;
        }).collect(Collectors.toList());
    }

}
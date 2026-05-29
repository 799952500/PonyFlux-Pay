package com.payflow.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payflow.admin.dto.NotificationDTO;
import com.payflow.admin.entity.Notification;
import com.payflow.admin.entity.SysMenu;
import com.payflow.admin.entity.SysRoleMenu;
import com.payflow.admin.entity.SysUserRole;
import com.payflow.admin.enums.NotificationTypeEnum;
import com.payflow.admin.mapper.NotificationMapper;
import com.payflow.admin.mapper.SysMenuMapper;
import com.payflow.admin.mapper.SysRoleMenuMapper;
import com.payflow.admin.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 站内通知服务，负责通知写入（异步/幂等）、查询、标记已读。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    /**
     * 异步写入通知，幂等去重（同 bizType+bizKey+recipientUserId 不重复）。
     */
    @Async
    public void send(NotificationTypeEnum type, String bizKey, String title,
                     String summary, String link, String merchantId,
                     List<Long> recipientUserIds) {
        if (recipientUserIds == null || recipientUserIds.isEmpty()) {
            return;
        }
        for (Long userId : recipientUserIds) {
            try {
                boolean exists = notificationMapper.selectCount(
                        new LambdaQueryWrapper<Notification>()
                                .eq(Notification::getBizType, type.name())
                                .eq(Notification::getBizKey, bizKey)
                                .eq(Notification::getRecipientUserId, userId)
                ) > 0;
                if (exists) {
                    continue;
                }
                Notification n = new Notification();
                n.setRecipientUserId(userId);
                n.setMerchantId(merchantId);
                n.setBizType(type.name());
                n.setBizKey(bizKey);
                n.setTitle(title);
                n.setSummary(summary);
                n.setLink(link);
                n.setReadStatus(0);
                notificationMapper.insert(n);
            } catch (Exception e) {
                log.error("写入通知失败 type={} bizKey={} userId={}", type, bizKey, userId, e);
            }
        }
    }

    /**
     * 按权限码查找接收人并写入通知。
     */
    @Async
    public void sendToRole(NotificationTypeEnum type, String bizKey, String title,
                           String summary, String link, String merchantId,
                           String permCode) {
        List<Long> userIds = findUserIdsByPermission(permCode);
        if (userIds.isEmpty()) {
            log.warn("权限码 {} 未关联到任何用户，通知跳过 bizKey={}", permCode, bizKey);
            return;
        }
        send(type, bizKey, title, summary, link, merchantId, userIds);
    }

    /**
     * 通知分页列表。
     */
    public Map<String, Object> listByUser(Long userId, List<String> merchantScope,
                                          String readFilter, String typeFilter,
                                          int page, int size) {
        LambdaQueryWrapper<Notification> qw = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getRecipientUserId, userId);

        applyMerchantScope(qw, merchantScope);

        if ("true".equalsIgnoreCase(readFilter)) {
            qw.eq(Notification::getReadStatus, 1);
        } else if ("false".equalsIgnoreCase(readFilter)) {
            qw.eq(Notification::getReadStatus, 0);
        }
        if (typeFilter != null && !typeFilter.isBlank()) {
            qw.eq(Notification::getBizType, typeFilter);
        }
        qw.orderByDesc(Notification::getCreatedAt);

        Page<Notification> pageResult = notificationMapper.selectPage(new Page<>(page, size), qw);

        List<NotificationDTO> dtos = pageResult.getRecords().stream()
                .map(this::toDTO).toList();

        Map<String, Object> data = new LinkedHashMap<>(4);
        data.put("list", dtos);
        data.put("total", pageResult.getTotal());
        data.put("page", page);
        data.put("size", size);
        return data;
    }

    /**
     * 未读总数。
     */
    public long countUnread(Long userId, List<String> merchantScope) {
        LambdaQueryWrapper<Notification> qw = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getRecipientUserId, userId)
                .eq(Notification::getReadStatus, 0);
        applyMerchantScope(qw, merchantScope);
        return notificationMapper.selectCount(qw);
    }

    /**
     * 单条标记已读。
     *
     * @return true 如果标记成功，false 如果通知不存在或不属于该用户
     */
    public boolean markRead(Long id, Long userId) {
        int rows = notificationMapper.update(null,
                new LambdaUpdateWrapper<Notification>()
                        .eq(Notification::getId, id)
                        .eq(Notification::getRecipientUserId, userId)
                        .eq(Notification::getReadStatus, 0)
                        .set(Notification::getReadStatus, 1)
                        .set(Notification::getReadAt, LocalDateTime.now()));
        return rows > 0;
    }

    /**
     * 标记当前用户全部未读为已读。
     */
    public int markAllRead(Long userId, List<String> merchantScope) {
        LambdaUpdateWrapper<Notification> uw = new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getRecipientUserId, userId)
                .eq(Notification::getReadStatus, 0)
                .set(Notification::getReadStatus, 1)
                .set(Notification::getReadAt, LocalDateTime.now());
        applyMerchantScopeUpdate(uw, merchantScope);
        return notificationMapper.update(null, uw);
    }

    /**
     * 批量标记已读。
     */
    public int markBatchRead(List<Long> ids, Long userId) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return notificationMapper.update(null,
                new LambdaUpdateWrapper<Notification>()
                        .in(Notification::getId, ids)
                        .eq(Notification::getRecipientUserId, userId)
                        .eq(Notification::getReadStatus, 0)
                        .set(Notification::getReadStatus, 1)
                        .set(Notification::getReadAt, LocalDateTime.now()));
    }

    /**
     * 清理 N 天前的已读通知。
     */
    public int cleanupOldRead(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return notificationMapper.delete(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getReadStatus, 1)
                        .lt(Notification::getCreatedAt, cutoff));
    }

    private NotificationDTO toDTO(Notification n) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(n.getId());
        dto.setBizType(n.getBizType());
        dto.setTitle(n.getTitle());
        dto.setSummary(n.getSummary());
        dto.setLink(n.getLink());
        dto.setReadStatus(n.getReadStatus());
        dto.setCreatedAt(n.getCreatedAt() != null
                ? n.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        return dto;
    }

    /**
     * 通过权限码查找拥有该权限的全部用户 ID。
     * 查询链路：admin_sys_menus(perm_code) → admin_sys_role_menus → admin_sys_user_roles
     */
    private List<Long> findUserIdsByPermission(String permCode) {
        List<SysMenu> menus = sysMenuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getPermCode, permCode));
        if (menus.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> menuIds = menus.stream().map(SysMenu::getId).toList();

        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getMenuId, menuIds));
        if (roleMenus.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = roleMenus.stream().map(SysRoleMenu::getRoleId).distinct().toList();

        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getRoleId, roleIds));
        return userRoles.stream().map(SysUserRole::getUserId).distinct().toList();
    }

    private void applyMerchantScope(LambdaQueryWrapper<Notification> qw, List<String> merchantScope) {
        if (merchantScope != null) {
            if (merchantScope.isEmpty()) {
                qw.eq(Notification::getId, -1);
            } else {
                qw.and(w -> w.in(Notification::getMerchantId, merchantScope)
                        .or().isNull(Notification::getMerchantId));
            }
        }
    }

    private void applyMerchantScopeUpdate(LambdaUpdateWrapper<Notification> uw, List<String> merchantScope) {
        if (merchantScope != null) {
            if (merchantScope.isEmpty()) {
                uw.eq(Notification::getId, -1);
            } else {
                uw.and(w -> w.in(Notification::getMerchantId, merchantScope)
                        .or().isNull(Notification::getMerchantId));
            }
        }
    }
}

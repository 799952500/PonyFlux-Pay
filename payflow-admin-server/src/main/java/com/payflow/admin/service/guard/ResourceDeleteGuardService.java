package com.payflow.admin.service.guard;

import java.util.List;

/**
 * 删除前资源依赖检查。
 */
public interface ResourceDeleteGuardService {

    ResourceDeleteCheckResult check(ResourceType type, Object resourceId, List<String> merchantScopeIds);

    ResourceDeleteCheckResult check(ResourceType type, Object resourceId);

    void assertDeletable(ResourceType type, Object resourceId, List<String> merchantScopeIds);

    void assertDeletable(ResourceType type, Object resourceId);
}

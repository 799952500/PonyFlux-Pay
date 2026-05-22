package com.payflow.admin.exception;

import com.payflow.admin.service.guard.ResourceDeleteCheckResult;
import lombok.Getter;

/**
 * 资源存在未解除关联，禁止删除。
 */
@Getter
public class ResourceDependencyException extends RuntimeException {

    public static final int CODE = 6006;

    private final ResourceDeleteCheckResult result;

    public ResourceDependencyException(ResourceDeleteCheckResult result) {
        super(result != null && result.getSummary() != null
                ? result.getSummary()
                : "存在未解除的关联，无法删除");
        this.result = result;
    }
}

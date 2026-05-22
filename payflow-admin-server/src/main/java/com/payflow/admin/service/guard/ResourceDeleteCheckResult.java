package com.payflow.admin.service.guard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 删除前依赖检查结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDeleteCheckResult {

    private boolean blocked;

    private String summary;

    @Builder.Default
    private List<ResourceRefDTO> refs = new ArrayList<>();

    public static ResourceDeleteCheckResult ok() {
        return ResourceDeleteCheckResult.builder()
                .blocked(false)
                .summary("可以安全删除")
                .refs(List.of())
                .build();
    }

    public static ResourceDeleteCheckResult blocked(String summary, List<ResourceRefDTO> refs) {
        return ResourceDeleteCheckResult.builder()
                .blocked(true)
                .summary(summary)
                .refs(refs == null ? List.of() : refs)
                .build();
    }
}

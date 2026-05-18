package com.payflow.cashier.arch;

import com.payflow.cashier.service.ResourceOwnershipService;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

/**
 * FR-020：含资源 ID 路径变量的 Controller 方法须由所有权拦截器或显式 Service 校验保护。
 */
@AnalyzeClasses(packages = "com.payflow.cashier.controller", importOptions = ImportOption.DoNotIncludeTests.class)
class MerchantControllerArchTest {

    private static final Set<String> EXEMPT_CONTROLLERS = Set.of(
            "PayNotifyController",
            "CashierController",
            "ReceiptController",
            "PublicPaymentLinkController",
            "MerchantAuthController"
    );

    private static final Set<String> EXEMPT_METHODS = Set.of("getPaymentStatus");

    /** {@link com.payflow.cashier.middleware.MerchantResourceOwnershipInterceptor} 识别的路径变量 */
    private static final Set<String> INTERCEPTOR_PATH_VARS = Set.of("orderId", "paymentId", "refundId", "linkId");

    private static final DescribedPredicate<JavaMethod> HAS_RESOURCE_ID_PATH_VARIABLE = new DescribedPredicate<>(
            "has @PathVariable *Id parameter") {
        @Override
        public boolean test(JavaMethod method) {
            return method.getParameters().stream().anyMatch(MerchantControllerArchTest::isResourceIdPathVariable);
        }
    };

    private static final ArchCondition<JavaMethod> PROTECTED_BY_INTERCEPTOR_OR_SERVICE = new ArchCondition<>(
            "be protected by MerchantResourceOwnershipInterceptor or ResourceOwnershipService") {
        @Override
        public void check(JavaMethod method, ConditionEvents events) {
            String controller = method.getOwner().getSimpleName();
            if (EXEMPT_CONTROLLERS.contains(controller) || EXEMPT_METHODS.contains(method.getName())) {
                return;
            }
            boolean interceptorCovers = method.getParameters().stream()
                    .filter(MerchantControllerArchTest::isResourceIdPathVariable)
                    .map(MerchantControllerArchTest::pathVariableName)
                    .allMatch(INTERCEPTOR_PATH_VARS::contains);
            if (interceptorCovers) {
                return;
            }
            boolean callsOwnershipService = method.getMethodCallsFromSelf().stream()
                    .anyMatch(call -> call.getTargetOwner().isAssignableTo(ResourceOwnershipService.class));
            if (callsOwnershipService) {
                return;
            }
            String message = String.format(
                    "方法 %s.%s 含资源 ID 路径变量但未豁免、未由拦截器覆盖（%s），也未调用 ResourceOwnershipService",
                    controller, method.getName(), INTERCEPTOR_PATH_VARS);
            events.add(SimpleConditionEvent.violated(method, message));
        }
    };

    @ArchTest
    static final ArchRule resourceIdPathVariablesMustBeProtected = methods()
            .that(HAS_RESOURCE_ID_PATH_VARIABLE)
            .should(PROTECTED_BY_INTERCEPTOR_OR_SERVICE)
            .because("商户隔离：禁止新增未受保护的 BOLA 端点");

    private static boolean isResourceIdPathVariable(JavaParameter parameter) {
        if (!parameter.isAnnotatedWith(PathVariable.class)) {
            return false;
        }
        String name = pathVariableName(parameter);
        return name.endsWith("Id");
    }

    private static String pathVariableName(JavaParameter parameter) {
        PathVariable pathVariable = parameter.getAnnotationOfType(PathVariable.class);
        if (pathVariable != null && pathVariable.value() != null && !pathVariable.value().isBlank()) {
            return pathVariable.value();
        }
        JavaMethod owner = (JavaMethod) parameter.getOwner();
        int index = owner.getParameters().indexOf(parameter);
        return owner.reflect().getParameters()[index].getName();
    }
}

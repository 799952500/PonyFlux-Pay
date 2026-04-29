package com.payflow.cashier.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.entity.Order;
import com.payflow.cashier.mapper.OrderMapper;
import com.payflow.cashier.openservice.PayChannelOpenService;
import com.payflow.cashier.openservice.PayChannelOpenServiceLocator;
import com.payflow.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 支付回调统一入口：按订单渠道路由到 {channel}OpenService（如 alipayOpenService、wxpayOpenService）。
 *
 * @author Lucas
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/callbacks")
@RequiredArgsConstructor
public class PayNotifyController {

    /** 订单数据访问：用于根据 outTradeNo 定位订单与渠道 */
    private final OrderMapper orderMapper;

    /** 渠道开放服务定位器：根据订单渠道获取对应的 OpenService */
    private final PayChannelOpenServiceLocator openServiceLocator;

    /** 全部渠道开放服务集合：用于先解析 outTradeNo（避免依赖弱特征字段判断渠道） */
    private final List<PayChannelOpenService> openServices;

    /**
     * 统一支付回调入口。
     * <p>
     * 处理流程：
     * <ul>
     *     <li>先尝试让各渠道 OpenService 解析回调，拿到 outTradeNo</li>
     *     <li>根据 outTradeNo 查询订单，得到订单的 channel</li>
     *     <li>用 channel 定位到最终 OpenService，返回渠道要求的 reply 文本</li>
     * </ul>
     * </p>
     *
     * @param request 回调请求
     * @return 渠道应答（微信 SUCCESS/FAIL；支付宝 success/fail）
     */
    @PostMapping
    public String handleNotify(HttpServletRequest request) {
        // 1) 先解析 outTradeNo：这是“回调 -> 订单”的唯一可靠锚点
        PayChannelOpenService.NotifyHandleResult parsed = tryParseOnce(request);
        String outTradeNo = parsed == null ? null : parsed.outTradeNo();
        if (outTradeNo == null || outTradeNo.isBlank()) {
            throw new BizException(7001, "回调解析失败：无法提取 outTradeNo");
        }

        // 2) 根据 outTradeNo 查订单：拿到订单的支付渠道（用于精确路由）
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderId, outTradeNo));
        if (order == null) {
            throw new BizException(7002, "订单不存在: " + outTradeNo);
        }

        // 3) 使用“渠道 + OpenService”约定定位最终处理服务
        PayChannelOpenService target = openServiceLocator.requireByOrderChannel(order.getChannel());
        if (parsed != null && parsed.channelCode() != null
                && parsed.channelCode().equalsIgnoreCase(target.channelCode())) {
            // 4) 若解析服务与目标服务一致，则直接复用解析结果（避免重复读 body/重复解密）
            log.info("统一回调处理完成(复用解析结果): orderId={}, channel={}, openService={}, reply={}",
                    outTradeNo, order.getChannel(), target.getClass().getSimpleName(), parsed.reply());
            return parsed.reply();
        }

        // 5) 不一致则由目标服务重新处理（保险兜底）
        String reply = target.handleNotify(request);
        log.info("统一回调处理完成: orderId={}, channel={}, openService={}, reply={}",
                outTradeNo, order.getChannel(), target.getClass().getSimpleName(), reply);
        return reply;
    }

    /**
     * 尝试用各渠道 OpenService 解析回调并拿到 outTradeNo。
     * <p>
     * 这里允许部分渠道解析抛异常：只要有任意渠道成功解析出 outTradeNo 即可继续。
     * </p>
     *
     * @param request 回调请求
     * @return 解析结果（包含 outTradeNo），若全部失败则返回 null
     */
    private PayChannelOpenService.NotifyHandleResult tryParseOnce(HttpServletRequest request) {
        for (PayChannelOpenService s : openServices) {
            try {
                PayChannelOpenService.NotifyHandleResult r = s.parseAndHandleNotify(request);
                if (r != null && r.outTradeNo() != null && !r.outTradeNo().isBlank()) {
                    return r;
                }
            } catch (Exception e) {
                log.warn("渠道回调解析失败: openService={}", s.getClass().getSimpleName(), e);
            }
        }
        return null;
    }
}

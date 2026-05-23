-- =============================================================================
-- 商户回调（平台 → 商户）演示数据 — 可重复执行
-- 覆盖：成功 / 失败重试 / 未配置 / 处理中 / 待投递 / 退款通知 / HTTP 异常 / 长报文
-- =============================================================================
USE payflow_cashier;
SET NAMES utf8mb4;

DELETE FROM cashier_merchant_notify_attempt WHERE notify_id LIKE 'MN-DEMO-%';
DELETE FROM cashier_merchant_notify WHERE notify_id LIKE 'MN-DEMO-%';

INSERT INTO cashier_merchant_notify (
  notify_id, order_id, merchant_id, merchant_order_no, notify_type, notify_url,
  summary_status, attempt_count, last_attempt_at, last_fail_reason, last_response_preview,
  order_status_snapshot, notify_payload_status, created_at, updated_at
) VALUES
-- 支付成功（1 次即成功）
('MN-DEMO-0001', 'ORD-20260518-0001', 'M100001', 'MPO-240518-001', 'PAYMENT',
 'https://m100001.demo/callback/payment', 'SUCCESS', 1,
 DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL, '{"code":0,"message":"ok"}',
 'PAID', 'PAID', DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR)),
-- 支付失败（商户返回非 success，3 次重试）
('MN-DEMO-0002', 'ORD-20260518-0003', 'M100002', 'MPO-240518-003', 'PAYMENT',
 'https://m100002.demo/callback/payment', 'FAILED', 3,
 DATE_SUB(NOW(), INTERVAL 3 HOUR), 'RESPONSE_NOT_SUCCESS', 'fail',
 'PAID', 'PAID', DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_SUB(NOW(), INTERVAL 3 HOUR)),
-- 未配置回调地址
('MN-DEMO-0003', 'ORD-20260518-0002', 'M100001', 'MPO-240518-002', 'PAYMENT',
 NULL, 'NOT_CONFIGURED', 0, NULL, NULL, NULL,
 'CREATED', NULL, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR)),
-- 大额订单支付成功（2 次，首次超时后成功）
('MN-DEMO-0004', 'ORD-20260517-0005', 'M100001', 'MPO-240517-005', 'PAYMENT',
 'https://m100001.demo/callback/payment', 'SUCCESS', 2,
 DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, 'SUCCESS',
 'PAID', 'PAID', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
-- 培训课程支付成功
('MN-DEMO-0005', 'ORD-20260516-0006', 'M100003', 'MPO-240516-006', 'PAYMENT',
 'https://m100003.demo/openapi/notify', 'SUCCESS', 1,
 DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, 'OK',
 'PAID', 'PAID', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
-- 团建宴席
('MN-DEMO-0006', 'ORD-20260515-0008', 'M100002', 'MPO-240515-008', 'PAYMENT',
 'https://m100002.demo/hooks/pay', 'SUCCESS', 1,
 DATE_SUB(NOW(), INTERVAL 3 DAY), NULL, 'received',
 'PAID', 'PAID', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
-- 5 次重试仍失败（HTTP 502）
('MN-DEMO-0007', 'ORD-20260513-0010', 'M100003', 'MPO-240513-010', 'PAYMENT',
 'https://m100003.demo/openapi/notify', 'FAILED', 5,
 DATE_SUB(NOW(), INTERVAL 5 DAY), 'HTTP_ERROR', '502 Bad Gateway',
 'PAID', 'PAID', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
-- 同一订单：退款通知处理中
('MN-DEMO-0008', 'ORD-20260518-0001', 'M100001', 'MPO-240518-001', 'REFUND',
 'https://m100001.demo/callback/refund', 'IN_PROGRESS', 1,
 DATE_SUB(NOW(), INTERVAL 30 MINUTE), NULL, NULL,
 'PAID', 'REFUNDING', DATE_SUB(NOW(), INTERVAL 45 MINUTE), DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
-- 订单已过期，回调失败（连接超时）
('MN-DEMO-0009', 'ORD-20260517-0004', 'M100002', 'MPO-240517-004', 'PAYMENT',
 'https://m100002.demo/callback/payment', 'FAILED', 2,
 DATE_SUB(NOW(), INTERVAL 1 DAY), 'TIMEOUT', NULL,
 'EXPIRED', 'EXPIRED', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
-- 支付中订单，回调重试进行中
('MN-DEMO-0010', 'ORD-20260516-0007', 'M100001', 'MPO-240516-007', 'PAYMENT',
 'https://m100001.demo/callback/payment', 'IN_PROGRESS', 2,
 DATE_SUB(NOW(), INTERVAL 20 MINUTE), 'RESPONSE_NOT_SUCCESS', 'retry_later',
 'PAYING', 'PAYING', DATE_SUB(NOW(), INTERVAL 40 MINUTE), DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
-- 待投递（已创建汇总，尚未发起 HTTP）
('MN-DEMO-0011', 'ORD-20260514-0009', 'M100001', 'MPO-240514-009', 'PAYMENT',
 'https://m100001.demo/callback/payment', 'PENDING', 0,
 NULL, NULL, NULL,
 'FAILED', 'FAILED', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
-- 退款已成功通知
('MN-DEMO-0012', 'ORD-20260518-0003', 'M100002', 'MPO-240518-003', 'REFUND',
 'https://m100002.demo/callback/refund', 'SUCCESS', 1,
 DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, '{"refundStatus":"REFUNDED"}',
 'PAID', 'REFUNDED', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

INSERT INTO cashier_merchant_notify_attempt (
  notify_id, attempt_no, request_params, response_body, http_status,
  result_status, fail_reason_type, fail_reason_detail, duration_ms, truncated, created_at
) VALUES
('MN-DEMO-0001', 1,
 '{"orderId":"ORD-20260518-0001","merchantId":"M100001","merchantOrderNo":"MPO-240518-001","status":"PAID","amount":29900,"currency":"CNY","payTime":"2026-05-22T15:00:00+08:00","sign":"demo_a1b2****c3d4"}',
 '{"code":0,"message":"ok"}', 200, 'SUCCESS', NULL, NULL, 85, 0, DATE_SUB(NOW(), INTERVAL 2 HOUR)),

('MN-DEMO-0002', 1,
 '{"orderId":"ORD-20260518-0003","merchantId":"M100002","status":"PAID","sign":"demo_sign_masked"}',
 'fail', 200, 'FAILED', 'RESPONSE_NOT_SUCCESS', 'body=fail', 120, 0, DATE_SUB(NOW(), INTERVAL 4 HOUR)),
('MN-DEMO-0002', 2,
 '{"orderId":"ORD-20260518-0003","merchantId":"M100002","status":"PAID","sign":"demo_sign_masked"}',
 'fail', 200, 'FAILED', 'RESPONSE_NOT_SUCCESS', 'body=fail', 98, 0, DATE_SUB(DATE_SUB(NOW(), INTERVAL 3 HOUR), INTERVAL 55 MINUTE)),
('MN-DEMO-0002', 3,
 '{"orderId":"ORD-20260518-0003","merchantId":"M100002","status":"PAID","sign":"demo_sign_masked"}',
 'fail', 200, 'FAILED', 'RESPONSE_NOT_SUCCESS', 'body=fail', 110, 0, DATE_SUB(NOW(), INTERVAL 3 HOUR)),

('MN-DEMO-0004', 1,
 '{"orderId":"ORD-20260517-0005","merchantId":"M100001","status":"PAID","amount":128800,"sign":"demo_****"}',
 NULL, NULL, 'FAILED', 'TIMEOUT', 'Read timed out after 5000ms', 5002, 0, DATE_SUB(DATE_SUB(NOW(), INTERVAL 1 DAY), INTERVAL 10 MINUTE)),
('MN-DEMO-0004', 2,
 '{"orderId":"ORD-20260517-0005","merchantId":"M100001","status":"PAID","amount":128800,"sign":"demo_****"}',
 'SUCCESS', 200, 'SUCCESS', NULL, NULL, 156, 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),

('MN-DEMO-0005', 1,
 '{"orderId":"ORD-20260516-0006","merchantId":"M100003","merchantOrderNo":"MPO-240516-006","status":"PAID","amount":19900,"courseName":"Python 进阶班","sign":"wx9f****12ab"}',
 'OK', 200, 'SUCCESS', NULL, NULL, 72, 0, DATE_SUB(NOW(), INTERVAL 2 DAY)),

('MN-DEMO-0006', 1,
 '{"orderId":"ORD-20260515-0008","merchantId":"M100002","status":"PAID","amount":25600,"sign":"ali_****"}',
 'received', 200, 'SUCCESS', NULL, NULL, 95, 0, DATE_SUB(NOW(), INTERVAL 3 DAY)),

('MN-DEMO-0007', 1,
 '{"orderId":"ORD-20260513-0010","merchantId":"M100003","merchantOrderNo":"MPO-240513-010","status":"PAID","amount":49900,"payChannel":"WECHAT_PAY","extra":{"courseId":"COURSE-ARCH-01","studentName":"演示学员"},"sign":"A1B2****9F0E"}',
 '<html><body>502 Bad Gateway</body></html>', 502, 'FAILED', 'HTTP_ERROR', 'HTTP 502', 320, 0, DATE_SUB(DATE_SUB(NOW(), INTERVAL 5 DAY), INTERVAL 4 HOUR)),
('MN-DEMO-0007', 2,
 '{"orderId":"ORD-20260513-0010","merchantId":"M100003","status":"PAID","amount":49900,"sign":"A1B2****9F0E"}',
 '502 Bad Gateway', 502, 'FAILED', 'HTTP_ERROR', 'HTTP 502', 280, 0, DATE_SUB(DATE_SUB(NOW(), INTERVAL 5 DAY), INTERVAL 3 HOUR)),
('MN-DEMO-0007', 3,
 '{"orderId":"ORD-20260513-0010","merchantId":"M100003","status":"PAID","amount":49900,"sign":"A1B2****9F0E"}',
 '502 Bad Gateway', 502, 'FAILED', 'HTTP_ERROR', 'HTTP 502', 305, 0, DATE_SUB(DATE_SUB(NOW(), INTERVAL 5 DAY), INTERVAL 2 HOUR)),
('MN-DEMO-0007', 4,
 '{"orderId":"ORD-20260513-0010","merchantId":"M100003","status":"PAID","amount":49900,"sign":"A1B2****9F0E"}',
 '502 Bad Gateway', 502, 'FAILED', 'HTTP_ERROR', 'HTTP 502', 298, 0, DATE_SUB(DATE_SUB(NOW(), INTERVAL 5 DAY), INTERVAL 1 HOUR)),
('MN-DEMO-0007', 5,
 '{"orderId":"ORD-20260513-0010","merchantId":"M100003","status":"PAID","amount":49900,"sign":"A1B2****9F0E"}',
 '502 Bad Gateway', 502, 'FAILED', 'HTTP_ERROR', 'HTTP 502', 310, 0, DATE_SUB(NOW(), INTERVAL 5 DAY)),

('MN-DEMO-0008', 1,
 '{"orderId":"ORD-20260518-0001","refundId":"REF-20260518-001","merchantId":"M100001","status":"REFUNDING","refundAmount":9900,"sign":"demo_rf****01"}',
 NULL, NULL, 'IN_PROGRESS', NULL, '等待商户确认退款结果', NULL, 0, DATE_SUB(NOW(), INTERVAL 30 MINUTE)),

('MN-DEMO-0009', 1,
 '{"orderId":"ORD-20260517-0004","merchantId":"M100002","status":"EXPIRED","sign":"demo_sign_masked"}',
 NULL, NULL, 'FAILED', 'TIMEOUT', 'Connection timed out', 5001, 0, DATE_SUB(DATE_SUB(NOW(), INTERVAL 1 DAY), INTERVAL 2 HOUR)),
('MN-DEMO-0009', 2,
 '{"orderId":"ORD-20260517-0004","merchantId":"M100002","status":"EXPIRED","sign":"demo_sign_masked"}',
 NULL, NULL, 'FAILED', 'TIMEOUT', 'Connection timed out', 5003, 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),

('MN-DEMO-0010', 1,
 '{"orderId":"ORD-20260516-0007","merchantId":"M100001","status":"PAYING","amount":6600,"sign":"demo_sign_masked"}',
 'retry_later', 200, 'FAILED', 'RESPONSE_NOT_SUCCESS', 'body=retry_later', 88, 0, DATE_SUB(NOW(), INTERVAL 40 MINUTE)),
('MN-DEMO-0010', 2,
 '{"orderId":"ORD-20260516-0007","merchantId":"M100001","status":"PAYING","amount":6600,"sign":"demo_sign_masked"}',
 'retry_later', 200, 'FAILED', 'RESPONSE_NOT_SUCCESS', 'body=retry_later', 92, 0, DATE_SUB(NOW(), INTERVAL 20 MINUTE)),

('MN-DEMO-0012', 1,
 '{"orderId":"ORD-20260518-0003","refundId":"REF-20260517-001","merchantId":"M100002","status":"REFUNDED","refundAmount":4400,"sign":"demo_rf****02"}',
 '{"refundStatus":"REFUNDED","message":"accepted"}', 200, 'SUCCESS', NULL, NULL, 103, 0, DATE_SUB(NOW(), INTERVAL 1 DAY));

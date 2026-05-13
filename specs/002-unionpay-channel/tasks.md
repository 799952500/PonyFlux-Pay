# Tasks: 银联支付渠道完整接入

**Input**: Design documents from `/specs/002-unionpay-channel/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/unionpay-api.md

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US5, US2, US3, US4)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Module Structure Refactoring)

**Purpose**: Create `payflow-payment-channels` parent POM, migrate three channel modules

- [x] T001 Create `payflow-payment-channels/pom.xml` — parent POM (packaging=pom) aggregating wechat/alipay/union submodules, with `<parent>` pointing to root `payflow-parent`
- [x] T002 [P] `git mv payflow-payment-wechat payflow-payment-channels/payflow-payment-wechat` — move wechat module under channels
- [x] T003 [P] `git mv payflow-payment-alipay payflow-payment-channels/payflow-payment-alipay` — move alipay module under channels
- [x] T004 [P] `git mv payflow-payment-union payflow-payment-channels/payflow-payment-union` — move union module under channels
- [x] T005 Update wechat POM `<parent><relativePath>`: `../pom.xml` → `../../pom.xml` in `payflow-payment-channels/payflow-payment-wechat/pom.xml`
- [x] T006 [P] Update alipay POM `<parent><relativePath>`: `../pom.xml` → `../../pom.xml` in `payflow-payment-channels/payflow-payment-alipay/pom.xml`
- [x] T007 [P] Update union POM `<parent><relativePath>`: `../pom.xml` → `../../pom.xml` in `payflow-payment-channels/payflow-payment-union/pom.xml`
- [x] T008 Update root `pom.xml` — replace 3 channel module declarations (`payflow-payment-wechat`, `payflow-payment-alipay`, `payflow-payment-union`) with single `<module>payflow-payment-channels</module>` in `<modules>` section
- [x] T009 Update root `pom.xml` — in `<dependencyManagement>`, update scm/relative references if any channel modules use `<systemPath>` or relative paths to other modules
- [x] T010 Run `mvn -B -DskipTests compile` from repo root to verify the restructured project compiles
- [x] T011 [P] Update CI/CD scripts (e.g., `.github/workflows/`, `Jenkinsfile`, or `Dockerfile`) that reference old module paths — replace `payflow-payment-wechat` etc. with `payflow-payment-channels/payflow-payment-wechat`

**Checkpoint**: Project compiles successfully with new module structure. All existing functionality preserved.

---

## Phase 2: Foundational (Shared UnionPay Infrastructure)

**Purpose**: Shared code that ALL UnionPay user stories depend on. Must complete before any story work begins.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [x] T012 Add `UNION_QR("UNION_QR", "银联扫码支付")` enum value to `payflow-payment-core/src/main/java/com/payflow/payment/core/PayMethod.java`; update existing `UNION_H5` comment removing "SPI 占位"
- [x] T013 [P] Create `UnionPayApiConstants.java` — gateway URL constants, API paths, version strings in `payflow-payment-channels/payflow-payment-union/src/main/java/com/payflow/payment/union/UnionPayApiConstants.java`
- [x] T014 [P] Create `UnionPayAccountConfig.java` — POJO with fields: merId, signCertPath, signCertPassword, encryptCertPath, encryptCertPassword, gatewayUrl, notifyUrl, unionPublicKeyPath in `payflow-payment-channels/payflow-payment-union/src/main/java/com/payflow/payment/union/UnionPayAccountConfig.java`
- [x] T015 [P] Create `UnionPayConfigLoader.java` — static utility to parse `UnionPayAccountConfig` from `ChannelConfigHolder.getChannelConfig()` JSON string (use Hutool JSONUtil) in `payflow-payment-channels/payflow-payment-union/src/main/java/com/payflow/payment/union/UnionPayConfigLoader.java`
- [x] T016 Create `UnionPaySignature.java` — RSA-SHA256 signing (merchant private key) + verify (UnionPay public key) utility using Java `java.security.Signature`, read key from .pfx via `KeyStore` in `payflow-payment-channels/payflow-payment-union/src/main/java/com/payflow/payment/union/UnionPaySignature.java`
- [x] T017 Create `UnionPayHttpClient.java` — HTTP client for UnionPay gateway POST with form-encoded body, auto-sign, response parsing in `payflow-payment-channels/payflow-payment-union/src/main/java/com/payflow/payment/union/UnionPayHttpClient.java`
- [x] T018 Rewrite `UnionPayIntegration.java` — replace placeholder URL constant with accurate gateway info, add utility constants in `payflow-payment-channels/payflow-payment-union/src/main/java/com/payflow/payment/union/UnionPayIntegration.java`
- [x] T019 Update `payflow-payment-channels/payflow-payment-union/pom.xml` — add dependencies if needed (Hutool already from parent; ensure no extra SDK needed per research decision), update description text

**Checkpoint**: Shared foundation ready — UnionPay config loading, signing, HTTP client all available for user story implementation.

---

## Phase 3: User Story 5 — 商户配置银联支付账号 (Priority: P1) 🎯 MVP

**Goal**: Admin can configure UnionPay payment account parameters (merchant ID, certificate path, signing key, etc.) via admin backend and enable the channel for merchants.

**Independent Test**: Log in to admin backend, create a UnionPay payment account config, enable channel and route, verify the cashier page shows UnionPay as a payment option.

### Implementation for User Story 5

- [x] T020 [US5] Create SQL migration `payflow-admin-server/src/main/resources/sql/migrations/2026-05-12_unionpay-qr-method-seed.sql` — INSERT `UNION_QR` into `payment_methods` table, ensure `UNION_H5` record exists with correct channel_id linking to `channels` table where `channel_code = 'UNION_PAY'`
- [x] T021 [US5] Update `sql/full-reseed-payflow-demo.sql` — add `UNION_QR` seed data to `payment_methods` section; update `channels` record for `UNION_PAY` if needed; add demo `payment_accounts` record for `UNION_PAY` with example merId/cert paths
- [x] T022 [US5] Verify `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminChannelController.java` — confirm existing CRUD endpoints (`GET/POST/PUT/DELETE` on `/api/v1/admin/channels` and `/api/v1/admin/channels/accounts`) handle UNION_PAY channel type without code changes (generic CRUD should work)
- [x] T023 [US5] Verify `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminChannelRouteController.java` — confirm route assignment endpoints work for UNION_PAY accounts (generic, no changes expected)
- [x] T024 [US5] Verify `payflow-admin-client/src/pages/admin/channels.vue` — confirm UNION_PAY channel type renders correctly (existing placeholder emoji should work); add `merId` and `gatewayUrl` form fields to the account config editor if generic JSON editor is insufficient
- [x] T025 [US5] Verify `payflow-admin-client/src/pages/admin/channel-routes.vue` — confirm UNION_PAY accounts appear in route assignment dropdowns

**Checkpoint**: Admin can fully configure UnionPay accounts and assign them to merchants. Cashier shows UnionPay option (though payment itself still uses placeholder).

---

## Phase 4: User Story 1 — 商户通过银联H5收银 (Priority: P1) 🎯 MVP

**Goal**: Merchant's mobile web users can pay via UnionPay H5 — system returns UnionPay payment page URL, user completes payment, system processes async notification and updates order.

**Independent Test**: Create a test order with `payChannel=UNION_PAY, payMethod=UNION_H5`, verify the system returns a valid UnionPay gateway redirect URL. In sandbox, complete payment and verify async notification updates order status to PAID.

### Implementation for User Story 1

- [x] T026 [US1] Create `UnionPayH5Handler.java` — build H5 payment request params (version, encoding, txnType="01", bizType="000201", channelType="08", merId, orderId, txnAmt, txnTime, frontUrl, backUrl), sign with `UnionPaySignature`, submit to gateway, parse redirect response in `payflow-payment-channels/payflow-payment-union/src/main/java/com/payflow/payment/union/UnionPayH5Handler.java`
- [x] T027 [US1] Rewrite `UnionH5Strategy.java` — replace placeholder `PayResult` with real call: load config via `UnionPayConfigLoader`, call `UnionPayH5Handler.pay()`, map gateway response to `PayResult(REDIRECT, h5Url, channelTradeNo)` in `payflow-cashier-server/src/main/java/com/payflow/cashier/sdk/strategy/UnionH5Strategy.java`
- [x] T028 [US1] Update `UnionPayPaymentOpenService.java` — extend `pay()` method to validate not only `UNION_H5` but also handle it correctly, add detailed logging; fix `channelCode()` returns `"unionpay"` in `payflow-cashier-server/src/main/java/com/payflow/cashier/openservice/payment/impl/UnionPayPaymentOpenService.java`
- [x] T029 [US1] Create `UnionPayNotifyHelper.java` — parse `application/x-www-form-urlencoded` notification body, extract `signature` field, verify signature with UnionPay public key, check `respCode == "00"`, call `PayNotifyService.handlePaymentSuccess(outTradeNo, queryId)` in `payflow-cashier-server/src/main/java/com/payflow/cashier/sdk/unionpay/UnionPayNotifyHelper.java`
- [x] T030 [US1] Update `payflow-cashier-server/src/main/java/com/payflow/cashier/controller/PayNotifyController.java` — verify `POST /notify/unionpay` route delegates to `UnionPayNotifyHelper.parseNotify()` via `PayChannelOpenServiceLocator` (check if existing `dispatchChannelNotify` covers unionpay case)
- [x] T031 [US1] Update `payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/PHPPaymentServiceImpl.java` — verify `toNotifyChannelCode()` already returns `"unionpay"` for `CHANNEL_UNION_PAY` (should be present); add if missing
- [x] T032 [US1] Add `UnionPayPaymentOpenService` notify handling — implement `parseAndHandleNotify(HttpServletRequest)` method that calls `UnionPayNotifyHelper` and returns proper `NotifyResult`, register in `PayChannelOpenServiceLocator` if not already auto-discovered in `payflow-cashier-server/src/main/java/com/payflow/cashier/openservice/payment/impl/UnionPayPaymentOpenService.java`

**Checkpoint**: H5 payment flow works end-to-end: create order → get UnionPay redirect URL → pay in sandbox → notify received → order status updated.

---

## Phase 5: User Story 2 — 商户通过银联扫码收银 (Priority: P2)

**Goal**: Merchant's PC cashier page generates UnionPay QR code for user to scan with Cloud QuickPass App. QR code has configurable expiry time. User scans and pays, system processes notification.

**Independent Test**: Create test order with `payMethod=UNION_QR`, verify system returns QR code (URL or image). Scan with sandbox test tool, verify payment completes and order updates.

### Implementation for User Story 2

- [x] T033 [US2] Create `UnionPayQrHandler.java` — build QR payment request params (txnType="01", txnSubType="07" for QR, bizType="000000"), sign, submit to `backTransReq.do`, parse response for `qrCode` and `queryId` in `payflow-payment-channels/payflow-payment-union/src/main/java/com/payflow/payment/union/UnionPayQrHandler.java`
- [x] T034 [US2] Create `UnionQrStrategy.java` — Spring Bean named `"union_qrPayStrategy"`, implements `PayStrategy`, `getPayMethod()` returns `PayMethod.UNION_QR`, `pay()` calls `UnionPayQrHandler`, returns `PayResult(QR_CODE, qrCode, channelTradeNo)` with `qrExpireSeconds` in `payflow-cashier-server/src/main/java/com/payflow/cashier/sdk/strategy/UnionQrStrategy.java`
- [x] T035 [US2] Update `UnionPayPaymentOpenService.pay()` — add `UNION_QR` support: when `payMethod == UNION_QR`, route to `union_qrPayStrategy` in `payflow-cashier-server/src/main/java/com/payflow/cashier/openservice/payment/impl/UnionPayPaymentOpenService.java`
- [x] T036 [US2] Update `payflow-cashier-client` cashier page component — verify QR code display component renders `qrCode` URL from `UNION_QR` response (should reuse existing QR display pattern from WeChat/Alipay QR methods)

**Checkpoint**: QR payment flow works end-to-end: create order → get QR code → scan with sandbox → notify received → order updated.

---

## Phase 6: User Story 3 — 商户对银联订单进行退款 (Priority: P2)

**Goal**: Merchant can refund paid UnionPay orders (full or partial) via admin backend. Refund amount returns to user's payment account via original route.

**Independent Test**: Create and pay a UnionPay order, then initiate a refund from admin backend. Verify refund status transitions to SUCCESS after UnionPay async refund notification.

### Implementation for User Story 3

- [x] T037 [US3] Create `UnionPayRefundHandler.java` — build refund request params (txnType="04", txnSubType="00", origQryId from original payment, refund amount, orderId), sign, submit to `backTransReq.do`, parse response in `payflow-payment-channels/payflow-payment-union/src/main/java/com/payflow/payment/union/UnionPayRefundHandler.java`
- [x] T038 [US3] Update `UnionH5Strategy.refund()` — replace `throw BizException` placeholder with real call: load config, call `UnionPayRefundHandler.refund()`, return `RefundResult` in `payflow-cashier-server/src/main/java/com/payflow/cashier/sdk/strategy/UnionH5Strategy.java`
- [x] T039 [P] [US3] Update `UnionQrStrategy.refund()` — implement refund by delegating to `UnionPayRefundHandler.refund()` (or delegate to `UnionH5Strategy.refund()` since refund logic is same across both methods) in `payflow-cashier-server/src/main/java/com/payflow/cashier/sdk/strategy/UnionQrStrategy.java`
- [x] T040 [US3] Update `UnionPayPaymentOpenService.refund()` — replace `throw BizException("银联渠道暂未接入退款")` placeholder with real implementation: locate strategy by payMethod, call `strategy.refund()` in `payflow-cashier-server/src/main/java/com/payflow/cashier/openservice/payment/impl/UnionPayPaymentOpenService.java`
- [x] T041 [US3] Fix `RefundServiceImpl.normalizeChannelCode()` — add `union_pay` / `unionpay` case: when `n` equals `"union_pay"` or `"unionpay"`, return `"unionpay"` at line 364 in `payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/RefundServiceImpl.java`
- [x] T042 [US3] Update `UnionPayNotifyHelper` — add refund notification handling: parse refund result from UnionPay notification body, call `PayNotifyService.handleRefundSuccess()` or equivalent refund callback in `payflow-cashier-server/src/main/java/com/payflow/cashier/sdk/unionpay/UnionPayNotifyHelper.java`

**Checkpoint**: Refund flow works: admin initiates refund → UnionPay processes → refund notification updates refund status.

---

## Phase 7: User Story 4 — 银联账单下载与对账 (Priority: P3)

**Goal**: System automatically downloads UnionPay T-1 daily bill files, parses them, compares with local payment records, and generates reconciliation diff reports.

**Independent Test**: Trigger UnionPay reconciliation task, verify T-1 bill is downloaded and parsed, compare against local payments, verify diff report is generated with correct categories.

### Implementation for User Story 4

- [x] T043 [US4] Create `UnionPayBillService.java` — build bill download request params (txnType="76", fileType="00", settleDate="MMDD"), sign, submit to `fileTransReq.do`, handle ZIP decompression, return raw CSV content in `payflow-payment-channels/payflow-payment-union/src/main/java/com/payflow/payment/union/UnionPayBillService.java`
- [x] T044 [US4] Create `UnionpayReconChannelOpenService.java` — Spring Bean named `"unionpayReconChannelOpenService"`, implements `ReconChannelOpenService`, `channelCode()` returns `"unionpay"`, `downloadBill()` calls `UnionPayBillService`, `parseBill()` delegates to `UnionpayBillParser` in `payflow-recon-server/src/main/java/com/payflow/recon/openservice/bill/impl/UnionpayReconChannelOpenService.java`
- [x] T045 [US4] Create `UnionpayBillParser.java` — Spring Bean named `"unionpayBillParser"`, implements `BillParser`, parse UnionPay CSV columns (queryId→channel_trade_no, orderId→out_trade_no, txnAmt→amount, fee→fee, txnTime→trade_time, txnType→trade_type, settleDate→settle_date), return `List<ReconBillRecord>` in `payflow-recon-server/src/main/java/com/payflow/recon/parser/impl/UnionpayBillParser.java`
- [x] T046 [US4] Register `UnionpayReconChannelOpenService` in `ReconChannelOpenServiceLocator` — verify Spring auto-discovery picks up the bean by name convention `"unionpayReconChannelOpenService"`; if manual registration needed, update locator in `payflow-recon-server/src/main/java/com/payflow/recon/openservice/bill/ReconChannelOpenServiceLocator.java`
- [x] T047 [US4] Update `ReconTaskSeedService` — verify it generates reconciliation tasks for `UNION_PAY` channel (check if channel filtering logic auto-discovers new `ReconChannelOpenService` beans); add unionpay to channel list if hardcoded in `payflow-recon-server/src/main/java/com/payflow/recon/service/ReconTaskSeedService.java`
- [x] T048 [US4] Verify `ReconCompareService` and `ReconDiffHealService` — confirm they work generically with channel_code `"unionpay"` (no hardcoded WeChat/Alipay branches); test with sample unionpay bill records in `payflow-recon-server/src/main/java/com/payflow/recon/service/ReconCompareService.java`

**Checkpoint**: Reconciliation works: T-1 bill downloaded → parsed → compared → diffs recorded in `recon_diff`.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Documentation, validation, and integration testing across all user stories.

- [x] T049 [P] Update `docs/CONTRACT_MATRIX.md` — add UnionPay API mappings: `POST /api/v1/payment/create` (UNION_H5, UNION_QR), `POST /notify/unionpay`, `POST /api/v1/merchant/refund` (unionpay)
- [x] T050 [P] Update `payflow-admin-client/src/pages/admin/channels.vue` — add UnionPay config form fields (merId, gatewayUrl, cert path, cert password) if generic JSON editor is not sufficient; ensure UNION_PAY channel displays correctly
- [x] T051 Run end-to-end validation per `quickstart.md` — reset demo database, configure UnionPay sandbox account, test H5 payment flow, test QR payment flow, test refund flow, test reconciliation trigger
- [x] T052 [P] Add UnionPay error codes to `payflow-common/src/main/java/com/payflow/common/exception/BizException.java` error code registry (channel errors in 6xxx range: 6100-6199 for UnionPay specific errors)
- [x] T053 Verify all Constitution Check items from plan.md still pass — run through Principles I-V with completed code

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup (T010 must pass) — BLOCKS all user stories
- **US5 (Phase 3)**: Depends on Foundational (Phase 2) — Admin config first
- **US1 (Phase 4)**: Depends on US5 (channel must be configurable before testing payment)
- **US2 (Phase 5)**: Depends on Foundational (Phase 2) + US1 (reuses H5 notify infrastructure)
- **US3 (Phase 6)**: Depends on US1 (need completed payments to test refund)
- **US4 (Phase 7)**: Depends on US1+US2 (need transaction data to test reconciliation)
- **Polish (Phase 8)**: Depends on all user stories being complete

### User Story Dependencies

```
Setup (Phase 1)
  └── Foundational (Phase 2)
        ├── US5 (Phase 3) [P1] — Admin Config
        │     └── US1 (Phase 4) [P1] — H5 Payment
        │           ├── US2 (Phase 5) [P2] — QR Payment
        │           ├── US3 (Phase 6) [P2] — Refund
        │           └── US4 (Phase 7) [P3] — Reconciliation
        └── Polish (Phase 8)
```

### Within Each User Story

- Models/Handlers (in payment-union module) before Strategies (in cashier-server)
- Strategies before OpenService updates
- Core payment before notify handling
- Core payment before refund
- Story complete before moving to next priority

### Parallel Opportunities

- **Phase 1**: T002, T003, T004 (three git mv) can run in parallel
- **Phase 2**: T013, T014, T015 (constants, POJO, config loader) can run in parallel
- **Phase 4**: T026 and T029 can be developed in parallel (H5Handler + NotifyHelper are separate files)
- **Phase 5+6**: US2 (QR) and US3 (Refund) can be developed in parallel if different developers — US2 depends on US1's notify infrastructure, US3 depends on US1's payment records
- **Phase 8**: T049 and T050 can run in parallel (docs + UI)

---

## Parallel Example: Phase 2 Foundational

```bash
# Launch independent shared code tasks together:
Task: "Create UnionPayApiConstants.java in payflow-payment-channels/payflow-payment-union/..."
Task: "Create UnionPayAccountConfig.java in payflow-payment-channels/payflow-payment-union/..."
Task: "Create UnionPayConfigLoader.java in payflow-payment-channels/payflow-payment-union/..."

# Then sequential (dependencies):
Task: "Create UnionPaySignature.java (depends on AccountConfig)"
Task: "Create UnionPayHttpClient.java (depends on Signature)"
```

## Parallel Example: Phase 4 US1 + Phase 5 US2

```bash
# After Phase 4 (US1) notify infrastructure is done, US2 QR can proceed:
# US1 tasks: T026 (H5Handler), T027 (H5Strategy), T028 (OpenService), T029 (Notify)
# US2 tasks (parallel with late US1 tasks): T033 (QrHandler), T034 (QrStrategy)

# T029 (NotifyHelper) and T033 (QrHandler) can be worked on simultaneously — different files
```

---

## Implementation Strategy

### MVP First (US5 + US1 ONLY)

1. Complete Phase 1: Setup (module restructure)
2. Complete Phase 2: Foundational (shared infra)
3. Complete Phase 3: US5 (admin config)
4. Complete Phase 4: US1 (H5 payment)
5. **STOP and VALIDATE**: Test H5 payment end-to-end in sandbox
6. Deploy/demo if ready — **this is a working UnionPay H5 channel**

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. US5 + US1 → Test independently → **MVP: UnionPay H5 payment live**
3. Add US2 (QR) → Test independently → QR payment available
4. Add US3 (Refund) → Test independently → Full payment lifecycle
5. Add US4 (Recon) → Test independently → Financial safety net
6. Each story adds value without breaking previous stories

### Quick MVP Strategy

Phase 1 + Phase 2 + Phase 3 + Phase 4 = **28 tasks** (T001–T032) = Fully working UnionPay H5 channel with admin config.

---

## Notes

- [P] tasks = different files, no dependencies — can execute in parallel
- [Story] label maps task to specific user story for traceability
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- UnionPay sandbox availability: weekdays 9:00-18:00 only — plan integration testing accordingly
- Use Mock mode (`payflow.unionpay.mock=true`) for development when sandbox is unavailable
- `RefundServiceImpl.normalizeChannelCode()` fix (T041) is a one-line addition — do it early to avoid blocking

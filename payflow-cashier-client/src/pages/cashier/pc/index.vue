<template>
  <div class="cashier-page cashier-page--pc">
    <PcCashierHeader />

    <div class="pc-cashier__container">
      <nav v-if="cashierStore.orderInfo" class="pc-breadcrumb" :aria-label="t('cashier.breadcrumbAria')">
        <span>{{ t('cashier.title') }}</span>
        <span class="pc-breadcrumb__sep">/</span>
        <span class="pc-breadcrumb__current">{{ cashierStore.orderInfo.merchantName }}</span>
      </nav>

      <div v-if="cashierStore.isLoading" class="pc-cashier__loading">
        <el-skeleton animated :rows="10" />
      </div>

      <div v-else-if="loadError" class="pc-cashier__loading">
        <el-result icon="error" :title="t('cashier.loadFailed')" :sub-title="loadError">
          <template #extra>
            <el-button type="primary" @click="retryLoad">{{ t('cashier.retry') }}</el-button>
          </template>
        </el-result>
      </div>

      <div v-else-if="cashierStore.orderInfo" class="pc-cashier__layout pc-cashier__layout--ticket">
        <PcOrderPanel
          :info="cashierStore.orderInfo"
          :merchant-initial="merchantInitial"
          @expired="handleOrderExpired"
        />

        <div class="pc-ticket-divider" aria-hidden="true">
          <span class="pc-ticket-divider__line" />
          <span class="pc-ticket-divider__hole pc-ticket-divider__hole--top" />
          <span class="pc-ticket-divider__hole pc-ticket-divider__hole--bottom" />
        </div>

        <aside class="pc-pay-aside pc-pay-aside--ticket">
          <section class="pc-pay-card pc-pay-card--ticket-top">
            <h2 class="pc-pay-card__title">{{ t('cashier.selectPayment') }}</h2>
            <PaymentMethodList
              layout="desktop"
              :methods="cashierStore.orderInfo.paymentMethods"
              :selected="selectedMethod"
              @update:selected="selectedMethod = $event"
            />
          </section>

          <section class="pc-pay-card pc-pay-card--summary pc-pay-card--ticket-bottom">
            <div class="pc-pay-summary">
                  <div class="pc-pay-summary__row">
                    <span>{{ t('cashier.productAmount') }}</span>
                    <span>¥{{ formatAmount(cashierStore.orderInfo.amount) }}</span>
                  </div>
                  <div class="pc-pay-summary__row pc-pay-summary__row--total">
                    <span>{{ t('cashier.totalAmount') }}</span>
                    <span class="pc-pay-summary__amount">¥{{ formatAmount(cashierStore.orderInfo.amount) }}</span>
                  </div>
                </div>

                <button
                  type="button"
                  class="pc-pay-submit"
                  :disabled="!selectedMethod || cashierStore.isPaying || checkoutDeadlinePassed"
                  @click="handlePay"
                >
                  <span v-if="cashierStore.isPaying" class="pay-btn__loading">
                    <svg class="pay-btn__spinner" fill="none" viewBox="0 0 24 24">
                      <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" opacity="0.25" />
                      <path fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" opacity="0.75" />
                    </svg>
                    {{ t('cashier.paying') }}
                  </span>
                  <span v-else>{{ t('cashier.confirmPay') }}</span>
                </button>

                <ul class="pc-pay-tips">
                  <li>{{ t('cashier.tipAutoRedirect') }}</li>
                  <li>{{ t('cashier.tipSecurity') }}</li>
                  <li>{{ t('cashier.tipSupport') }}</li>
                </ul>
              </section>
        </aside>
      </div>
    </div>

    <footer class="pc-cashier__footer">
      <span>© {{ year }} PonyFlux Pay</span>
      <span class="pc-cashier__footer-sep">·</span>
      <span>{{ t('cashier.secureTransfer') }}</span>
    </footer>

    <CheckoutOverlays
      :show-q-r="showQR"
      :confirming="confirming"
      :pay-result="payResult"
      :cashier-store="cashierStore"
      @update:show-q-r="showQR = $event"
      @confirm-pay="handleConfirmPay"
      @retry="handleRetry"
    />
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useCashierCheckout } from '@/composables/useCashierCheckout'
import PcCashierHeader from './components/PcCashierHeader.vue'
import PcOrderPanel from './components/PcOrderPanel.vue'
import PaymentMethodList from '../components/PaymentMethodList.vue'
import CheckoutOverlays from '../components/CheckoutOverlays.vue'

const { t } = useI18n()
const year = new Date().getFullYear()

const {
  cashierStore,
  selectedMethod,
  payResult,
  confirming,
  showQR,
  loadError,
  merchantInitial,
  checkoutDeadlinePassed,
  handleOrderExpired,
  handlePay,
  handleConfirmPay,
  handleRetry,
  retryLoad,
  formatAmount,
} = useCashierCheckout('PC')
</script>

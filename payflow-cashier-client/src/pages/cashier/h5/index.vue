<template>
  <div class="cashier-page cashier-page--h5">
    <H5CashierNav />

    <main class="h5-main">
      <div v-if="cashierStore.isLoading" class="h5-skeleton">
        <el-skeleton animated :rows="8" />
      </div>

      <div v-else-if="loadError" class="h5-skeleton">
        <el-result icon="error" :title="t('cashier.loadFailed')" :sub-title="loadError">
          <template #extra>
            <el-button type="primary" @click="retryLoad">{{ t('cashier.retry') }}</el-button>
          </template>
        </el-result>
      </div>

      <template v-else-if="cashierStore.orderInfo">
        <section class="h5-hero">
          <div class="h5-merchant">
            <div class="h5-merchant__avatar">{{ merchantInitial }}</div>
            <div class="h5-merchant__text">
              <span class="h5-merchant__name">{{ cashierStore.orderInfo.merchantName }}</span>
              <span class="h5-merchant__label">{{ t('cashier.collectingFromYou') }}</span>
            </div>
          </div>

          <p class="h5-hero__subject">{{ cashierStore.orderInfo.subject }}</p>
          <div class="h5-hero__amount">
            <span class="h5-hero__currency">¥</span>
            <span class="h5-hero__int">{{ amountParts.int }}</span>
            <span class="h5-hero__dec">.{{ amountParts.dec }}</span>
          </div>
          <p v-if="cashierStore.orderInfo.body" class="h5-hero__body">{{ cashierStore.orderInfo.body }}</p>

          <div class="h5-hero__meta">
            <span
              class="h5-status"
              :class="checkoutDeadlinePassed ? 'h5-status--danger' : 'h5-status--pending'"
            >
              {{ checkoutDeadlinePassed ? t('order.expired') : t('order.pending') }}
            </span>
            <span class="h5-order-id">{{ cashierStore.orderInfo.orderId }}</span>
          </div>
        </section>

        <section class="h5-methods">
          <h3 class="h5-methods__title">{{ t('cashier.paymentMethods') }}</h3>
          <PaymentMethodList
            layout="h5"
            :methods="cashierStore.orderInfo.paymentMethods"
            :selected="selectedMethod"
            @update:selected="selectedMethod = $event"
          />
        </section>
      </template>
    </main>

    <footer v-if="cashierStore.orderInfo && !cashierStore.isLoading" class="h5-footer">
      <button
        type="button"
        class="h5-pay-btn"
        :disabled="!selectedMethod || cashierStore.isPaying || checkoutDeadlinePassed"
        @click="handlePay"
      >
        <span v-if="cashierStore.isPaying" class="pay-btn__loading">
          <svg class="pay-btn__spinner" fill="none" viewBox="0 0 24 24">
            <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" opacity="0.25" />
            <path fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" opacity="0.75" />
          </svg>
          {{ t('cashier.launchingPay') }}
        </span>
        <span v-else>{{ t('cashier.payNow') }} ¥{{ formatAmount(cashierStore.orderInfo.amount) }}</span>
      </button>
      <p class="h5-footer__trust">{{ t('cashier.securePayment') }}</p>
    </footer>

    <CheckoutOverlays
      :show-q-r="showQR"
      :confirming="confirming"
      :pay-result="payResult"
      :cashier-store="cashierStore"
      fullscreen-qr
      @update:show-q-r="showQR = $event"
      @confirm-pay="handleConfirmPay"
      @retry="handleRetry"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useCashierCheckout } from '@/composables/useCashierCheckout'
import H5CashierNav from './components/H5CashierNav.vue'
import PaymentMethodList from '../components/PaymentMethodList.vue'
import CheckoutOverlays from '../components/CheckoutOverlays.vue'

const { t } = useI18n()

const {
  cashierStore,
  selectedMethod,
  payResult,
  confirming,
  showQR,
  loadError,
  merchantInitial,
  checkoutDeadlinePassed,
  handlePay,
  handleConfirmPay,
  handleRetry,
  retryLoad,
  formatAmount,
} = useCashierCheckout('H5')

const amountParts = computed(() => {
  const info = cashierStore.orderInfo
  if (!info) return { int: '0', dec: '00' }
  const str = (info.amount / 100).toFixed(2)
  const [int, dec] = str.split('.')
  return {
    int: int.replace(/\B(?=(\d{3})+(?!\d))/g, ','),
    dec,
  }
})
</script>

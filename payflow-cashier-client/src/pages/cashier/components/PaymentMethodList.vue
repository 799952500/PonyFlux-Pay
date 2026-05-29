<template>
  <section
    class="checkout-pay-methods"
    :class="{
      'checkout-pay-methods--h5': layout === 'h5',
      'checkout-pay-methods--desktop': layout === 'desktop',
      'checkout-pay-methods--dense': (layout === 'desktop' || layout === 'h5') && methods.length > 5,
    }"
  >
    <h3 v-if="layout === 'pc'" class="checkout-section-title">{{ t('cashier.selectPayment') }}</h3>

    <div
      class="pay-options-scroller"
      :class="{
        'pay-options-scroller--desktop': layout === 'desktop',
        'pay-options-scroller--h5': layout === 'h5',
      }"
    >
      <div
        class="pay-options"
        role="listbox"
        :aria-label="layout === 'desktop' ? t('paymentMethods.listAria') : undefined"
      >
      <button
        v-for="method in methods"
        :key="method.methodCode"
        type="button"
        role="option"
        :aria-selected="selected === method.methodCode"
        class="pay-option"
        :class="{ 'pay-option--active': selected === method.methodCode }"
        @click="$emit('update:selected', method.methodCode)"
      >
        <PaymentChannelIcon :channel="method.channel" />

        <div class="pay-option__body">
          <span class="pay-option__name">{{ method.methodName }}</span>
          <span v-if="method.discount" class="pay-option__hint pay-option__hint--promo">
            {{ method.discount.name }}
          </span>
          <span v-else class="pay-option__hint">{{ t('paymentMethods.safeQuick') }}</span>
        </div>

        <div class="pay-option__tail">
          <span v-if="method.discount" class="pay-option__discount">
            -¥{{ (method.discount.amount / 100).toFixed(2) }}
          </span>
          <span class="pay-option__radio" :class="{ 'pay-option__radio--on': selected === method.methodCode }">
            <span v-if="selected === method.methodCode" class="pay-option__radio-dot" />
          </span>
        </div>
      </button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import PaymentChannelIcon from '@/components/PaymentChannelIcon.vue'
import type { PaymentMethod } from '@/types'

const { t } = useI18n()

withDefaults(
  defineProps<{
    methods: PaymentMethod[]
    selected: string
    layout?: 'pc' | 'h5' | 'desktop'
  }>(),
  { layout: 'pc' }
)

defineEmits<{
  (e: 'update:selected', value: string): void
}>()
</script>

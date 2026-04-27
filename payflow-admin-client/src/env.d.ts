/// <reference types="vite/client" />

declare module '@/api/channel' {
  export const listRoutes: any
  export const createRoute: any
  export const deleteRoute: any
  export const toggleRoute: any
  export const listChannels: any
  export const listAllAccounts: any
  export const getMerchantsSimple: any

  export const createAccount: any
  export const updateAccount: any
  export const toggleAccount: any
  export const deleteAccount: any
}

declare module '@/api/channel-api' {
  export const listChannels: any
}

declare module '@/api/payment-method' {
  export const listPaymentMethods: any
  export const createPaymentMethod: any
  export const updatePaymentMethod: any
  export const deletePaymentMethod: any
}

declare module '@/api/merchant-api' {
  export const listMerchantPayments: any
  export const createMerchantPayment: any
  export const deleteMerchantPayment: any
  export const toggleMerchantPayment: any
  export const listMerchants: any
}


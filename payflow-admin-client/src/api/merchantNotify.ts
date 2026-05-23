import request from './request'
import { DEFAULT_PAGE_SIZE } from '@/constants/pagination'

export interface MerchantNotifyListItem {
  notifyId: string
  orderId: string
  merchantId: string
  merchantOrderNo?: string
  notifyType: string
  notifyUrl?: string
  summaryStatus: string
  attemptCount: number
  lastAttemptAt?: string
  lastFailReason?: string
  lastResponsePreview?: string
  orderStatus?: string
  notifyPayloadStatus?: string
}

export interface MerchantNotifyListResponse {
  total: number
  page: number
  size: number
  list: MerchantNotifyListItem[]
}

export interface MerchantNotifyAttemptItem {
  attemptNo: number
  resultStatus: string
  failReasonType?: string
  failReasonDetail?: string
  httpStatus?: number
  durationMs?: number
  requestParams: Record<string, unknown> | string
  responseBody?: string
  truncated?: boolean
  createdAt?: string
}

export interface MerchantNotifyDetailResponse {
  summary: MerchantNotifyListItem & { createdAt?: string; updatedAt?: string }
  attempts: MerchantNotifyAttemptItem[]
}

export interface MerchantNotifyByOrderResponse {
  orderId: string
  orderStatus?: string
  summaries: MerchantNotifyListItem[]
}

export interface MerchantNotifyListQuery {
  merchantId?: string
  orderId?: string
  merchantOrderNo?: string
  notifyType?: string
  summaryStatus?: string
  startTime?: string
  endTime?: string
  page?: number
  size?: number
}

export const listMerchantNotifies = async (
  params: MerchantNotifyListQuery,
): Promise<MerchantNotifyListResponse> => {
  const data = (await request.get<MerchantNotifyListResponse>('/admin/merchant-notifies', {
    params,
  })) as unknown as MerchantNotifyListResponse
  return {
    total: Number(data?.total ?? 0),
    page: Number(data?.page ?? params.page ?? 1),
    size: Number(data?.size ?? params.size ?? DEFAULT_PAGE_SIZE),
    list: data?.list ?? [],
  }
}

export const getMerchantNotifyDetail = async (
  notifyId: string,
): Promise<MerchantNotifyDetailResponse> => {
  const data = (await request.get<MerchantNotifyDetailResponse>(
    `/admin/merchant-notifies/${encodeURIComponent(notifyId)}`,
  )) as unknown as MerchantNotifyDetailResponse
  return data
}

export const getMerchantNotifyByOrder = async (
  orderId: string,
  notifyType?: string,
): Promise<MerchantNotifyByOrderResponse> => {
  const data = (await request.get<MerchantNotifyByOrderResponse>(
    `/admin/merchant-notifies/by-order/${encodeURIComponent(orderId)}`,
    { params: notifyType ? { notifyType } : {} },
  )) as unknown as MerchantNotifyByOrderResponse
  return data
}

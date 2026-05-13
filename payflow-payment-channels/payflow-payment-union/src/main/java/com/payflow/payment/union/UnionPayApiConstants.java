package com.payflow.payment.union;

/**
 * 银联全渠道网关 API 常量。
 *
 * @author PayFlow Team
 */
public final class UnionPayApiConstants {

    private UnionPayApiConstants() {
    }

    /** 银联全渠道版本 */
    public static final String VERSION = "5.1.0";
    /** 字符编码 */
    public static final String ENCODING = "UTF-8";
    /** 签名方法：RSA-SHA256 */
    public static final String SIGN_METHOD_RSA2 = "01";

    /** 交易类型：消费 */
    public static final String TXN_TYPE_PAY = "01";
    /** 交易类型：退货（退款） */
    public static final String TXN_TYPE_REFUND = "04";
    /** 交易类型：查询 */
    public static final String TXN_TYPE_QUERY = "00";
    /** 交易类型：对账文件下载 */
    public static final String TXN_TYPE_FILE = "76";

    /** 交易子类型：默认 */
    public static final String TXN_SUB_TYPE_DEFAULT = "00";
    /** 交易子类型：H5 */
    public static final String TXN_SUB_TYPE_H5 = "01";
    /** 交易子类型：二维码 */
    public static final String TXN_SUB_TYPE_QR = "07";

    /** 业务类型：H5 */
    public static final String BIZ_TYPE_H5 = "000201";
    /** 业务类型：默认 */
    public static final String BIZ_TYPE_DEFAULT = "000000";

    /** 渠道类型：手机 */
    public static final String CHANNEL_TYPE_MOBILE = "08";

    /** 接入类型：商户直连 */
    public static final String ACCESS_TYPE_MERCHANT = "0";

    /** 文件类型：商户账单 */
    public static final String FILE_TYPE_MERCHANT_BILL = "00";

    /** 银联 API 路径 */
    /** 前台交易（H5 跳转） */
    public static final String PATH_FRONT_TRANS = "/gateway/api/frontTransReq.do";
    /** 后台交易（二维码、退款、查询） */
    public static final String PATH_BACK_TRANS = "/gateway/api/backTransReq.do";
    /** 文件传输（账单下载） */
    public static final String PATH_FILE_TRANS = "/gateway/api/fileTransReq.do";
    /** 查询交易 */
    public static final String PATH_QUERY_TRANS = "/gateway/api/queryTrans.do";

    /** 银联沙箱网关 */
    public static final String SANDBOX_GATEWAY_URL = "https://gateway.test.95516.com/gateway/api";
    /** 银联生产网关 */
    public static final String PRODUCTION_GATEWAY_URL = "https://gateway.95516.com/gateway/api";

    /** 响应码：成功 */
    public static final String RESP_CODE_SUCCESS = "00";
    /** 响应码：重复交易 */
    public static final String RESP_CODE_DUPLICATE = "03";
}

package com.payflow.common.exception;

import lombok.Getter;

/**
 * 业务异常（收银台、支付渠道模块共用）
  * @author Lucas
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}

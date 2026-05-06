package com.payflow.cashier.util;

/**
 * 金额分→中文大写转换工具类
 *
 * <p>将以分为单位的金额转换为中文大写金额字符串，例如：</p>
 * <ul>
 *   <li>10000 分 → "壹佰元整"</li>
 *   <li>10050 分 → "壹佰元零伍角整"</li>
 *   <li>10051 分 → "壹佰元零伍角壹分"</li>
 *   <li>10101 分 → "壹佰零壹元零壹分"</li>
 *   <li>0 分 → "零元整"</li>
 * </ul>
 *
 * <p>纯静态方法，线程安全。</p>
 *
 * @author PayFlow Team
 */
public final class AmountCnConverter {

    /** 中文数字字符 */
    private static final String[] CN_DIGITS = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};

    /** 中文单位（整数部分，对应个十百千万...） */
    private static final String[] CN_UNITS = {"", "拾", "佰", "仟", "万", "拾", "佰", "仟", "亿", "拾", "佰", "仟"};

    private AmountCnConverter() {
    }

    /**
     * 将以分为单位的金额转换为中文大写金额
     *
     * @param amountFen 金额（分），必须 >= 0
     * @return 中文大写金额字符串，末尾以"整"结尾
     * @throws IllegalArgumentException 如果金额为负数
     */
    public static String convert(long amountFen) {
        if (amountFen < 0) {
            throw new IllegalArgumentException("金额不能为负数: " + amountFen);
        }

        if (amountFen == 0) {
            return "零元整";
        }

        long yuan = amountFen / 100;
        int jiao = (int) ((amountFen % 100) / 10);
        int fen = (int) (amountFen % 10);

        StringBuilder sb = new StringBuilder();

        // 整数部分
        if (yuan > 0) {
            sb.append(convertIntegerPart(yuan));
            sb.append("元");
        }

        // 角分部分
        if (jiao == 0 && fen == 0) {
            sb.append("整");
        } else if (jiao == 0) {
            if (yuan > 0) {
                sb.append("零");
            }
            sb.append(CN_DIGITS[fen]).append("分");
        } else {
            sb.append(CN_DIGITS[jiao]).append("角");
            if (fen == 0) {
                sb.append("整");
            } else {
                sb.append(CN_DIGITS[fen]).append("分");
            }
        }

        return sb.toString();
    }

    /**
     * 转换整数部分为中文大写
     *
     * <p>逐位处理，正确处理"零"的读法和"万""亿"单位。</p>
     *
     * @param yuan 元部分金额（> 0）
     * @return 中文大写字符串
     */
    private static String convertIntegerPart(long yuan) {
        if (yuan == 0) {
            return "";
        }

        String numStr = String.valueOf(yuan);
        int len = numStr.length();
        StringBuilder sb = new StringBuilder();
        boolean zeroFlag = false; // 标记是否遇到了零
        boolean hasOutput = false; // 标记是否有输出（用于决定是否在万/亿前补零）

        for (int i = 0; i < len; i++) {
            int digit = numStr.charAt(i) - '0';
            int pos = len - 1 - i; // 当前位的权位（0=个位,1=十位...）

            if (digit == 0) {
                zeroFlag = true;
            } else {
                // 如果前面有零且已有输出，补一个"零"
                if (zeroFlag && hasOutput) {
                    sb.append("零");
                }
                zeroFlag = false;
                sb.append(CN_DIGITS[digit]).append(CN_UNITS[pos]);
                hasOutput = true;
            }

            // 万位、亿位必须输出单位（无论当前位是否为零）
            if (pos == 4 && yuan % 10000 != 0) {
                sb.append("万");
                zeroFlag = false;
                hasOutput = true;
            }
            if (pos == 8 && (yuan / 100000000) % 10000 != 0) {
                sb.append("亿");
                zeroFlag = false;
                hasOutput = true;
            }
        }

        return sb.toString();
    }
}

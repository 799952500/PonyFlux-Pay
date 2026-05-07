package com.payflow.admin.mapper.recon;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.admin.dto.recon.ReconAbnormalPageRow;
import com.payflow.admin.entity.recon.ReconDiffEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * recon_diff Mapper。
 *
 * @author PayFlow Team
 */
@Mapper
public interface ReconDiffEntityMapper extends BaseMapper<ReconDiffEntity> {

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM recon_diff d
            INNER JOIN recon_task t ON d.task_id = t.task_id
            WHERE t.bill_date = #{billDate}
            <if test="channel != null and channel != ''">AND t.channel = #{channel}</if>
            <if test="accountCode != null and accountCode != ''">AND t.account_code = #{accountCode}</if>
            <if test="handleStatus != null and handleStatus != ''">AND d.handle_status = #{handleStatus}</if>
            </script>
            """)
    long countAbnormalByBillDate(
            @Param("billDate") LocalDate billDate,
            @Param("channel") String channel,
            @Param("accountCode") String accountCode,
            @Param("handleStatus") String handleStatus);

    @Select("""
            <script>
            SELECT d.id AS diffId, d.task_id AS taskId, d.diff_type AS diffType,
                   d.channel_trade_no AS channelTradeNo, d.local_order_id AS localOrderId,
                   d.channel_amount AS channelAmount, d.local_amount AS localAmount,
                   d.handle_status AS handleStatus, d.suggested_action AS suggestedAction,
                   t.channel AS reconChannel, t.account_code AS accountCode, t.bill_date AS billDate
            FROM recon_diff d
            INNER JOIN recon_task t ON d.task_id = t.task_id
            WHERE t.bill_date = #{billDate}
            <if test="channel != null and channel != ''">AND t.channel = #{channel}</if>
            <if test="accountCode != null and accountCode != ''">AND t.account_code = #{accountCode}</if>
            <if test="handleStatus != null and handleStatus != ''">AND d.handle_status = #{handleStatus}</if>
            ORDER BY d.id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<ReconAbnormalPageRow> listAbnormalByBillDate(
            @Param("billDate") LocalDate billDate,
            @Param("channel") String channel,
            @Param("accountCode") String accountCode,
            @Param("handleStatus") String handleStatus,
            @Param("offset") long offset,
            @Param("limit") long limit);
}


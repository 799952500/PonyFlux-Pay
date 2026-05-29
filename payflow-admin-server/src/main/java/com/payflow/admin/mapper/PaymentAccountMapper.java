package com.payflow.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.admin.entity.PaymentAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Mapper
/**
 * @author Lucas
 */
public interface PaymentAccountMapper extends BaseMapper<PaymentAccount> {

    @Select("SELECT pa.*, c.channel_name " +
            "FROM payment_accounts pa " +
            "LEFT JOIN channels c ON pa.channel_id = c.id " +
            "ORDER BY pa.id")
    List<PaymentAccount> listWithChannelName();

    @Select("""
            <script>
            SELECT pa.*, c.channel_name
            FROM payment_accounts pa
            LEFT JOIN channels c ON pa.channel_id = c.id
            WHERE 1=1
            <if test="channelId != null">AND pa.channel_id = #{channelId}</if>
            <if test="keyword != null and keyword != ''">
              AND (pa.account_code LIKE CONCAT('%', #{keyword}, '%')
                OR pa.account_name LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="accountIds != null and accountIds.size() > 0">
              AND pa.id IN
              <foreach collection="accountIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            </if>
            <if test="accountIds != null and accountIds.size() == 0">
              AND 1=0
            </if>
            ORDER BY pa.id
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<PaymentAccount> pageWithChannelName(@Param("channelId") Long channelId,
                                             @Param("keyword") String keyword,
                                             @Param("accountIds") Collection<Long> accountIds,
                                             @Param("offset") long offset,
                                             @Param("limit") int limit);

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM payment_accounts pa
            WHERE 1=1
            <if test="channelId != null">AND pa.channel_id = #{channelId}</if>
            <if test="keyword != null and keyword != ''">
              AND (pa.account_code LIKE CONCAT('%', #{keyword}, '%')
                OR pa.account_name LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="accountIds != null and accountIds.size() > 0">
              AND pa.id IN
              <foreach collection="accountIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            </if>
            <if test="accountIds != null and accountIds.size() == 0">
              AND 1=0
            </if>
            </script>
            """)
    long countFiltered(@Param("channelId") Long channelId,
                       @Param("keyword") String keyword,
                       @Param("accountIds") Collection<Long> accountIds);

    @Select("SELECT pa.*, c.channel_name " +
            "FROM payment_accounts pa " +
            "LEFT JOIN channels c ON pa.channel_id = c.id " +
            "WHERE pa.id = #{id}")
    PaymentAccount getByIdWithChannelName(@Param("id") Long id);

    @Select("SELECT r.id, r.merchant_id, r.channel_id, r.payment_account_id, " +
            "       r.enabled, r.priority, r.description, r.created_at, r.updated_at, " +
            "       m.merchant_name, c.channel_name, " +
            "       pa.account_code, pa.account_name " +
            "FROM admin_channel_routes r " +
            "LEFT JOIN merchants m ON r.merchant_id = m.merchant_id " +
            "LEFT JOIN channels c ON r.channel_id = c.id " +
            "LEFT JOIN payment_accounts pa ON r.payment_account_id = pa.id " +
            "ORDER BY r.priority DESC, r.id ASC")
    List<Map<String, Object>> channelRouteListWithDetails();
}


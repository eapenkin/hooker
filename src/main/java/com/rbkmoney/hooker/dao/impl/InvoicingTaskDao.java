package com.rbkmoney.hooker.dao.impl;

import com.rbkmoney.hooker.dao.AbstractTaskDao;
import com.rbkmoney.hooker.dao.DaoException;
import com.rbkmoney.hooker.utils.FilterUtils;
import lombok.RequiredArgsConstructor;
import com.rbkmoney.swag_webhook_events.model.Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedRuntimeException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

/**
 * Created by jeckep on 17.04.17.
 */
@Slf4j
@Component
public class InvoicingTaskDao extends AbstractTaskDao {

    public InvoicingTaskDao(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    protected String getMessageTopic() {
        return Event.TopicEnum.INVOICESTOPIC.getValue();
    }

    //TODO limit invoices from hook
    public int[] saveBatch(List<Long> messageIds) throws DaoException {
        final String sql =
                " insert into hook.scheduled_task(message_id, queue_id, message_type)" +
                        " select m.id, q.id, w.topic" +
                        " from hook.message m" +
                        " join hook.webhook w on m.party_id = w.party_id and w.enabled and w.topic=CAST(:message_type as hook.message_topic)" +
                        " join hook.webhook_to_events wte on wte.hook_id = w.id" +
                        " join hook.invoicing_queue q on q.hook_id=w.id and q.enabled and q.invoice_id=m.invoice_id" +
                        " where m.id = :message_id " +
                        " and m.event_type = wte.event_type " +
                        " and (m.shop_id = wte.invoice_shop_id or wte.invoice_shop_id is null) " +
                        " and (m.invoice_status = wte.invoice_status or wte.invoice_status is null) " +
                        " and (m.payment_status = wte.invoice_payment_status or wte.invoice_payment_status is null)" +
                        " and (m.refund_status = wte.invoice_payment_refund_status or wte.invoice_payment_refund_status is null)" +
                        " ON CONFLICT (message_id, queue_id, message_type) DO NOTHING";

        MapSqlParameterSource[] sqlParameterSources = messageIds
                .stream()
                .map(id -> new MapSqlParameterSource()
                        .addValue("message_id", id)
                        .addValue("message_type", Event.TopicEnum.INVOICESTOPIC.getValue()))
                .toArray(MapSqlParameterSource[]::new);

        try {
            return jdbcTemplate.batchUpdate(sql, sqlParameterSources);
        } catch (NestedRuntimeException e) {
            throw new DaoException("Failed to create tasks for messageIds=" + messageIds, e);
        }
    }
}

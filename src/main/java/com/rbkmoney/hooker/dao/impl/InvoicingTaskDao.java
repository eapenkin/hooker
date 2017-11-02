package com.rbkmoney.hooker.dao.impl;

import com.rbkmoney.hooker.dao.AbstractTaskDao;
import com.rbkmoney.hooker.dao.DaoException;
import com.rbkmoney.swag_webhook_events.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedRuntimeException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import javax.sql.DataSource;

/**
 * Created by jeckep on 17.04.17.
 */
public class InvoicingTaskDao extends AbstractTaskDao {

    Logger log = LoggerFactory.getLogger(this.getClass());

    public InvoicingTaskDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    protected String getMessageTopic() {
        return Event.TopicEnum.INVOICESTOPIC.getValue();
    }

    @Override
    public void create(long messageId) {
        final String sql =
                " insert into hook.scheduled_task(message_id, hook_id, message_type)" +
                        " select m.id, w.id, '" + getMessageTopic() + "'" +
                        " from hook.message m" +
                        " join hook.webhook w on m.party_id = w.party_id and w.enabled" +
                        " join hook.webhook_to_events wte on wte.hook_id = w.id" +
                        " where m.id = :id " +
                        " and m.event_type = wte.event_type " +
                        " and (m.shop_id = wte.invoice_shop_id or wte.invoice_shop_id is null) " +
                        " and (m.invoice_status = wte.invoice_status or wte.invoice_status is null) " +
                        " and (m.payment_status = wte.invoice_payment_status or wte.invoice_payment_status is null)" +
                        " ON CONFLICT (message_id, hook_id) DO NOTHING";
        try {
            int updateCount = getNamedParameterJdbcTemplate().update(sql, new MapSqlParameterSource("id", messageId));
            log.debug("Created tasks count : " + updateCount);
        } catch (NestedRuntimeException e) {
            log.error("Fail to create tasks for messages messages.", e);
            throw new DaoException(e);
        }
    }
}
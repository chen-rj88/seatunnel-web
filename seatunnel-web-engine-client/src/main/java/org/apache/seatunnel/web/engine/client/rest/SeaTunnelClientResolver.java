package org.apache.seatunnel.web.engine.client.rest;

import jakarta.annotation.Resource;
import org.apache.seatunnel.web.dao.entity.SeaTunnelClient;
import org.apache.seatunnel.web.dao.repository.SeaTunnelClientDao;
import org.apache.seatunnel.web.engine.client.modal.SeaTunnelClientAuth;
import org.springframework.stereotype.Component;

import static org.apache.seatunnel.plugin.datasource.api.utils.PasswordUtils.decodePassword;

@Component
public class SeaTunnelClientResolver {

    @Resource
    private SeaTunnelClientDao seatunnelClientDao;

    public String resolveBaseApiUrl(Long clientId) {
        SeaTunnelClient entity = seatunnelClientDao.queryById(clientId);
        return entity.getBaseUrl();
    }

    public SeaTunnelClientAuth resolveAuth(Long clientId) {
        SeaTunnelClient client = seatunnelClientDao.selectById(clientId);

        SeaTunnelClientAuth auth = new SeaTunnelClientAuth();
        auth.setAuthEnabled(client.getAuthEnabled());
        auth.setUsername(client.getUsername());
        auth.setPassword(client.getPassword());
        return auth;
    }
}

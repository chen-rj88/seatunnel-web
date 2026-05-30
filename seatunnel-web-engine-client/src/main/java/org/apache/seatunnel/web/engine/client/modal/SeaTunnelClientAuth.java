package org.apache.seatunnel.web.engine.client.modal;

import lombok.Data;

@Data
public class SeaTunnelClientAuth {

    private Boolean authEnabled;

    private String username;

    private String password;
}
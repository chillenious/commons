package com.chillenious.common.metrics;

import com.chillenious.common.util.Duration;

import java.io.Serializable;

/**
 * Settings that are populated from settings like:
 * <code>metrics.graphite.property=value</code>
 */
public final class GraphiteSettings implements Serializable {

    private static final long serialVersionUID = 1L;

    private String host;

    private int port;

    private String apiKey;

    private boolean enabled;

    private String instanceName;

    private Duration reportingInterval = Duration.seconds(30);

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Duration getReportingInterval() {
        return reportingInterval;
    }

    public void setReportingInterval(Duration reportingInterval) {
        this.reportingInterval = reportingInterval;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    @Override
    public String toString() {
        return "GraphiteSettings{" +
                "apiKey='" + apiKey + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", reportingInterval=" + reportingInterval +
                '}';
    }
}

package com.infomate.app.agent

enum class HealthState {
    OPERATIONAL,
    DEGRADED,
    OFFLINE,
    RECOVERING,
    UNKNOWN
}

enum class HealthDetail {
    STABLE,
    AUTH_FAILURE,
    TIMEOUT,
    EMPTY_RESPONSE,
    PARSE_FAILURE,
    QUOTA_EXCEEDED,
    NETWORK_ERROR,
    COLD_START,
    NOT_FOUND,
    INVALID_HANDSHAKE
}

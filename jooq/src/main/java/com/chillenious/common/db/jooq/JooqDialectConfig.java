package com.chillenious.common.db.jooq;

import org.jooq.SQLDialect;

/**
 * Any extra configuration for factory creation that needs to be set at bootstrapping time.
 */
public final class JooqDialectConfig {

    private final SQLDialect dialect;

    public JooqDialectConfig(SQLDialect dialect) {
        this.dialect = dialect;
    }

    public SQLDialect getDialect() {
        return dialect;
    }
}

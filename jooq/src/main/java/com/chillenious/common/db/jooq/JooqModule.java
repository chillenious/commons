package com.chillenious.common.db.jooq;

import com.chillenious.common.db.DataSources;
import com.chillenious.common.db.DataSourcesModule;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.matcher.Matcher;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.RecordMapperProvider;
import org.jooq.SQLDialect;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.jooq.impl.DefaultRecordMapperProvider;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import static com.google.inject.matcher.Matchers.annotatedWith;
import static com.google.inject.matcher.Matchers.any;

/**
 * Sets up Jooq for various things in this package.
 * <p>
 * Also installs {@link DataSourcesModule}.
 * <p>
 * Optionally, you can pass in a qualifier
 */
public class JooqModule extends AbstractModule {

    public static final Settings JOOQ_SETTINGS = new Settings()
            .withRenderSchema(false)
            .withRenderFormatted(true)
            .withRenderNameStyle(RenderNameStyle.QUOTED);

    protected final SQLDialect dialect;

    protected final Named qualifier;

    /**
     * Construct using the {@link org.jooq.SQLDialect#MYSQL} dialect
     * and regular commit mode.
     */
    public JooqModule() {
        this(SQLDialect.MYSQL, null);
    }

    /**
     * Construct using the passed in dialect
     * and the passed in commit code
     *
     * @param dialect the dialect to use
     */
    public JooqModule(SQLDialect dialect) {
        this(dialect, null);
    }

    /**
     * Construct using the {@link org.jooq.SQLDialect#MYSQL} dialect
     * and regular commit mode.
     */
    public JooqModule(String qualifier) {
        this(SQLDialect.MYSQL, qualifier);
    }

    /**
     * Construct using the passed in dialect
     * and the passed in commit code
     *
     * @param dialect the dialect to use
     */
    public JooqModule(SQLDialect dialect, String qualifier) {
        this.dialect = dialect;
        if (qualifier != null) {
            this.qualifier = Names.named(qualifier);
        } else {
            this.qualifier = null;
        }
    }

    protected static class TxTemplateProvider implements Provider<TransactionTemplate> {

        @Inject
        private Injector injector;

        private final Named qualifier;

        TxTemplateProvider(Named qualifier) {
            this.qualifier = qualifier;
        }

        @Override
        public TransactionTemplate get() {
            TransactionTemplate template = new TransactionTemplate();
            template.setTransactionManager(getTxMgr(injector, qualifier));
            return template;
        }
    }

    static class TxMgrProvider implements Provider<DataSourceTransactionManager> {

        @Inject
        private Injector injector;

        private final Named qualifier;

        TxMgrProvider(Named qualifier) {
            this.qualifier = qualifier;
        }

        @Override
        public DataSourceTransactionManager get() {
            DataSource dataSource = getDataSource(injector, qualifier);
            return new DataSourceTransactionManager(
                    new TransactionAwareDataSourceProxy(dataSource));
        }
    }

    protected static class ConfigurationProvider implements Provider<Configuration> {

        @Inject
        private Injector injector;

        private final Named qualifier;

        private final RecordMapperProvider recordMapperProvider;

        ConfigurationProvider(Named qualifier,
                              RecordMapperProvider recordMapperProvider) {
            this.qualifier = qualifier;
            this.recordMapperProvider = recordMapperProvider;
        }

        @Override
        public Configuration get() {
            DataSource dataSource = getDataSource(injector, qualifier);
            JooqDialectConfig dialectConfig = getDialectConfig(injector, qualifier);
            return new DefaultConfiguration()
                    .set(new SpringConnectionProvider(dataSource))
                    .set(dialectConfig.getDialect())
                    .set(JOOQ_SETTINGS)
                    .set(new DefaultExecuteListenerProvider(new ExceptionTranslator(dataSource)))
                    .set(recordMapperProvider);
        }
    }

    protected static class DataSourceProvider implements Provider<DataSource> {

        @Inject
        private DataSources dataSources;

        private final Named qualifier;

        DataSourceProvider(Named qualifier) {
            this.qualifier = qualifier;
        }

        @Override
        public DataSource get() {
            return qualifier == null ?
                    dataSources.getDataSource("dataSource") :
                    dataSources.getDataSource(qualifier.value() + "DataSource");
        }
    }

    protected static class JooqDSLContextProvider implements Provider<DSLContext> {
        @Inject
        private Injector injector;

        private final Named qualifier;

        JooqDSLContextProvider(Named qualifier) {
            this.qualifier = qualifier;
        }

        @Override
        public DSLContext get() {
            DataSourceTransactionManager txMgr = getTxMgr(injector, qualifier);
            JooqDialectConfig dialectConfig = getDialectConfig(injector, qualifier);
            return DSL.using(txMgr.getDataSource(),
                    dialectConfig.getDialect(), JOOQ_SETTINGS);
        }
    }

    private static DataSource getDataSource(Injector injector, Named qualifier) {
        return qualifier == null ?
                injector.getInstance(DataSource.class) :
                injector.getInstance(Key.get(DataSource.class, qualifier));
    }

    private static JooqDialectConfig getDialectConfig(Injector injector, Named qualifier) {
        return qualifier == null ?
                injector.getInstance(JooqDialectConfig.class) :
                injector.getInstance(Key.get(JooqDialectConfig.class, qualifier));
    }

    private static DataSourceTransactionManager getTxMgr(Injector injector, Named qualifier) {
        return qualifier == null ?
                injector.getInstance(DataSourceTransactionManager.class) :
                injector.getInstance(Key.get(DataSourceTransactionManager.class, qualifier));
    }


    @Override
    protected void configure() {

        install(new DataSourcesModule());

        if (qualifier != null) {
            DataSourceProvider dataSourceProvider = new DataSourceProvider(qualifier);
            requestInjection(dataSourceProvider);
            bind(key(DataSource.class)).toProvider(dataSourceProvider);
        }

        TransactionalMethodInterceptor interceptor = configureTransactions();
        bindInterceptor(matcher(Transactional.class), any(), interceptor);
        bindInterceptor(any(), matcher(Transactional.class), interceptor);

        bind(key(JooqDialectConfig.class)).toInstance(new JooqDialectConfig(dialect));
        bind(key(Settings.class)).toInstance(JOOQ_SETTINGS);

        ConfigurationProvider configurationProvider =
                new ConfigurationProvider(qualifier, newRecordMapperProvider());
        requestInjection(configurationProvider);
        bind(key(Configuration.class)).toProvider(configurationProvider);

        TxTemplateProvider txTemplateProvider = new TxTemplateProvider(qualifier);
        bind(key(TransactionTemplate.class)).toProvider(txTemplateProvider);

        configureDslContextProvider();
    }

    protected void configureDslContextProvider() {
        JooqDSLContextProvider ctxProvider = new JooqDSLContextProvider(qualifier);
        requestInjection(ctxProvider);
        bind(key(DSLContext.class)).toProvider(ctxProvider);
    }

    protected RecordMapperProvider newRecordMapperProvider() {
        return new DefaultRecordMapperProvider();
    }

    protected TransactionalMethodInterceptor configureTransactions() {
        TransactionalMethodInterceptor interceptor = new TransactionalMethodInterceptor();
        requestInjection(interceptor);
        bind(key(TransactionDefinition.class)).to(DefaultTransactionDefinition.class);
        TxMgrProvider txMgrProvider = new TxMgrProvider(qualifier);
        bind(key(DataSourceTransactionManager.class)).toProvider(txMgrProvider);
        return interceptor;
    }

    /**
     * Get a matcher with optional scoping applied.
     */
    protected Matcher<AnnotatedElement> matcher(
            Class<? extends Annotation> annotationType) {
        return qualifier != null ?
                annotatedWith(annotationType).and(annotatedWith(qualifier)) :
                annotatedWith(annotationType);
    }

    /**
     * Gets key with optional scoping applied.
     */
    protected <T> Key<T> key(Class<T> type) {
        return qualifier != null ? Key.get(type, qualifier) : Key.get(type);
    }
}

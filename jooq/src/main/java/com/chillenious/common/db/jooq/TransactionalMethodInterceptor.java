/**
 * Copyright (c) 2009-2013, Data Geekery GmbH (http://www.datageekery.com)
 * All rights reserved.
 *
 * This work is dual-licensed
 * - under the Apache Software License 2.0 (the "ASL")
 * - under the jOOQ License and Maintenance Agreement (the "jOOQ License")
 * =============================================================================
 * You may choose which license applies to you:
 *
 * - If you're using this work with Open Source databases, you may choose
 *   either ASL or jOOQ License.
 * - If you're using this work with at least one commercial database, you must
 *   choose jOOQ License
 *
 * For more information, please visit http://www.jooq.org/licenses
 *
 * Apache Software License 2.0:
 * -----------------------------------------------------------------------------
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * jOOQ License and Maintenance Agreement:
 * -----------------------------------------------------------------------------
 * Data Geekery grants the Customer the non-exclusive, timely limited and
 * non-transferable license to install and use the Software under the terms of
 * the jOOQ License and Maintenance Agreement.
 *
 * This library is distributed with a LIMITED WARRANTY. See the jOOQ License
 * and Maintenance Agreement for more details: http://www.jooq.org/licensing
 */
package com.chillenious.common.db.jooq;

import com.google.inject.Inject;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;

/**
 * A {@link org.aopalliance.intercept.MethodInterceptor} that implements nested transactions.
 * <p/>
 * Only the outermost transactional method will <code>commit()</code> or
 * <code>rollback()</code> the contextual transaction. This can be verified
 * through {@link org.springframework.transaction.TransactionStatus#isNewTransaction()}, which returns
 * <code>true</code> only for the outermost transactional method call.
 * <p/>
 *
 * @author Lukas Eder
 * @author Eelco Hillenius
 */
class TransactionalMethodInterceptor implements MethodInterceptor {

    private static final TransactionAttributeSource annotationSource
            = new AnnotationTransactionAttributeSource(false);

    @Inject
    private DataSourceTransactionManager transactionManager;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (invocation.getThis() != null) {
            TransactionAttribute txAttribute =
                    annotationSource.getTransactionAttribute(
                            invocation.getMethod(), invocation.getThis().getClass());
            TransactionStatus transaction = transactionManager.getTransaction(txAttribute);
            try {
                Object result = invocation.proceed();
                try {
                    if (transaction.isNewTransaction()) {
                        transactionManager.commit(transaction);
                    }
                } catch (UnexpectedRollbackException ignore) {
                }

                return result;
            } catch (Exception e) {
                if (transaction.isNewTransaction()) {
                    transactionManager.rollback(transaction);
                }
                throw e;
            }
        } else {
            // not supported on static methods
            return invocation.proceed();
        }
    }
}

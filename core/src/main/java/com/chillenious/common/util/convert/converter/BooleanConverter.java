/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.chillenious.common.util.convert.converter;

import com.chillenious.common.util.StringValueConversionException;
import com.chillenious.common.util.Strings;
import com.chillenious.common.util.convert.IConverter;

import java.util.Locale;

/**
 * Converts from Object to Boolean.
 *
 * @author Eelco Hillenius
 * @author Jonathan Locke
 */
public class BooleanConverter extends AbstractConverter<Boolean> {
    private static final long serialVersionUID = 1L;

    /**
     * The singleton instance for a boolean converter
     */
    public static final IConverter<Boolean> INSTANCE = new BooleanConverter();

    /**
     * @see com.chillenious.common.util.convert.IConverter#convertToObject(String, java.util.Locale)
     */
    @Override
    public Boolean convertToObject(final String value, final Locale locale) {
        try {
            return Strings.toBoolean(value);
        } catch (StringValueConversionException e) {
            throw newConversionException("Cannot convert '" + value + "' to Boolean", value, locale);
        }
    }

    /**
     * @see AbstractConverter#getTargetType()
     */
    @Override
    protected Class<Boolean> getTargetType() {
        return Boolean.class;
    }
}
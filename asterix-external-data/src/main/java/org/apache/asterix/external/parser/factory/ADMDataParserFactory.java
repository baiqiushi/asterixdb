/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.asterix.external.parser.factory;

import org.apache.asterix.external.api.IExternalDataSourceFactory.DataSourceType;
import org.apache.asterix.external.api.IRecordDataParser;
import org.apache.asterix.external.api.IStreamDataParser;
import org.apache.asterix.external.parser.ADMDataParser;
import org.apache.asterix.external.util.ExternalDataUtils;
import org.apache.asterix.om.types.ARecordType;
import org.apache.hyracks.api.context.IHyracksTaskContext;
import org.apache.hyracks.api.exceptions.HyracksDataException;

public class ADMDataParserFactory extends AbstractRecordStreamParserFactory<char[]> {

    private static final long serialVersionUID = 1L;

    @Override
    public IRecordDataParser<char[]> createRecordParser(IHyracksTaskContext ctx) throws HyracksDataException {
        return createParser();
    }

    private ADMDataParser createParser() throws HyracksDataException {
        try {
            ADMDataParser parser = new ADMDataParser(recordType,
                    ExternalDataUtils.getDataSourceType(configuration).equals(DataSourceType.STREAM));
            return parser;
        } catch (Exception e) {
            throw new HyracksDataException(e);
        }
    }

    @Override
    public Class<? extends char[]> getRecordClass() {
        return char[].class;
    }

    @Override
    public IStreamDataParser createInputStreamParser(IHyracksTaskContext ctx, int partition)
            throws HyracksDataException {
        return createParser();
    }

    @Override
    public void setMetaType(ARecordType metaType) {
    }

}

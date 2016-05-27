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
package org.apache.asterix.runtime.evaluators.functions;

import java.io.DataOutput;
import java.io.IOException;

import org.apache.asterix.common.exceptions.AsterixException;
import org.apache.asterix.dataflow.data.nontagged.serde.AOrderedListSerializerDeserializer;
import org.apache.asterix.dataflow.data.nontagged.serde.AUnorderedListSerializerDeserializer;
import org.apache.asterix.om.functions.AsterixBuiltinFunctions;
import org.apache.asterix.om.functions.IFunctionDescriptor;
import org.apache.asterix.om.functions.IFunctionDescriptorFactory;
import org.apache.asterix.om.types.ATypeTag;
import org.apache.asterix.om.types.EnumDeserializer;
import org.apache.asterix.om.util.NonTaggedFormatUtil;
import org.apache.asterix.runtime.evaluators.base.AbstractScalarFunctionDynamicDescriptor;
import org.apache.hyracks.algebricks.common.exceptions.AlgebricksException;
import org.apache.hyracks.algebricks.core.algebra.functions.FunctionIdentifier;
import org.apache.hyracks.algebricks.runtime.base.IScalarEvaluator;
import org.apache.hyracks.algebricks.runtime.base.IScalarEvaluatorFactory;
import org.apache.hyracks.api.context.IHyracksTaskContext;
import org.apache.hyracks.data.std.api.IPointable;
import org.apache.hyracks.data.std.primitive.VoidPointable;
import org.apache.hyracks.data.std.util.ArrayBackedValueStorage;
import org.apache.hyracks.dataflow.common.data.accessors.IFrameTupleReference;

public class AnyCollectionMemberDescriptor extends AbstractScalarFunctionDynamicDescriptor {

    private static final long serialVersionUID = 1L;
    public static final IFunctionDescriptorFactory FACTORY = new IFunctionDescriptorFactory() {
        @Override
        public IFunctionDescriptor createFunctionDescriptor() {
            return new AnyCollectionMemberDescriptor();
        }
    };

    @Override
    public IScalarEvaluatorFactory createEvaluatorFactory(final IScalarEvaluatorFactory[] args) {
        return new AnyCollectionMemberEvalFactory(args[0]);
    }

    @Override
    public FunctionIdentifier getIdentifier() {
        return AsterixBuiltinFunctions.ANY_COLLECTION_MEMBER;
    }

    private static class AnyCollectionMemberEvalFactory implements IScalarEvaluatorFactory {

        private static final long serialVersionUID = 1L;

        private IScalarEvaluatorFactory listEvalFactory;
        private byte serItemTypeTag;
        private ATypeTag itemTag;
        private boolean selfDescList = false;

        public AnyCollectionMemberEvalFactory(IScalarEvaluatorFactory arg) {
            this.listEvalFactory = arg;
        }

        @Override
        public IScalarEvaluator createScalarEvaluator(IHyracksTaskContext ctx) throws AlgebricksException {
            return new IScalarEvaluator() {

                private ArrayBackedValueStorage resultStorage = new ArrayBackedValueStorage();
                private DataOutput out = resultStorage.getDataOutput();
                private IPointable inputArgList = new VoidPointable();
                private IScalarEvaluator evalList = listEvalFactory.createScalarEvaluator(ctx);
                private int itemOffset;
                private int itemLength;

                @Override
                public void evaluate(IFrameTupleReference tuple, IPointable result) throws AlgebricksException {

                    try {
                        resultStorage.reset();
                        evalList.evaluate(tuple, inputArgList);
                        byte[] serList = inputArgList.getByteArray();
                        int offset = inputArgList.getStartOffset();

                        if (serList[offset] != ATypeTag.SERIALIZED_ORDEREDLIST_TYPE_TAG
                                && serList[offset] != ATypeTag.SERIALIZED_UNORDEREDLIST_TYPE_TAG) {
                            throw new AlgebricksException(AsterixBuiltinFunctions.ANY_COLLECTION_MEMBER.getName()
                                    + ": expects input type ORDEREDLIST/UNORDEREDLIST, but got "
                                    + EnumDeserializer.ATYPETAGDESERIALIZER.deserialize(serList[offset]));
                        }

                        if (serList[offset] == ATypeTag.SERIALIZED_ORDEREDLIST_TYPE_TAG) {
                            if (AOrderedListSerializerDeserializer.getNumberOfItems(serList, offset) == 0) {
                                out.writeByte(ATypeTag.SERIALIZED_MISSING_TYPE_TAG);
                                result.set(resultStorage);
                                return;
                            }
                            itemOffset = AOrderedListSerializerDeserializer.getItemOffset(serList, offset, 0);
                        } else {
                            if (AUnorderedListSerializerDeserializer.getNumberOfItems(serList, offset) == 0) {
                                out.writeByte(ATypeTag.SERIALIZED_MISSING_TYPE_TAG);
                                result.set(resultStorage);
                                return;
                            }
                            itemOffset = AUnorderedListSerializerDeserializer.getItemOffset(serList, offset, 0);
                        }

                        itemTag = EnumDeserializer.ATYPETAGDESERIALIZER.deserialize(serList[offset + 1]);
                        if (itemTag == ATypeTag.ANY) {
                            selfDescList = true;
                        } else {
                            serItemTypeTag = serList[offset + 1];
                        }

                        if (selfDescList) {
                            itemTag = EnumDeserializer.ATYPETAGDESERIALIZER.deserialize(serList[itemOffset]);
                            itemLength = NonTaggedFormatUtil.getFieldValueLength(serList, itemOffset, itemTag, true)
                                    + 1;
                            result.set(serList, itemOffset, itemLength);
                        } else {
                            itemLength = NonTaggedFormatUtil.getFieldValueLength(serList, itemOffset, itemTag, false);
                            out.writeByte(serItemTypeTag);
                            out.write(serList, itemOffset, itemLength);
                            result.set(resultStorage);
                        }
                    } catch (IOException e) {
                        throw new AlgebricksException(e);
                    } catch (AsterixException e) {
                        throw new AlgebricksException(e);
                    }
                }
            };
        }

    }

}
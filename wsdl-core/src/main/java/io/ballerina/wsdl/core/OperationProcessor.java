/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.wsdl.core;

import io.ballerina.wsdl.core.recordgenerator.ballerinair.BasicField;
import io.ballerina.wsdl.core.recordgenerator.ballerinair.ComplexField;
import io.ballerina.wsdl.core.recordgenerator.ballerinair.Field;
import io.ballerina.wsdl.core.wsdlmodel.WSDLHeader;
import io.ballerina.wsdl.core.wsdlmodel.WSDLOperation;
import io.ballerina.wsdl.core.wsdlmodel.WSDLPart;

import java.util.ArrayList;
import java.util.List;

public class OperationProcessor {
    private final WSDLOperation wsdlOperation;

    OperationProcessor(WSDLOperation wsdlOperation) {
        this.wsdlOperation = wsdlOperation;
    }

    List<Field> generateFields() {
        List<Field> fields = new ArrayList<>();
        PartProcessor partProcessor = new PartProcessor();
        // Input
        List<WSDLPart> inputParts = new ArrayList<>();
        List<WSDLPart> inputHeaderParts =
                wsdlOperation.getOperationInput().getHeaders().stream().map(WSDLHeader::getPart).toList();
        List<WSDLPart> inputBodyParts = wsdlOperation.getOperationInput().getMessage().getParts();
        inputParts.addAll(inputHeaderParts);
        inputParts.addAll(inputBodyParts);
        for (WSDLPart inputPart : inputParts) {
            List<Field> inputFields = partProcessor.generateFields(inputPart);
            fields.addAll(inputFields);
        }

        // Output
        List<WSDLPart> outputParts = new ArrayList<>();
        List<WSDLPart> outputHeaderParts =
                wsdlOperation.getOperationInput().getHeaders().stream().map(WSDLHeader::getPart).toList();
        List<WSDLPart> outputBodyParts = wsdlOperation.getOperationOutput().getMessage().getParts();
        outputParts.addAll(outputHeaderParts);
        outputParts.addAll(outputBodyParts);
        for (WSDLPart outputPart : outputParts) {
            List<Field> outputFields = partProcessor.generateFields(outputPart);
            fields.addAll(outputFields);
        }
        fields.add(processOperationInput(wsdlOperation, fields));
        return fields;
    }

    private ComplexField processOperationInput(WSDLOperation operation, List<Field> processedFields) {
        ComplexField envelopField =
                new ComplexField("Envelope", operation.getOperationName() + "Envelope");
        envelopField.setAttributeName("Envelope");
        ComplexField headerField = new ComplexField("Header", operation.getOperationName()+"Header");
        ComplexField bodyField =
                new ComplexField("Body", operation.getOperationName() + "Body");

        List<WSDLPart> inputHeaderParts =
                wsdlOperation.getOperationInput().getHeaders().stream().map(WSDLHeader::getPart).toList();
        for (WSDLPart inputPart : inputHeaderParts) {
            List<Field> filteredFields = processedFields.stream()
                    .filter(field -> inputPart.getElementName().equals(field.getName()))
                    .toList();
            if (!filteredFields.isEmpty()) {
                Field headerMemberField = filteredFields.get(0);
                if (headerMemberField instanceof ComplexField complexHeaderMemberField) {
                    complexHeaderMemberField.setFields(new ArrayList<>());
                    complexHeaderMemberField.setCyclicDep(true);
                    headerField.addField(complexHeaderMemberField);
                } else if (headerMemberField instanceof BasicField basicHeaderMemberField) {
                    headerField.addField(basicHeaderMemberField);
                }
            }
        }

        // TODO: For body also have to check for both Complex and Simple Fields.
        List<WSDLPart> inputBodyParts = wsdlOperation.getOperationInput().getMessage().getParts();
        for (WSDLPart inputPart : inputBodyParts) {
            ComplexField bodyPart = new ComplexField(inputPart.getElementName(), inputPart.getElementName());
            bodyPart.setCyclicDep(true);
            bodyField.addField(bodyPart);
        }

        envelopField.addField(headerField);
        envelopField.addField(bodyField);
        return envelopField;
    }
}

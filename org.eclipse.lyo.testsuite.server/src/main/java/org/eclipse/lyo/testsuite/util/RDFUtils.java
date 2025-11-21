/*
 * Copyright (c) 2011, 2025 IBM Corporation and others
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Eclipse Distribution License 1.0
 * which is available at http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */
package org.eclipse.lyo.testsuite.util;

import java.util.Iterator;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.ValidityReport;
import org.junit.jupiter.api.Assertions;

public class RDFUtils {

    public static void printModel(Model rdfModel) {
        StmtIterator listProperties = rdfModel.listStatements();
        System.out.println("Dumping triples....");
        while (listProperties.hasNext()) {
            System.out.println(listProperties.nextStatement().toString());
        }
    }

    public static void validateModel(Model model) {

        InfModel infmodel = ModelFactory.createRDFSModel(model);
        ValidityReport validityReport = infmodel.validate();

        if (!validityReport.isClean()) {

            StringBuffer errorMessage = new StringBuffer();
            errorMessage.append("Invalid model:"); // $NON-NLS-1$

            Iterator<ValidityReport.Report> reports = validityReport.getReports();

            while (reports.hasNext()) {

                errorMessage.append('\n');
                errorMessage.append(reports.next().toString());
            }

            Assertions.fail(errorMessage.toString());
        }
    }
}

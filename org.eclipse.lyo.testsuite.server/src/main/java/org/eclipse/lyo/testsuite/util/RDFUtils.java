/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *
 *    Steve Speicher - initial API and implementation
 *******************************************************************************/
package org.eclipse.lyo.testsuite.util;

import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.ValidityReport;
import java.util.Iterator;
import org.junit.Assert;

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

            Assert.fail(errorMessage.toString());
        }
    }
}

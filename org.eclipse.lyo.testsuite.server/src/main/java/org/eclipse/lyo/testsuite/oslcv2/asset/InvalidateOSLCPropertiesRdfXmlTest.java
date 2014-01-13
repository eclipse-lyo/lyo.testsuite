package org.eclipse.lyo.testsuite.oslcv2.asset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.eclipse.lyo.testsuite.server.util.OSLCConstants;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

@RunWith(Parameterized.class)
public class InvalidateOSLCPropertiesRdfXmlTest extends InvalidateOSLCPropertiesTestBase {
	
	private String baseUrl;

	public InvalidateOSLCPropertiesRdfXmlTest(String url) throws IOException {
		super(url, OSLCConstants.CT_RDF, OSLCConstants.CT_RDF);
		
		assetUrl = createAsset(rdfXmlCreateTemplate);
		assertTrue("The location of the asset after it was create was not returned", assetUrl != null);
		baseUrl = setupProps.getProperty("baseUrl");
	}

	@Override
	protected void queryInvalidOSLCProperties(String properties) throws IOException {
		HttpResponse resp = getAssetResponse();
		
		Model model = ModelFactory.createDefaultModel();
		model.read(resp.getEntity().getContent(), baseUrl);
		EntityUtils.consume(resp.getEntity());
		
		// Updates the title
		String name = "updated asset";
		setPropertyValue(model, OSLCConstants.DC_TITLE_PROP, name);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		model.write(output);
		String content = output.toString();
		
		resp = putAssetProperties(properties, content);
		
		assertEquals(HttpStatus.SC_CONFLICT, resp.getStatusLine().getStatusCode());
	}

	private void setPropertyValue(Model model, String uri, String newValue) {
		Property property = model.getProperty(uri);
		Selector select = new SimpleSelector(null, property, (RDFNode)null);
		StmtIterator statements = model.listStatements(select);
		ArrayList<Statement> statementList = new ArrayList<Statement>();
		// Converts the iterator into an array list so that the statement(s) can be modified
		while(statements.hasNext()) {
			statementList.add(statements.nextStatement());
		}
		
		for(int i = 0; i < statementList.size(); i++) {
			Statement statement = statementList.get(i);
			statement.changeObject(newValue);
		}
	}
}

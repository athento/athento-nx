package org.athento.nuxeo.query;

import junit.framework.TestCase;
import org.junit.Test;

import java.util.List;

/**
 * Test for Query.
 */
public class TestQuery extends TestCase {

    @Test
    public void testMultipleExecution() {

        String query = "SELECT * FROM Document WHERE ecm:mixinType != 'HiddenInNavigation' AND ecm:isProxy = 0 AND ecm:isCheckedInVersion = 0 AND ecm:currentLifeCycleState != 'deleted' AND dc:title = 'Test'" +
                "|SELECT * FROM Document WHERE ecm:mixinType != 'HiddenInNavigation' AND ecm:isProxy = 0 AND ecm:isCheckedInVersion = 0 AND ecm:currentLifeCycleState != 'deleted' AND dc:creator = '${dc:creator}'";

        List<String> queries = QueryUtils.extractQueriesFromQuery(query, "\\|");
        assertEquals(queries.size(), 2);

    }
}

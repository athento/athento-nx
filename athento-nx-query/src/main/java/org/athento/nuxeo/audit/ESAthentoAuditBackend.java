package org.athento.nuxeo.audit;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.max.Max;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.nuxeo.common.utils.TextTemplate;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.uidgen.UIDGeneratorService;
import org.nuxeo.ecm.core.uidgen.UIDSequencer;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.Work.State;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.FilterMapEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.query.AuditQueryException;
import org.nuxeo.ecm.platform.audit.api.query.DateRangeParser;
import org.nuxeo.ecm.platform.audit.service.AbstractAuditBackend;
import org.nuxeo.ecm.platform.audit.service.AuditBackend;
import org.nuxeo.ecm.platform.audit.service.BaseLogEntryProvider;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.audit.service.extension.AuditBackendDescriptor;
import org.nuxeo.ecm.platform.query.api.PredicateDefinition;
import org.nuxeo.ecm.platform.query.api.PredicateFieldDefinition;
import org.nuxeo.elasticsearch.ElasticSearchConstants;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.audit.ESAuditBackend;
import org.nuxeo.elasticsearch.audit.ESAuditMigrationWork;
import org.nuxeo.elasticsearch.audit.ESExtendedInfo;
import org.nuxeo.elasticsearch.audit.io.AuditEntryJSONReader;
import org.nuxeo.elasticsearch.audit.io.AuditEntryJSONWriter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.*;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Implementation of the {@link AuditBackend} interface using Elasticsearch persistence for Athento.
 *
 * @author tiry
 */
public class ESAthentoAuditBackend extends ESAuditBackend {

    protected static final Log LOG = LogFactory.getLog(ESAthentoAuditBackend.class);

    public ESAthentoAuditBackend(NXAuditEventsService component, AuditBackendDescriptor config) {
        super(component, config);
    }

    public List<LogEntry> getLogEntries(Map<String, FilterMapEntry> filterMap, boolean doDefaultSort) {
        QueryBuilder filter = QueryBuilders.boolQuery();
        if (!MapUtils.isEmpty(filterMap)) {
            for (String key : filterMap.keySet()) {
                FilterMapEntry entry = filterMap.get(key);
                Object value = entry.getObject();
                if (value instanceof String && ((String) value).contains("|")) {
                    String v0 = ((String) value).split("\\|")[0];
                    String v1 = ((String) value).split("\\|")[1];
                    RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(entry.getColumnName());
                    rangeQueryBuilder.gte(v0).lt(v1);
                    ((BoolQueryBuilder) filter).must(rangeQueryBuilder);
                } else {
                    ((BoolQueryBuilder) filter).must(QueryBuilders.termQuery(entry.getColumnName(), entry.getObject()));
                }
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Audit filter=" + filter);
        }
        return super.getLogEntries(filter, doDefaultSort);
    }

    @Override
    public List<LogEntry> getLogEntriesFor(String uuid, Map<String, FilterMapEntry> filterMap, boolean doDefaultSort) {
        TermQueryBuilder docFilter = QueryBuilders.termQuery("docUUID", uuid);
        QueryBuilder filter;
        if (MapUtils.isEmpty(filterMap)) {
            filter = docFilter;
        } else {
            filter = QueryBuilders.boolQuery().must(docFilter);
            for (String key : filterMap.keySet()) {
                FilterMapEntry entry = filterMap.get(key);
                if ("in".equalsIgnoreCase(entry.getOperator())) {
                    ((BoolQueryBuilder) filter).must(QueryBuilders.termsQuery(entry.getColumnName(), entry.getObject()));
                } else {
                    ((BoolQueryBuilder) filter).must(QueryBuilders.termQuery(entry.getColumnName(), entry.getObject()));
                }
            }
        }
        return getLogEntries(filter, doDefaultSort);
    }

}

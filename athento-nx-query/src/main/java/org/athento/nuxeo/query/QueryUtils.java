package org.athento.nuxeo.query;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.provider.ElasticSearchQueryProviderDescriptor;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.tag.Tag;
import org.nuxeo.ecm.platform.tag.TagService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.api.Framework;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Query utils.
 */
public class QueryUtils {

    /** Log. */
    private static final Log LOG = LogFactory.getLog(QueryUtils.class);


    enum CastTypes {
        date, shortdate, vocabulary, document, user, parent, complex, list;
    }

    /** Query separator. */
    public static final String QUERY_SEPARATOR = "|";

    /** Date formats. */
    private static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat SHORT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Casting a metadata for a document.
     *
     * @param metadata to casting
     * @param doc to get information about casting
     * @return
     */
    public static CastField [] cast(String metadata, DocumentModel doc) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Casting " + metadata );
        }
        List<CastField> castValues = new ArrayList<>();
        if (hasCasting(metadata)) {
            // Add origin value for metadata
            String field = getSegment(metadata, 0);
            // Get original value from document given the field name
            CastField originalField = getOriginalField(field, doc);
            castValues.add(originalField);
            // Start casting
            String castType = getSegment(metadata, 1);
            if (castType.startsWith(CastTypes.date.name())) {
                int hours = getTimeChange(castType);
                GregorianCalendar date = (GregorianCalendar) doc.getPropertyValue(field);
                if (date != null) {
                    Calendar castedDated = GregorianCalendar.getInstance();
                    castedDated.setTime(date.getTime());
                    castedDated.add(GregorianCalendar.HOUR, hours);
                    castValues.add(new CastField(field, formatDate(castedDated.getTime(), DEFAULT_DATE_FORMAT)));
                }
            } else if (castType.startsWith(CastTypes.shortdate.name())) {
                int hours = getTimeChange(castType);
                GregorianCalendar date = (GregorianCalendar) doc.getPropertyValue(field);
                if (date != null) {
                    Calendar castedDated = GregorianCalendar.getInstance();
                    castedDated.setTime(date.getTime());
                    castedDated.add(GregorianCalendar.HOUR, hours);
                    castValues.add(new CastField(field, formatDate(castedDated.getTime(), SHORT_DATE_FORMAT)));
                }
            } else if (castType.equals(CastTypes.vocabulary.name())) {
                try {
                    String vocabularyName = getSegment(metadata, 2);
                    DocumentModel entry = getVocabularyEntry(vocabularyName, (String) doc.getPropertyValue(field));
                    if (entry != null) {
                        if (entry.hasSchema("xvocabulary")) {
                            castValues.add(new CastField(field, entry.getPropertyValue("xvocabulary:label")));
                        } else {
                            castValues.add(new CastField(field, entry.getPropertyValue("vocabulary:label")));
                        }
                    }
                } catch (IndexOutOfBoundsException e) {
                    LOG.warn("Vocabulary casting is not well formatted " + e.getMessage());
                } catch (DirectoryException e) {
                    LOG.warn("Vocabulary is not found in field casting. Use field::vocabulary::<vocabulary-name>: " + e.getMessage());
                }
            } else if (castType.equals(CastTypes.document.name())) {
                try {
                    // Check original field document id
                    if (originalField.getValue() != null) {
                        String docField;
                        if (countSegments(metadata) < 3) {
                            docField = "dc:title"; // Default doc field to extract
                        } else {
                            // Use to include more fields for document casting
                            docField = getRightSegment(metadata, 2);
                        }
                        CoreSession session = doc.getCoreSession();
                        DocumentModel fieldDoc = session
                                .getDocument(new IdRef((String) originalField.getValue()));
                        try {
                            if (hasCasting(docField)) {
                                CastField[] fieldCastFields = cast(docField, fieldDoc);
                                for (CastField fieldCastField : fieldCastFields) {
                                    fieldCastField.setField(field + ">>" + fieldCastField.getField());
                                    castValues.add(fieldCastField);
                                }
                            } else {
                                castValues.add(new CastField(field + ">>" + docField, getMetadataValue(docField, fieldDoc)));
                            }
                        } catch (PropertyException e) {
                            LOG.warn("Doc field " + docField + " is not found in the document: " + e.getMessage());
                        }
                    }
                } catch (DocumentNotFoundException e) {
                    LOG.error("Unable to cast document for " + metadata + ": " + e.getMessage());
                }
            } else if (castType.equals(CastTypes.user.name())) {
                try {
                    // Check original field document id
                    if (originalField.getValue() != null) {
                        String userField;
                        if (countSegments(metadata) < 3) {
                            userField = "username";
                        } else {
                            // Use to include more fields for user casting
                            userField = getRightSegment(metadata, 2);
                        }
                        UserManager userManager = Framework.getService(UserManager.class);
                        DocumentModel userModel = userManager.getUserModel((String) originalField.getValue());
                        Serializable metadataValue = "";
                        if ("fullName".equals(userField)) {
                            String firstName = (String) getMetadataValue("user:firstName", userModel);
                            String lastName = (String) getMetadataValue("user:lastName", userModel);
                            metadataValue = firstName + " " + lastName;
                        } else {
                            metadataValue = getMetadataValue("user:" + userField, userModel);
                        }
                        castValues.add(new CastField(field + ">>" + userField, metadataValue));
                    }
                } catch (DocumentNotFoundException e) {
                    LOG.error("Unable to cast user for " + metadata + ": " + e.getMessage());
                }
            } else if (castType.equals(CastTypes.complex.name())) {
                Serializable complexValue = manageComplexField(doc, field);
                castValues.add(new CastField(field, complexValue));
            } else if (castType.equals(CastTypes.list.name())) {
                Serializable listValue = manageFieldList(doc, field);
                castValues.add(new CastField(field, listValue));
            }
        }
        return castValues.toArray(new CastField[0]);
    }

    /**
     * Get time change from casting type in dates.
     *
     * @param castType
     * @return
     */
    private static int getTimeChange(String castType) {
        int hours = 0;
        if (castType.contains("-")) {
            String [] values = castType.split("-", 2);
            if (values.length == 2) {
                hours = Integer.valueOf(values[1]);
            } else {
                LOG.warn("Time change for castType: " + castType + " is malformed");
            }
        } else if (castType.contains("-")) {
            String [] values = castType.split("--", 2);
            if (values.length == 2) {
                hours = Integer.valueOf(values[1]);
            } else {
                LOG.warn("Time change for castType: " + castType + " is malformed");
            }
        }
        return hours;
    }

    /**
     * Get metadata value from document.
     *
     * @param field
     * @param doc
     * @return
     */
    private static Serializable getMetadataValue(String field, DocumentModel doc) {
        if (!field.startsWith("ecm:")) {
            return doc.getPropertyValue(field);
        } else {
            if ("ecm:uuid".equals(field)) {
                return doc.getId();
            } else if ("ecm:currentLifeCycleState".equals(field)) {
                return doc.getCurrentLifeCycleState();
            } else if ("ecm:path".equals(field)) {
                return doc.getPathAsString();
            } else if ("ecm:primaryType".equals(field)) {
                return doc.getType();
            } else if ("ecm:isProxy".equals(field)) {
                return doc.isProxy();
            } else if ("ecm:isVersion".equals(field)) {
                return doc.isVersion();
            } else if ("ecm:isCheckedInVersion".equals(field)) {
                return doc.isCheckedOut();
            } else {
                return null;
            }
        }
    }

    /**
     * Get original field value from document.
     *
     * @param field
     * @param doc
     * @return
     */
    private static CastField getOriginalField(String field, DocumentModel doc) {
        CastField cf = new CastField(field, null);
        if ("ecm:parentId".equals(field)) {
            cf.setValue((String) doc.getParentRef().reference());
        } else {
            cf.setValue(doc.getPropertyValue(field));
        }
        cf.setOriginal(true);
        return cf;
    }

    /**
     * Count segments.
     *
     * @param metadata
     * @return
     */
    private static int countSegments(String metadata) {
        return metadata.split("::").length;
    }

    /**
     * Get segment of metadata field.
     *
     * @param metadata
     * @param i
     * @return
     */
    private static String getSegment(String metadata, int i) {
        String [] info = metadata.split("::");
        if (info.length > i) {
            return info[i];
        }
        return null;
    }

    /**
     * Get right segments of metadata field.
     *
     * @param metadata
     * @param pos
     * @return
     */
    private static String getRightSegment(String metadata, int pos) {
        String [] info = metadata.split("::");
        if (info.length > pos) {
            String result = "";
            for (int i = pos;i < info.length; i++) {
                result += info[i];
                if (i < info.length - 1) {
                    result += "::";
                }
            }
            return result;
        }
        return null;
    }

    /**
     * Format a date.
     *
     * @param date
     * @param format
     * @return
     */
    private static Serializable formatDate(Date date, SimpleDateFormat format) {
        return format.format(date);
    }

    /**
     * Check if metadata has a casting.
     *
     * @param metadata
     * @return
     */
    public static boolean hasCasting(String metadata) {
        return metadata.contains("::");
    }

    /**
     * Get vocabulary entry.
     *
     * @param vocabularyName
     * @param entryId
     * @return
     */
    public static DocumentModel getVocabularyEntry(String vocabularyName, String entryId) {
        Session directorySession = null;
        try {
            directorySession = getDirectoryService().open(vocabularyName);
            return directorySession.getEntry(entryId);
        } finally {
            if (directorySession != null) {
                directorySession.close();
            }
        }
    }

    /**
     * Get directory service.
     *
     * @return
     */
    private static DirectoryService getDirectoryService() {
        return Framework.getService(DirectoryService.class);
    }

    /**
     * Extract queries.
     *
     * @param query
     * @param separator
     * @return
     */
    public static ArrayList<String> extractQueriesFromQuery(String query, String separator, boolean removeAccents) {
        ArrayList<String> subqueries = new ArrayList(Arrays.asList(query.split(separator)));
        if (!removeAccents) {
            return subqueries;
        }
        ArrayList<String> normalizedQueries = new ArrayList<>();
        for (String subquery : subqueries) {
            normalizedQueries.add(stripAccents(subquery));
        }
        return normalizedQueries;
    }

    /**
     * Normalize query removing accents.
     *
     * @param query
     * @return
     */
    public static String stripAccents(String query) {
        if (!query.toUpperCase().contains("WHERE")) {
            return query;
        }
        // Extract WHERE clauses
        String whereClause = query.split("WHERE|where")[1].trim();
        if (!whereClause.contains("ecm:tag")) {
            return StringUtils.stripAccents(query);
        }
        // Get ecm:tag predicate
        String regex = "\\s(ecm:tag)\\s*(LIKE|ILIKE|IN|in|like|ilike|=)\\s*([^\\s]*)";
        Pattern ecmTagPattern = Pattern.compile(regex);
        Matcher matcher = ecmTagPattern.matcher(query);
        if (matcher.find()) {
            String operator = matcher.group(2);
            String roperand = matcher.group(3);
            String ecmTagFinal = " ecm:tag " + operator + " " + roperand;
            query = StringUtils.stripAccents(query);
            query = query.replaceAll(regex, ecmTagFinal);
        }
        return query;
    }

    /**
     * Execute recursive query.
     *
     * @param session
     * @param queryCtxt
     * @throws IOException
     */
    public static void executeRecursiveQuery(CoreSession session, QueryContext queryCtxt) throws IOException {
        executeRecursiveQuery(session, queryCtxt, true);
    }

    /**
     * Execute recursive queries.
     *
     * @param session
     * @param queryCtxt
     * @param executeLast
     */
    public static void executeRecursiveQuery(CoreSession session, QueryContext queryCtxt, boolean executeLast) throws IOException {
        if (queryCtxt.isLastQuery()) {
            if (executeLast) {
                String query = queryCtxt.getQuery();
                DocumentModelList result = executeQuery(session, query, queryCtxt.getOffset(), queryCtxt.getLimit(), queryCtxt.getSortInfo());
                ((List) queryCtxt.getResult()).addAll(result);
            }
        } else {
            String query = queryCtxt.getQuery();
            DocumentModelList result = executeQuery(session, query, 0, -1, new ArrayList<SortInfo>(0));
            queryCtxt.put("uuids", QueryUtils.getUUIDsClause(result));
            executeRecursiveQuery(session, queryCtxt);
        }
    }

    /**
     * Execute query.
     *
     * @param session
     * @param query
     * @param offset
     * @param limit
     * @param sortInfo
     * @return
     */
    public static DocumentModelList executeQuery(CoreSession session, String query, int offset, int limit, List<SortInfo> sortInfo) {
        DocumentModelList result;
        // Build and execute the ES query
        ElasticSearchService ess = Framework.getLocalService(ElasticSearchService.class);
        NxQueryBuilder nxQuery = new NxQueryBuilder(session).nxql(query)
                .addSort(sortInfo.toArray(new SortInfo[0])).limit(limit).offset(offset);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Execute query: " + query);
        }
        result = ess.query(nxQuery);
        return result;
    }

    /**
     * Execute recursive resultset.
     *
     * @param ppService
     * @param queryCtxt
     * @param properties
     * @param params
     * @param executeLast
     */
    public static void executeRecursiveResultset(PageProviderService ppService, QueryContext queryCtxt, Map<String, Serializable> properties, Object[] params, boolean executeLast) throws IOException {
        if (queryCtxt.isLastQuery()) {
            if (executeLast) {
                String query = queryCtxt.getQuery();
                List<Map<String, Serializable>> result = executeResultSet(ppService, query, queryCtxt.getOffset(), queryCtxt.getLimit(), queryCtxt.getSortInfo(), properties, params);
                ((List) queryCtxt.getResult()).addAll(result);
            }
        } else {
            String query = queryCtxt.getQuery();
            List<Map<String, Serializable>> result = executeResultSet(ppService, query, 0, -1, new ArrayList<SortInfo>(0), properties, params);
            Map<String, List<Serializable>> clauses = getMetadataClauses(result);
            for (Map.Entry<String, List<Serializable>> entry : clauses.entrySet()) {
                String metadata = entry.getKey();
                String clause = getMetadataClause(entry.getValue());
                queryCtxt.put(metadata, clause);
            }
            executeRecursiveResultset(ppService, queryCtxt, properties, params, executeLast);
        }
    }

    /**
     * Execute resultset.
     *
     * @param ppService
     * @param query
     * @param page
     * @param pageSize
     * @param sortInfos
     * @param properties
     * @param params
     * @return
     */
    public static List<Map<String, Serializable>> executeResultSet(PageProviderService ppService, String query, long page, long pageSize, List<SortInfo> sortInfos, Map<String, Serializable> properties, Object[] params) {
        ElasticSearchQueryProviderDescriptor desc = new ElasticSearchQueryProviderDescriptor();
        desc.setPattern(query);
        PageProvider<Map<String, Serializable>> pp = (ElasticSearchQueryAndFetchPageProvider) ppService.getPageProvider("", desc, null, sortInfos,
                pageSize, page, properties, params);
        return pp.getCurrentPage();
    }

    /**
     * Expand params in query.
     *
     * @param query
     * @param params
     * @return
     */
    public static String expandParams(String query, Map<String, Object> params) throws IOException {
        StringBuffer queryWork = new StringBuffer(query);
        String pattern = "\\$\\{(.*?)\\}";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(query);
        while(m.find()) {
            String param = m.group(1);
            int start = m.start();
            int end = m.end();
            if (params.containsKey(param)) {
                queryWork = queryWork.replace(start, end, params.get(param).toString());
            } else {
                LOG.warn("Param " + param + " is not found in parameters");
                queryWork = queryWork.replace(start, end, "''");
            }
            m = p.matcher(queryWork.toString());
        }
        return queryWork.toString();
    }

    /**
     * Get UUIDs clause from document list.
     *
     * @param list
     * @return
     */
    public static String getUUIDsClause(DocumentModelList list) {
        String uuids  = "'";
        for (ListIterator<DocumentModel> it = list.listIterator(); it.hasNext();) {
            uuids += it.next().getId();
            if (it.hasNext()) {
                uuids += "', '";
            }
        }
        return uuids + "'";
    }

    /**
     * Get metadata clause from list.
     *
     * @param list
     * @return
     */
    public static String getMetadataClause(List<Serializable> list) {
        String clause  = "'";
        for (ListIterator<Serializable> it = list.listIterator(); it.hasNext();) {
            clause += it.next().toString();
            if (it.hasNext()) {
                clause += "', '";
            }
        }
        return clause + "'";
    }

    /**
     * Get metadata clauses for resultset list.
     *
     * @param list
     * @return
     */
    public static Map<String, List<Serializable>> getMetadataClauses(List<Map<String, Serializable>> list) {
        Map<String, List<Serializable>> clauses = new HashMap<>();
        for (ListIterator<Map<String, Serializable>> it = list.listIterator(); it.hasNext();) {
            Map<String, Serializable> record = it.next();
            for (Iterator<Map.Entry<String, Serializable>> itEntry = record.entrySet().iterator(); itEntry.hasNext();) {
                Map.Entry<String, Serializable> entry = itEntry.next();
                if (clauses.get(entry.getKey()) == null) {
                    clauses.put(entry.getKey(), new ArrayList<Serializable>());
                }
                clauses.get(entry.getKey()).add(entry.getValue());
            }
        }
        return clauses;
    }

    /**
     * Manage complex field.
     *
     * @param doc
     * @param field
     */
    public static Serializable manageComplexField(DocumentModel doc, String field) {
        String uuid = doc.getId();
        if (uuid != null) {
            try {
                if (field != null) {
                    field = field.trim();
                    if (!field.isEmpty()) {
                        try {
                            String aux = field;
                            if (aux.contains("/")) {
                                aux = field.split("/")[0];
                            }
                            Property prop = doc.getProperty(aux);
                            if (prop.isComplex()) {
                                return doc.getPropertyValue(field);
                            } else {
                                LOG.warn("Field " + field + " is not complex");
                            }
                        } catch (PropertyNotFoundException e) {
                            LOG.trace("Ignore document property " + field + " for " + doc.getId());
                        }
                    }
                }
            } catch (NuxeoException e) {
                LOG.trace("Document is not found into ResultSet " + uuid);
            }
        }
        return null;
    }

    /**
     * Manage field list.
     *
     * @param doc
     * @param field
     */
    public static Serializable manageFieldList(DocumentModel doc, String field) {
        String uuid = doc.getId();
        if (uuid != null) {
            try {
                if (field != null) {
                    field = field.trim();
                    if (!field.isEmpty()) {
                        if ("ecm:tag".equals(field)) {
                            TagService tagService = Framework.getService(TagService.class);
                            List<Tag> tags = tagService.getDocumentTags(doc.getCoreSession(), doc.getId(), null);
                            StringBuilder sb = new StringBuilder();
                            for (Iterator<Tag> it = tags.iterator(); it.hasNext(); ) {
                                Tag tag = it.next();
                                sb.append(tag.getLabel());
                                if (it.hasNext()) {
                                    sb.append(", ");
                                }
                            }
                            return sb.toString();
                        } else {
                            try {
                                Property prop = doc.getProperty(field);
                                if (prop.isList()) {
                                    return doc.getPropertyValue(field);
                                } else {
                                    LOG.warn("Property " + field + " is not a list");
                                }
                            } catch (PropertyNotFoundException e) {
                                LOG.trace("Ignore document property " + field + " for " + doc.getId());
                            }
                        }
                    }
                }
            } catch (NuxeoException e) {
                LOG.trace("Document is not found into ResultSet " + uuid);
            }
        }
        return null;
    }

}

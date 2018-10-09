package org.athento.nuxeo.security.core;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.security.api.*;
import org.athento.nuxeo.security.util.PasswordHelper;
import org.athento.nuxeo.security.util.TemplatesHelper;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.InitialContext;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Remember password component.
 *
 * @author <a href="vs@athento.com">Victor Sanchez</a>
 */
public class RememberPasswordComponent extends DefaultComponent implements
        RememberPasswordService {

    /** Remember password doctype. */
    public static final String REMEMBER_PASSWORD_DOCTYPE = "RememberPassword";
    public static final String REMEMBER_PASSWORD_CONTAINER_DOCTYPE = "RememberPasswordContainer";

    /** Old password days to able repeat. */
    public static final int OLD_PASSWORD_DAYS = 30 * 12 * 2; // 2 years

    protected static Log LOG = LogFactory.getLog(RememberPasswordComponent.class);

    public static final String NUXEO_URL_KEY = "nuxeo.url";

    protected String repoName = null;

    protected TemplatesHelper templatesHelper = new TemplatesHelper();

    private static final String SEND_REMEMBERPASSWORD_SUBMITTED_EVENT = "rememberPasswordSubmitted";
    private static final String PASSWORD_EVENT = "passwordChanged";

    protected String getTargetRepositoryName() {

        if (repoName == null) {
            try {
                RepositoryManager rm = Framework.getService(RepositoryManager.class);
                repoName = rm.getDefaultRepositoryName();
            } catch (Exception e) {
                LOG.error("Error while getting default repository name", e);
                repoName = "default";
            }
        }
        return repoName;
    }

    /**
     * Get mail service.
     *
     * @return
     */
    protected String getJavaMailJndiName() {
        return Framework.getProperty("jndi.java.mail", "java:/Mail");
    }

    /**
     * Get remember password model.
     *
     * @param email is the email
     * @param mode is the change password mode
     * @return get remember password model
     * @throws ClientException on error
     */
    public DocumentModel getRememberPasswordModel(String email, String mode) throws ClientException {
        RememberPasswordModelCreator creator = new RememberPasswordModelCreator(email, mode);
        creator.runUnrestricted();
        return creator.getRememberPasswordModel();
    }

    /**
     * Get root document for remember passwords.
     *
     * @param session
     * @return the root document
     * @throws ClientException on error
     */
    public DocumentModel getOrCreateRootDocument(CoreSession session) throws ClientException {
        String targetPath = "/management/RememberPasswordRequests";
        DocumentRef targetRef = new PathRef(targetPath);
        DocumentModel root;
        if (!session.exists(targetRef)) {
            root = session.createDocumentModel(REMEMBER_PASSWORD_CONTAINER_DOCTYPE);
            root.setPathInfo("/management/",
                    "RememberPasswordRequests");
            root.setPropertyValue("dc:title", "Remember Password Requests Container");
            root = session.createDocument(root);
        } else {
            root = session.getDocument(targetRef);
        }
        return root;
    }

    /**
     * Remember document model creator.
     */
    protected class RememberPasswordModelCreator extends UnrestrictedSessionRunner {

        DocumentModel rememberPasswordModel;
        String email;
        String mode;

        /**
         * Constructor.
         *
         * @param email is the email
         * @param mode "recovery" or "expiration"
         */
        public RememberPasswordModelCreator(String email, String mode) {
            super(getTargetRepositoryName());
            this.email = email;
            this.mode = mode;
        }

        @Override
        public void run() throws ClientException {
            rememberPasswordModel = session.createDocumentModel(REMEMBER_PASSWORD_DOCTYPE);
            rememberPasswordModel.setPropertyValue("remember:email", email);
            rememberPasswordModel.setPropertyValue("remember:mode", mode);
        }

        public DocumentModel getRememberPasswordModel() {
            return rememberPasswordModel;
        }
    }

    /**
     * Class to create a new remember password request.
     */
    protected class RememberPasswordCreator extends UnrestrictedSessionRunner {

        protected String rememberPasswordUuid;

        protected DocumentModel sendRememberPasswordModel;

        public String getRememberPasswordUuid() {
            return rememberPasswordUuid;
        }

        public RememberPasswordCreator(DocumentModel sendRememberPasswordModel) {
            super(getTargetRepositoryName());
            this.sendRememberPasswordModel = sendRememberPasswordModel;
        }

        @Override
        public void run() throws ClientException {

            String title = "Remember password request for "
                    + sendRememberPasswordModel.getPropertyValue("remember:email");
            String name = IdUtils.generateId(title + "-"
                    + System.currentTimeMillis());

            String targetPath = getOrCreateRootDocument(session).getPathAsString();

            sendRememberPasswordModel.setPathInfo(targetPath, name);
            sendRememberPasswordModel.setPropertyValue("dc:title", title);

            // Create document
            sendRememberPasswordModel = session.createDocument(sendRememberPasswordModel);

            // Get new document id
            rememberPasswordUuid = sendRememberPasswordModel.getId();

            // Send email
            /*Map<String, Serializable> additionnalInfo = new HashMap<String, Serializable>();
            if (!additionnalInfo.containsKey("enterPasswordUrl")) {
                additionnalInfo.put("enterPasswordUrl", buildEnterPasswordUrl());
            }
            sendChangePasswordEmail(additionnalInfo, sendRememberPasswordModel);*/

            // Send event
            sendEvent(session, sendRememberPasswordModel, getNameEventSendRememberPasswordSubmitted());

            session.save();
        }

    }

    protected String buildEnterPasswordUrl() {
        String baseUrl = Framework.getProperty(NUXEO_URL_KEY);

        baseUrl = StringUtils.isBlank(baseUrl) ? "/" : baseUrl;
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        String enterPasswordUrl = "site/security/enterpassword/";
        if (enterPasswordUrl.startsWith("/")) {
            enterPasswordUrl = enterPasswordUrl.substring(1);
        }
        return baseUrl.concat(enterPasswordUrl);
    }

    /**
     * Change password.
     *
     * @param document
     * @param password
     * @throws RememberPasswordException
     */
    @Override
    public void changePassword(DocumentModel document, String password) throws RememberPasswordException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Changing password...");
        }
        RememberPasswordGetEmail rememberPasswordGetEmail = new RememberPasswordGetEmail(document.getId());
        rememberPasswordGetEmail.runUnrestricted();
        DocumentModelList users = rememberPasswordGetEmail.getUsersByEmail();
        if (users.size() > 1) {
            throw new RememberPasswordException("Multiple users with this email");
        } else if (users.isEmpty()) {
            throw new RememberPasswordException("User with this email is not found");
        } else {
            RememberPasswordSave save = new RememberPasswordSave(repoName, document,
                    (String) users.get(0).getPropertyValue("user:username")
                    , password);
            save.runUnrestricted();
        }
    }

    /**
     * Check if is valid user by email.
     *
     * @param email
     * @return
     */
    @Override
    public boolean isValidUserByEmail(String email) {
        // Get user by email
        UserManager userManager = Framework.getService(UserManager.class);
        Map<String, Serializable> filter = new HashMap<>();
        filter.put("email", email);
        List<DocumentModel> users = userManager.searchUsers(filter, null);
        return users.size() == 1;
    }

    /**
     * Get the email for a remember password document.
     */
    protected class RememberPasswordGetEmail extends UnrestrictedSessionRunner {

        protected DocumentModelList users;
        protected String rememberDocId;

        public RememberPasswordGetEmail(String rememberDocId) {
            super(getTargetRepositoryName());
            this.rememberDocId = rememberDocId;
        }

        public DocumentModelList getUsersByEmail() {
            return users;
        }

        @Override
        public void run() throws ClientException {
            UserManager userManager = Framework.getService(UserManager.class);
            DocumentModel rememberPasswordDocument = session.getDocument(new IdRef(rememberDocId));
            String email = (String) rememberPasswordDocument.getPropertyValue("remember:email");
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("email", email);
            this.users = userManager.searchUsers(filter, null);
            rememberPasswordDocument.detach(sessionIsAlreadyUnrestricted);
        }
    }

    /**
     * Remember password validator.
     */
    protected class RememberPasswordValidator extends UnrestrictedSessionRunner {

        protected String uuid;

        protected Map<String, Serializable> rememberPasswordData = new HashMap<String, Serializable>();

        protected Map<String, Serializable> additionnalInfo;

        public RememberPasswordValidator(String uuid, Map<String, Serializable> additionalInfo) {
            super(getTargetRepositoryName());
            this.uuid = uuid;
            this.additionnalInfo = additionalInfo;
        }

        public Map<String, Serializable> getRememberPasswordData() {
            return rememberPasswordData;
        }

        @Override
        public void run() throws ClientException {
            // Check valid password at first
            if (!PasswordHelper.isValidPassword((String) this.additionnalInfo.get("password"))) {
                throw new InvalidPasswordException("Invalid password.");
            }
            DocumentRef idRef = new IdRef(uuid);
            DocumentModel rememberPasswordDoc = session.getDocument(idRef);
            if (rememberPasswordDoc.getLifeCyclePolicy().equals(
                    "rememberPasswordRequest")) {
                    if (rememberPasswordDoc.getCurrentLifeCycleState().equals(
                            "changed")) {
                        throw new AlreadyRememberPasswordException(
                                "Recovery password request was processed.");
                    } else if (!rememberPasswordDoc.getCurrentLifeCycleState().equals(
                            "requested")) {
                        throw new RememberPasswordException(
                                "Recovery password request has not been accepted yet.");
                    }
            }

            session.saveDocument(rememberPasswordDoc);
            sendEvent(session, rememberPasswordDoc, getNameEventRememberPasswordChanged());
            rememberPasswordData.put("changePasswordDoc", rememberPasswordDoc);
        }

    }

    /**
     * Change password request validator.
     */
    protected class ChangePasswordRequestIdValidator extends UnrestrictedSessionRunner {

        protected String requestId;
        protected String mode;

        public ChangePasswordRequestIdValidator(String id, String mode) {
            super(getTargetRepositoryName());
            this.requestId = id;
            this.mode = mode;
        }

        @Override
        public void run() throws ClientException {
            DocumentRef idRef = new IdRef(requestId);
            if (!session.exists(idRef)) {
                throw new RememberPasswordException(
                        "Password request with id "
                                + requestId + " is not found");
            }
            // Check request mode
            DocumentModel rememberPasswordDoc = session.getDocument(idRef);
            if (mode != null && !rememberPasswordDoc.getPropertyValue("remember:mode").equals(mode)) {
                throw new RememberPasswordException(
                        "Change password request has invalid mode.");
            }
            // If request exists, check lifecycle state
            if (rememberPasswordDoc.getCurrentLifeCycleState().equals("changed")) {
                throw new AlreadyRememberPasswordException(
                        "Change password request has already been processed.");
            }
        }
    }

    /**
     * Send event util class.
     *
     * @param session
     * @param source
     * @param evName
     * @return
     * @throws RememberPasswordException on error
     */
    protected EventContext sendEvent(CoreSession session, DocumentModel source,
                                     String evName) throws RememberPasswordException {
        try {
            EventService evService = Framework.getService(EventService.class);
            EventContext evContext = new DocumentEventContext(session,
                    session.getPrincipal(), source);
            Event event = evContext.newEvent(evName);
            evService.fireEvent(event);
            return evContext;
        } catch (RememberPasswordException ue) {
            LOG.warn("Error during recovery password processing", ue);
            throw ue;
        } catch (Exception e) {
            LOG.error("Error witg event service", e);
            return null;
        }

    }

    /**
     * Send change password email.
     *
     * @param additionnalInfo
     * @param rememberPasswordDoc
     * @throws ClientException
     */
    protected void sendChangePasswordEmail(
            Map<String, Serializable> additionnalInfo,
            DocumentModel rememberPasswordDoc) throws ClientException {

        String emailAdress = (String) rememberPasswordDoc.getPropertyValue("remember:email");

        Map<String, Serializable> input = new HashMap<String, Serializable>();
        input.put("changePasswordDoc", rememberPasswordDoc);
        input.put("info", (Serializable) additionnalInfo);
        StringWriter writer = new StringWriter();

        try {
            templatesHelper.getRenderingEngine().render(
                    "skin/views/webSecurity/ChangePasswordTemplate.ftl", input, writer);
        } catch (Exception e) {
            throw new ClientException("Error during rendering email", e);
        }

        String body = writer.getBuffer().toString();
        try {
            generateMail(emailAdress, null, "Change your password", body);
        } catch (Exception e) {
            throw new ClientException("Error while sending mail : ", e);
        }
    }

    /**
     * Generate email.
     *
     * @param destination
     * @param copy
     * @param title
     * @param content
     * @throws Exception
     */
    protected void generateMail(String destination, String copy, String title,
            String content) throws Exception {

        InitialContext ic = new InitialContext();
        Session session = (Session) ic.lookup(getJavaMailJndiName());

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(session.getProperty("mail.from")));
        msg.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse(destination, false));
        if (!StringUtils.isBlank(copy)) {
            msg.addRecipient(Message.RecipientType.CC, new InternetAddress(
                    copy, false));
        }

        msg.setSubject(title, "UTF-8");
        msg.setSentDate(new Date());
        msg.setContent(content, "text/html; charset=utf-8");

        Transport.send(msg);
    }

    /**
     * Create a remember password request.
     *
     * @param email is the email
     * @param mode "recovery" or "expiration"
     * @return is the generated request id
     * @throws RememberPasswordException
     */
    @Override
    public String submitRememberPasswordRequest(String email, String mode)
            throws RememberPasswordException {
        // Create a new remember password model
        DocumentModel changePasswordModel = getRememberPasswordModel(email, mode);
        RememberPasswordCreator creator = new RememberPasswordCreator(changePasswordModel);
        creator.runUnrestricted();
        return creator.getRememberPasswordUuid();
    }

    /**
     * Validate password change.
     *
     * @param requestId
     * @param additionalInfo
     * @return
     * @throws ClientException
     * @throws RememberPasswordException
     */
    @Override
    public Map<String, Serializable> validatePasswordChange(String requestId, Map<String, Serializable> additionalInfo) {
        RememberPasswordValidator validator = new RememberPasswordValidator(requestId, additionalInfo);
        validator.runUnrestricted();
        return validator.getRememberPasswordData();
    }

    /**
     * Delete remember password request.
     *
     * @param session
     * @param rememberPasswordDocs
     * @throws ClientException
     */
    @Override
    public void deleteRememberPasswordRequests(CoreSession session,
            List<DocumentModel> rememberPasswordDocs) throws ClientException {
        for (DocumentModel rememberPasswordDoc : rememberPasswordDocs) {
            if (!rememberPasswordDoc.hasSchema("remember")) {
                throw new ClientException(
                        "Recovery password document do not contains needed schema");
            }
            session.removeDocument(rememberPasswordDoc.getRef());
        }
    }

    /**
     * Get a remember password request for email.
     *
     * @param email
     * @return
     * @throws ClientException
     */
    @Override
    public DocumentModelList getRememberPasswordForEmail(
                                                     final String email, final String mode) throws ClientException {
        final DocumentModelList rememberPasswordDocs = new DocumentModelListImpl();
        new UnrestrictedSessionRunner(getTargetRepositoryName()) {
            @Override
            public void run() throws ClientException {
                String query = "SELECT * FROM Document WHERE ecm:currentLifeCycleState != 'validated' AND"
                        + " ecm:mixinType = 'RememberPassword' AND"
                        + " remember:email = '%s'AND remember:mode = '%s' AND ecm:isCheckedInVersion = 0";
                query = String.format(query, email, mode);
                rememberPasswordDocs.addAll(session.query(query));
            }
        }.runUnrestricted();
        return rememberPasswordDocs;
    }

    /**
     * Check change password request id.
     *
     * @param requestId
     * @param mode is "recovery" or "expiration" or null for both
     * @throws ClientException
     * @throws RememberPasswordException
     */
    @Override
    public void checkChangePasswordRequestId(final String requestId, final String mode) throws ClientException,
            RememberPasswordException {
        ChangePasswordRequestIdValidator runner = new ChangePasswordRequestIdValidator(requestId, mode);
        runner.runUnrestricted();
    }

    public String getNameEventSendRememberPasswordSubmitted() {
        return SEND_REMEMBERPASSWORD_SUBMITTED_EVENT;
    }

    public String getNameEventRememberPasswordChanged() {
        return PASSWORD_EVENT;
    }
}

/**
 *
 */
package org.athento.nuxeo.validator;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIViewRoot;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.xml.bind.ValidationException;

import com.sun.faces.component.visit.FullVisitContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

/**
 * @author athento
 */
@Name("bFieldValidator")
@Scope(ScopeType.CONVERSATION)
public class FieldValidatorBean implements Serializable {

    public static final String NIFletters = "TRWAGMYFPDXBNJZSQVHLCKET";

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(FieldValidatorBean.class);

    @In(create = true)
    protected Map<String, String> messages;

    @Begin(join = true)
    @Create
    public void init() {
        if (_log.isDebugEnabled()) {
            _log.debug("Initting bean");
        }
    }

    public boolean accept() {
        if (_log.isDebugEnabled()) {
            _log.debug("Accepting form");
        }
        return true;
    }

    public void validateBankAccountNumber_byParts(FacesContext context,
                                                  UIComponent component, Object value) {
        String bankCode = retrieveInputComponentValue_asString(component,
                "bankCodeInputId");
        String branchCode = retrieveInputComponentValue_asString(component,
                "branchCodeInputId");
        String controlDigit = retrieveInputComponentValue_asString(component,
                "dcInputId");
        String accountNumber = retrieveInputComponentValue_asString(component,
                "accountNumberInputId");

        if (!isBankAccount(bankCode + branchCode + controlDigit +
                accountNumber)) {
            LOG.error("Invalid bank account " + bankCode + " " + branchCode + " " + controlDigit + " " + accountNumber);
            // display an error in the input form
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                    context,
                    "label.error.validator.bank.account.number"), null);
            throw new ValidatorException(message);
        }
    }

    public void validateBankAccountNumber_allInOne(FacesContext context,
                                                   UIComponent component, Object value) {
        if (_log.isDebugEnabled()) {
            _log.debug("Validating bank account: " + value);
        }
        String ban = (String) value;
        String bankCode = ban.substring(0, 4);
        String branchCode = ban.substring(4, 8);
        String controlDigit = ban.substring(8, 10);
        String accountNumber = ban.substring(10, 20);

        if (isBankAccount(bankCode + branchCode
                + controlDigit
                + accountNumber)) {
            // do nothing as the given string is well-formed
            return;
        } else {
            // display an error in the input form
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                    context,
                    "label.error.validator.bank.account.number"), null);
            throw new ValidatorException(message);
        }
    }

    public void validateEmailAddress(FacesContext context,
                                     UIComponent component, Object value) {
        if (_log.isDebugEnabled()) {
            _log.debug("Validating emailAddress: " + value);
        }
        String emailAddress = (String) value;
        String expression = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$";
        if (isValid(emailAddress, expression)) {
            // do nothing as the given string is well-formed
            return;
        } else {
            // display an error in the input form
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                    context, "label.error.validator.email"), null);
            throw new ValidatorException(message);
        }
    }

    public void validateHomePhoneNumber(FacesContext context,
                                        UIComponent component, Object value) {
        if (_log.isDebugEnabled()) {
            _log.debug("Validating homePhoneNumber: " + value);
        }
        String phoneNumber = (String) value;
        String expression = "^[89]\\d{8}$";
        if (isValid(phoneNumber, expression)) {
            // do nothing as the given string is well-formed
            return;
        } else {
            // display an error in the input form
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                    context, "label.error.validator.homePhoneNumber"),
                    null);
            throw new ValidatorException(message);
        }
    }

    public void validateIBAN_PT(FacesContext context, UIComponent component,
                                Object value) {
        if (_log.isDebugEnabled()) {
            _log.debug("Validating IBAN PT: " + value);
        }
        // 1st step
        String iban = value.toString().replaceAll("\\s", ""); // Clean blank
        // spaces
        iban = iban.replaceAll("[a-zA-Z]", "");

        // 2nd & 3rd step
        iban = iban + "252950"; // Form the 25 digit PT IBAN
        // 4th step
        BigInteger ibanD = new BigInteger(iban);
        BigInteger mod = new BigInteger("97");
        BigInteger rest = ibanD.remainder(mod);

        if (rest.equals(new BigInteger("1"))) {
            return;
        } else {
            // display an error in the input form
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                    context,
                    "label.error.validator.bank.account.number"), null);
            throw new ValidatorException(message);
        }
    }

    public void validateMobilePhoneNumber(FacesContext context,
                                          UIComponent component, Object value) {
        if (_log.isDebugEnabled()) {
            _log.debug("Validating mobilePhoneNumber: " + value);
        }
        String phoneNumber = (String) value;
        String expression = "^[67]\\d{8}$";
        if (isValid(phoneNumber, expression)) {
            // do nothing as the given string is well-formed
            return;
        } else {
            // display an error in the input form
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    ComponentUtils.translate(context,
                            "label.error.validator.mobilePhoneNumber"), null);
            throw new ValidatorException(message);
        }
    }

    public void validateColombiaMobilePhoneNumber(FacesContext context,
                                          UIComponent component, Object value) {
        if (_log.isDebugEnabled()) {
            _log.debug("Validating CO mobilePhoneNumber: " + value);
        }
        String phoneNumber = (String) value;
        String expression = "^3\\d{9}$";
        if (isValid(phoneNumber, expression)) {
            // do nothing as the given string is well-formed
            return;
        } else {
            // display an error in the input form
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    ComponentUtils.translate(context,
                            "label.error.validator.celular"), null);
            throw new ValidatorException(message);
        }
    }

    public void validateNIF(FacesContext context, UIComponent component,
                            Object value) {
        if (_log.isDebugEnabled()) {
            _log.debug("Validating nif: " + value);
        }
        String nif = (String) value;

        if (nif.toUpperCase().startsWith("X")
                || nif.toUpperCase().startsWith("Y")
                || nif.toUpperCase().startsWith("Z")) {
            nif = nif.substring(1);
            if (_log.isDebugEnabled()) {
                _log.debug("It's a NIE. Removing first char: " + nif);
            }
        }
        try {
            if (isValidNIF(nif)) {
                Long number = Long
                        .parseLong(nif.substring(0, nif.length() - 1));
                String letter = nif.substring(nif.length() - 1, nif.length());
                int pos = (int) (number % 23);
                String matchingLetter = FieldValidatorBean.NIFletters
                        .substring(pos, pos + 1);
                if (!(letter.toUpperCase().equals(matchingLetter.toUpperCase()))) {
                    throw new ValidationException(ComponentUtils.translate(
                            context, "label.error.validator.nifLetter"));
                }
            } else {
                throw new ValidationException(ComponentUtils.translate(context,
                        "label.error.validator.nifFormat"));
            }
        } catch (ValidationException e) {
            // display an error in the input form
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, e.getMessage(), null);
            throw new ValidatorException(message);
        }
        return; // all validations are ok
    }

    public void validateNIFLazy(FacesContext context, UIComponent component,
                                Object value) {
        if (_log.isDebugEnabled()) {
            _log.debug("Validating nif lazy: " + value);
        }
        String nif = (String) value;
        if (isValidNIF(nif)) {
            // do nothing as the given string is well-formed
            return;
        } else {
            // display an error in the input form
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                    context, "label.error.validator.homePhoneNumber"),
                    null);
            throw new ValidatorException(message);
        }
    }

    public void validateText(FacesContext context, UIComponent component,
                             Object value) {
        if (_log.isDebugEnabled()) {
            _log.debug("Validating text: " + value);
        }
        String theValue = (String) value;
        String expression = "^[a-zA-Z0-9]{0,30}$";
        if (isValid(theValue, expression)) {
            // do nothing as the given string is well-formed
            return;
        } else {
            // display an error in the input form
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                    context, "label.error.validator.text"), null);
            throw new ValidatorException(message);
        }
    }

    /**
     * Validate a value for a regular expression.
     *
     * @param context
     * @param component
     * @param value
     */
    public void validateRegex(FacesContext context, UIComponent component,
                             Object value) {
        if (_log.isDebugEnabled()) {
            _log.debug("Validating regex: " + value);
        }
        String theValue = (String) value;
        // Check regex attribute
        String regex = (String) component.getAttributes().get("regex");
        boolean required = (Boolean) component.getAttributes().get("required");
        if (!isValid(theValue, regex, required)) {
            // display an error in the input form
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                    context, "label.error.validator.text"), null);
            throw new ValidatorException(message);
        }
    }

    /**
     * Validate a value for a regular expression.
     *
     * @param context
     * @param component
     * @param value
     */
    public void isRequiredIf(FacesContext context, UIComponent component,
                             Object value) {
        if (_log.isDebugEnabled()) {
            _log.debug("Validating required: " + value);
        }
        String theValue = (String) value;
        // Check regex attribute
        String regex = (String) component.getAttributes().get("regex");
        // Takes the required field
        String requiredField = (String) component.getAttributes().get("requiredField");

        UIComponent componentFound = findComponent("nxw_" + requiredField);

        if (componentFound != null) {
            _log.debug("Componente encontrado: " + componentFound.getId());
            UIInput requiredFieldComponent = (UIInput) componentFound;
            Object requiredValue = new java.lang.String();
            try {
                requiredValue = requiredFieldComponent.getValue();
                _log.info("Validating against component " + requiredField + ", with value: " + requiredValue
                        + ", against regular expression: " + regex);
            }
            catch (Exception e){
                _log.error("Error obtaining value from the required field. Validation will be skipped.");
                _log.error("ERROR: " + e);
            }
            if (!isRequiredIfEvaluation(theValue, requiredField, (String) requiredValue, regex)) {
                // display an error in the input form
                FacesMessage message = new FacesMessage(
                        FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                        context, "label.error.validator.text"), null);
                throw new ValidatorException(message);
            }

        }
        else {
            _log.debug("Componente " + requiredField + " no encontrado.");
        }

    }

    /**
     * Check if a valid value for a regex.
     *
     * @param value
     * @param regex
     * @return
     */
    private boolean isValid(String value, String regex) {
        return isValid(value, regex, false);
    }

    /**
     * Check if a valid value for a regex.
     *
     * @param value
     * @param regex
     * @param required
     * @return
     */
    private boolean isValid(String value, String regex, boolean required) {
        if (_log.isDebugEnabled()) {
            _log.debug("Validating [" + value + "] against regexp [" + regex
                    + "], required= " + required);
        }
        // Check regex is not null
        if (regex == null) {
            return false;
        }
        if (!required && (value == null || value.isEmpty())) {
            return true;
        }
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(value);
        if (matcher.matches()) {
            if (_log.isDebugEnabled()) {
                _log.debug("Match successful for value: " + value);
            }
            return true;
        } else {
            if (_log.isDebugEnabled()) {
                _log.debug("Value do not match regular expression: " + regex);
            }
            return false;
        }
    }

    public UIComponent findComponent(final String id) {

        FacesContext context = FacesContext.getCurrentInstance();
        UIViewRoot root = context.getViewRoot();
        final UIComponent[] found = new UIComponent[1];

        root.visitTree(new FullVisitContext(context), new VisitCallback() {
            @Override
            public VisitResult visit(VisitContext context, UIComponent component) {
                if(component.getId().equals(id)){
                    found[0] = component;
                    return VisitResult.COMPLETE;
                }
                return VisitResult.ACCEPT;
            }
        });

        return found[0];

    }

    /**
     * Check if a value is valid for a regex.
     *
     * @param value
     * @param regexp
     * @return
     */
    private boolean isRequiredIfEvaluation(String value, String requiredField, String requiredFieldValue, String regexp) {
        if (_log.isDebugEnabled()) {
            _log.info("Validating current field with value [" + value + "] is not null, if field [" + requiredField +
                    "] with value [" + requiredFieldValue + "] accomplish regexp [" + regexp + "].");
        }
        // Check regex is not null
        if (regexp == null) {
            return false;
        }

        // Check requiredField is not null
        if (requiredField == null) {
            return false;
        }

        Pattern pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(requiredFieldValue);
        if (matcher.matches()) {
            if (value == null | value.equals("")) {
                _log.info("Match successful for value and value is null: " + requiredFieldValue);
                return false;
            }
            else {
                if (_log.isDebugEnabled()) {
                    _log.debug("Match successful for value and value is not null: " + requiredFieldValue);
                }
                return true;
            }


        } else {
            if (_log.isDebugEnabled()) {
                _log.debug("Value do not match regular expression: " + regexp);
            }
            _log.info("Value do not match regular expression: " + regexp);
            return true;
        }
    }

    private boolean isValidNIF(String nif) {
        if (_log.isDebugEnabled()) {
            _log.debug("is a valid NIF?: " + nif);
        }
        String expression = "(\\d{8})([A-Za-z]{1})";
        if (isValid(nif, expression)) {
            if (_log.isDebugEnabled()) {
                _log.debug("it is :)");
            }
            return true;
        } else {
            if (_log.isDebugEnabled()) {
                _log.debug("it's not :(");
            }
            return false;
        }
    }

    private Object retrieveInputComponentValue(UIComponent anchor,
                                               String componentId) {
        Map attributes = anchor.getAttributes();
        String inputId = (String) attributes.get(componentId);
        UIInput component = (UIInput) anchor.findComponent(inputId);
        return component.getLocalValue();
    }

    private String retrieveInputComponentValue_asString(UIComponent anchor,
                                                        String componentId) {
        Object o = retrieveInputComponentValue(anchor, componentId);
        return (o != null ? o.toString() : null);
    }


    // Check if bank account is valid
    public static boolean isBankAccount(String cuenta) {

        Pattern cuentaPattern = Pattern.compile("\\d{20}");
        Matcher m = cuentaPattern.matcher(cuenta);
        if (m.matches()) {

            String bank = "00" + cuenta.substring(0, 8);

            int sum = Integer.parseInt(bank.substring(0, 1)) * 1 +
                    Integer.parseInt(bank.substring(1, 2)) * 2 +
                    Integer.parseInt(bank.substring(2, 3)) * 4 +
                    Integer.parseInt(bank.substring(3, 4)) * 8 +
                    Integer.parseInt(bank.substring(4, 5)) * 5 +
                    Integer.parseInt(bank.substring(5, 6)) * 10 +
                    Integer.parseInt(bank.substring(6, 7)) * 9 +
                    Integer.parseInt(bank.substring(7, 8)) * 7 +
                    Integer.parseInt(bank.substring(8, 9)) * 3 +
                    Integer.parseInt(bank.substring(9, 10)) * 6;

            int control = 11 - (sum % 11);

            if (control == 10) {
                control = 1;
            } else if (control == 11) {
                control = 0;
            }

            int bankControl = Integer.parseInt(cuenta.substring(8, 9));
            if (bankControl != control) {
                return false;
            }

            sum = Integer.parseInt(cuenta.substring(10, 11)) * 1 +
                    Integer.parseInt(cuenta.substring(11, 12)) * 2 +
                    Integer.parseInt(cuenta.substring(12, 13)) * 4 +
                    Integer.parseInt(cuenta.substring(13, 14)) * 8 +
                    Integer.parseInt(cuenta.substring(14, 15)) * 5 +
                    Integer.parseInt(cuenta.substring(15, 16)) * 10 +
                    Integer.parseInt(cuenta.substring(16, 17)) * 9 +
                    Integer.parseInt(cuenta.substring(17, 18)) * 7 +
                    Integer.parseInt(cuenta.substring(18, 19)) * 3 +
                    Integer.parseInt(cuenta.substring(19, 20)) * 6;

            control = 11 - (sum % 11);

            if (control == 10)
                control = 1;
            else if (control == 11)
                control = 0;


            int accountControl = Integer.parseInt(cuenta.substring(9, 10));
            if (accountControl != control) {
                return false;
            } else {
                return true;
            }

        } else {
            return false;
        }

    }

    // Takes referenced component to check its value
    private UIInput getReferencedComponent(String attribute, UIComponent component) {
        Map<String, Object> attributes = component.getAttributes();

        String targetComponentId = (String) attributes.get(attribute);

        if (targetComponentId == null) {
            LOG.error(String.format("Target component id (%s) not found in attributes", attribute));
            return null;
        }

        UIInput targetComponent = (UIInput) component.findComponent(targetComponentId);
        if (targetComponent == null) {
            return null;
        }

        return targetComponent;
    }

    private static Log _log = LogFactory.getLog(FieldValidatorBean.class);
    /**
     *
     */
    private static final long serialVersionUID = 1L;
}

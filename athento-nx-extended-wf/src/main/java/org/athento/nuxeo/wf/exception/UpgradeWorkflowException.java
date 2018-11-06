package org.athento.nuxeo.wf.exception;

/**
 * Upgrade exception.
 */
public class UpgradeWorkflowException extends Exception {

    public UpgradeWorkflowException() {
        super();
    }

    public UpgradeWorkflowException(String message) {
        super(message);
    }

    public UpgradeWorkflowException(String message, Throwable cause) {
        super(message, cause);
    }

    public UpgradeWorkflowException(Throwable cause) {
        super(cause);
    }

    protected UpgradeWorkflowException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

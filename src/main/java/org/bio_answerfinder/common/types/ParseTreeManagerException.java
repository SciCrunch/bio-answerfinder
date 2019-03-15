package org.bio_answerfinder.common.types;

/**
 * Exception used by {@link ParseTreeManager}.
 *
 * @author I. Burak Ozyurt
 * @version $Id: ParseTreeManagerException.java,v 1.2 2007/02/05 09:51:17
 *          bozyurt Exp $
 */
public class ParseTreeManagerException extends Exception {
    private static final long serialVersionUID = -5473095390282591119L;

    public ParseTreeManagerException() {
        super();
    }

    public ParseTreeManagerException(String message) {
        super(message);
    }

    public ParseTreeManagerException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParseTreeManagerException(Throwable cause) {
        super(cause);
    }

}

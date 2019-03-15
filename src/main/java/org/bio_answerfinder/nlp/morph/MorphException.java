package org.bio_answerfinder.nlp.morph;

/**
 * @author I. Burak Ozyurt
 * @version $Id$
 */
public class MorphException extends Exception {
    private static final long serialVersionUID = 1L;

    public MorphException() {
        super();
    }

    public MorphException(String message) {
        super(message);
    }

    public MorphException(String message, Throwable cause) {
        super(message, cause);
    }

    public MorphException(Throwable cause) {
        super(cause);
    }

}

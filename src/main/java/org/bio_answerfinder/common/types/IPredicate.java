package org.bio_answerfinder.common.types;

/**
 * @author I. Burak Ozyurt
 * @version $Id$
 */
public interface IPredicate<T> {
    public boolean satisfied(T arg);
}

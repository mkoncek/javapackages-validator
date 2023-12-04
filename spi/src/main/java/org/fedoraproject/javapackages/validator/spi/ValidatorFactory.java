package org.fedoraproject.javapackages.validator.spi;

import java.util.List;

/**
 * A {@link org.fedoraproject.javapackages.validator.spi.Validator} factory.
 *
 * @author Marián Konček
 */
public interface ValidatorFactory {
    /**
     * Get a list of validators to be executed.
     * @return A list of validators to be executed. Must not be {@code null}.
     */
    List<Validator> getValidators();
}

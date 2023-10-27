package org.fedoraproject.javapackages.validator;

import java.util.List;

public interface ValidatorFactory {
	List<Validator> getValidators();
}

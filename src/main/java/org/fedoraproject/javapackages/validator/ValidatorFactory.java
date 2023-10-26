package org.fedoraproject.javapackages.validator;

public interface ValidatorFactory {
	Validator getValidatorFor(String testName);
}

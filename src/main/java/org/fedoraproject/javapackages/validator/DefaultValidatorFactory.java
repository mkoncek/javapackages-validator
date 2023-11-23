package org.fedoraproject.javapackages.validator;

import java.util.List;

import org.fedoraproject.javapackages.validator.spi.Validator;
import org.fedoraproject.javapackages.validator.spi.ValidatorFactory;
import org.fedoraproject.javapackages.validator.validators.AttributeProvidesValidator;
import org.fedoraproject.javapackages.validator.validators.BytecodeVersionValidator;
import org.fedoraproject.javapackages.validator.validators.JavaExclusiveArchValidator;
import org.fedoraproject.javapackages.validator.validators.JavadocNoarchValidator;
import org.fedoraproject.javapackages.validator.validators.MavenMetadataValidator;
import org.fedoraproject.javapackages.validator.validators.NVRJarMetadataValidator;

public class DefaultValidatorFactory implements ValidatorFactory {
    @Override
    public List<Validator> getValidators() {
        return List.of(new Validator[] {
                new AttributeProvidesValidator(),
                new BytecodeVersionValidator(),
                new JavadocNoarchValidator(),
                new JavaExclusiveArchValidator(),
                // new JpmsProvidesValidator(),
                new MavenMetadataValidator(),
                new NVRJarMetadataValidator(),
        });
    }
}

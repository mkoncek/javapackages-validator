module org.fedoraproject.javapackages.validator {
	requires org.fedoraproject.javapackages.validator.spi;
	uses org.fedoraproject.javapackages.validator.spi.ValidatorFactory;
	// provides org.fedoraproject.javapackages.validator.spi.ValidatorFactory with org.fedoraproject.javapackages.validator.validators.DefaultValidatorFactory;
	
	requires java.compiler;
	requires java.xml;
	
	requires org.apache.commons.collections4;
	requires org.apache.commons.io;
	requires org.apache.commons.text;
	requires org.apache.maven.resolver;
	requires org.apache.maven.resolver.supplier;
	
	requires org.yaml.snakeyaml;
	
	requires static com.github.spotbugs.annotations;
	
	// xmvn has no module name
}

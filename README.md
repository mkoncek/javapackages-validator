# javapackages-validator

This tool is used for checking existing .rpm packages against various criteria.

## Building

Requires OpenJDK 11.

Executing

	mvn package

will compile the project and generate a tarball containing the validator `.jar`
and its dependencies.

Furthermore executing

	mvn install

will unpack the tarball and make the validator `.jar` executable.

## Usage

### In case of `mvn package`

After packaging the project extract files:

	tar -xf target/assembly-${version}.tar.gz

Run the executable `.jar` file:

	java -jar ./assembly-${version}/validator-${version}.jar [OPTIONS]

### In case of `mvn install`

Simply run

	./assembly-${version}/validator-${version}.jar [OPTIONS]

### Command line options

Traditional help with `-h`, `--help`.

An option to print the configuration in XML form after being read, this is used
for debugging. Enabled by `-d`, `--dump-config`.

#### Mandatory

Path to the configuration file is provided by the `-c`, `--config` flag.
More to configuration in the next sections.

The list of `.rpm` files to validate can be provided one of these ways:

* Specify the file paths directly using the `-f`, `--files` flag.

* Specify a file which names the validated files (one file path per line) by
using the `-i`, `--input` flag.

* Stream the file paths separated by new line via the standard input at
the program invocation.

	  ./generator | java -jar validator.jar

#### Optional

The text output is written to the standard output or, if provided by `-o`,
`--output` into the provided file.

For colored output it is possible to set the `-r`, `--color` flag.

For more detailed output which shows which checks failed or succeeded there is
the `-v`, `--verbose` flag.

In order to print only failed checks use the `-n`, `--only-failed` flag.


## Configuration

The configuration file is an XML file.
It contains the main node `<config>`. This contains variable amount of nodes of
type `<rule>`. Each rule must have exactly one `<name>` and `<match>` node.

### Name

`<name>` is used to refer to already defined rules. Two rules cannot have the
same name.

### Match

`<match>` is used to deterimne whether the rule is applicable to given `.rpm` file.
This is determined for each validated package.

It may consist of logical predicates, names of previous rules or `.rpm` file
attributes (such as pachage name) which are checked as regular expressions.

It may also be empty in which case it matches everything (but still must be declared).

#### Expressions

* Logical predicates: **`<not>`**, **`<and>`**, **`<or>`**.

* **`<rule>`** - Matches *exactly* the name of an existing rule.

* Expressions using regular expressions:

	* **`<name>`** - Matches according to the package name.

	* **`<arch>`** - Matches according to the package architecture.

#### Examples

* Matching any `javadoc` package.

	  <match>
	    <name>.*-javadoc.*</name>
	  </match>

* Matching any source `.rpm`.

	  <match>
	    <or>
	      <arch>src</arch>
	      <arch>nosrc</arch>
	    </or>
	  </match>

* Matching non-source package for the `x86_64` architecture, assuming previously
declared rule for matching source packages.

	  <match>
	    <and>
          <not>
            <rule>source</rule>
          </not>
	      <arch>x86_64</arch>
	    </and>
	  </match>

### Checks

Other than `<match>` a rule may contain variable amount of checks to make. These
are:

* **`<rpm-file-size-bytes>`** -
Applies the validator on the file size of the `.rpm` file in bytes.

* **`<files>`** -
Applies the validator on each file path contained in the `.rpm` file.

* **`<requires>`** -
Applies the validator on each string in the `requires` section.

* **`<provides>`** -
Applies the validator on each string in the `provides` section.

* **`<java-bytecode>`** -
Applies the validator on each numeric version string of each `.class` file of
each `.jar` file contained in the `.rpm` file.

### Validators

There are two types of validators: aggregate and primitive.

#### Aggregate

These validators are recursively composed of other validators.

* **`<all>`** -
The validator passes if all its member validators accept the value.

* **`<any>`** -
The validator passes if any of its member validators accept the value.

* **`<none>`** -
The validator passes if none of its member validators accept the value.

#### Primitive

These simply take the string value and accept or reject it.

* **`<regex>`** -
The validator applies a regular expression search to the value. The regular
expression is in the form conforming to the [java.util.regex.Pattern](
https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html
)
class.<br>
Examples:

	* Accepts everything.
	
		  <regex>.*</regex>

* **`<int-range>`** -
The validator contains two integer values separated by a dash (`-`). Whitespace
is ignored. If any of the range limits is ommited then it is substituteb by
negative / positive maximum integer depending on the position within the range.
The validator expects the string value to represent an integer number as well.
It passes if the string value is between the limits specified in the range
**inclusive**.<br>
Examples:

	* Accepts any value in the inclusive range of `[25-75]`.
	
		  <int-range>25-75</int-range>
			
	* Accepts any representable value lesser or equal to `100`.
	
		  <int-range>-100</int-range>`

## Configuration examples

	<config>
	  <!-- Rule for source packages -->
	  <rule>
	    <name>source</name>
	    <match>
	      <or>
	        <arch>src</arch>
	        <arch>nosrc</arch>
	      </or>
	    </match>
	  </rule>
	  
	  <!-- Rule for checking the size of every non-source package -->
	  <rule>
	    <name>size</name>
	    <match>
	      <not>
	        <rule>source</rule>
	      </not>
	    </match>
	    
	    <filesize-mb>
	      <int-range>-15</int-range>
	    </filesize-mb>
	  </rule>
	  
	  <rule>
	    <name>javadoc</name>
	    <match>
	      <and>
	        <not>
	          <rule>source</rule>
	        </not>
	        <name>.*-javadoc.*</name>
	      </and>
	    </match>
	    
	    <!-- -->
	  </rule>
	  
	  <rule>
	    <name>javapackages-tools</name>
	    <match>
	      <and>
	        <not>
	          <rule>source</rule>
	        </not>
	        <name>javapackages-tools</name>
	      </and>
	    </match>
	    
	    <!-- -->
	  </rule>
	  
	  <rule>
	    <name>byaccj</name>
	    <match>
	      <and>
	        <not>
	          <rule>source</rule>
	        </not>
	        <name>byaccj</name>
	      </and>
	    </match>
	    
	    <!-- -->
	  </rule>
	  
	  <!-- Matches all non-source packages -->
	  <rule>
	    <name>generic</name>
		<match>
		  <not>
	        <rule>source</rule>
		  </not>
		</match>
	    
	    <requires>
	      <any>
	        <regex>maven-local</regex>
	        <regex>maven-local-openjdk8</regex>
	        <regex>mvn\(.+:.+\)</regex>
	        <regex>rpmlib\(.+\)</regex>
	        <regex>javapackages-local</regex>
	      </any>
	    </requires>
	  </rule>
	</config>

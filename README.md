# javapackages-validator

This tool is used for checking existing .rpm packages against various criteria.

## Configuration

The configuration file is an XML file.
It contains the main node `<config>`. This contains variable amount of nodes of
type `<rule>`. For each validated package the validator selects applicable
rules. Whether or not a rule is applicable depends on its `<match>` node. Each
rule must have exactly one match.

Additionally there is the option to make a rule **exclusive** by setting:

	<rule>
		<exclusive>true</exclusive>
		...
	</rule>

If the list of applicable rules as read
from top to the bottom contains an exclusive rule then the first exclusive rule
is selected and only that single rule will be applied to the package.

### Match

The `<match>` node may consist of logical predicates or plain epressions which
are checked as regular expressions.

* Logical predicates are **`<not>`**, **`<and>`**, **`<or>`**.

The primitive expressions are:

* **`<name>`** --
Matches according to the package name.

* **`<arch>`** --
Matches according to the package architecture.

Examples:

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

### Checks

Other than `<match>` a rule may contain variable amount of checks to make. These
are:

* **`<filesize-b>`** -- (Also **`-kb`** and **`-mb`**)
Applies the validator on the file size in given units of the `.rpm` file.  
**TODO unify**

* **`<files>`** --
Applies the validator on each file path contained in the `.rpm` file.

* **`<requires>`** --
Applies the validator on each string in the `requires` section.

* **`<provides>`** --
Applies the validator on each string in the `provides` section.

* **`<java-bytecode>`** --
Applies the validator on each numeric version string of each `.class` file of
each `.jar` file contained in the `.rpm` file.

### Validators

There are two types of validators: aggregate and primitive.

#### Aggregate

These validators are recursively composed of other validators.

* **`<all>`** --
The validator passes if all its member validators accept the value.

* **`<any>`** or  **`<whitelist>`** --
The validator passes if any of its member validators accept the value.

* **`<none>`** or  **`<blacklist>`** --
The validator passes if none of its member validators accept the value.

#### Primitive

These simply take the string value and accept or reject it.

* **`<regex>`** --
The validator applies a regular expression search to the value. The regular
expression is in the form conforming to the [java.util.regex.Pattern](
https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html
)
class.
<br>
Examples:
	* Accepts everything.
	
			<regex>.*</regex>

* **`<int-range>`** --
The validator contains two integer values separated by a dash (`-`). Whitespace
is ignored. If any of the range limits is ommited then it is substituteb by
negative / positive maximum integer depending on the position within the range.
The validator expects the string value to represent an integer number as well.
It passes if the string value is between the limits specified in the range
**inclusive**.
<br>
Examples:
	* Accepts any value in the inclusive range of `[25-75]`.
	
			<int-range>25-75</int-range>
	* Accepts any representable value lesser or equal to `100`.
	
			<int-range>-100</int-range>`

## Configuration examples

	<config>
		<!-- Source packages -->
		<rule>
			<exclusive>true</exclusive>
			<match>
				<or>
					<arch>src</arch>
					<arch>nosrc</arch>
				</or>
			</match>
			<!-- -->
		</rule>
		
		<!-- javapackages-tools -->
		<rule>
			<exclusive>true</exclusive>
			<match>
				<name>javapackages-tools</name>
			</match>
			
			<!-- -->
			
		</rule>
		
		<!-- Javadoc packages -->
		<rule>
			<exclusive>true</exclusive>
			<match>
				<name>.*-javadoc.*</name>
			</match>
			<files>
				<regex>.*</regex>
				<!-- -->
			</files>
			<!-- -->
		</rule>
		
		<!-- Everything else -->
		<rule>
			<match>
				<name>.*</name>
			</match>
			<requires>
				<any>
					<regex>maven-local</regex>
					<regex>maven-local-openjdk8</regex>
					<regex>mvn\(.+:.+\)</regex>
					<regex>rpmlib\(.+\)</regex>
					<regex>javapackages-local</regex>
					<!-- -->
				</any>
			</requires>
		</rule>
	</config>

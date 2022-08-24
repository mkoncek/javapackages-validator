# javapackages-validator
Validator runs a single check on a set of `rpm` files.

## Usage
`run.sh <simple class name of the check> [optional flags] <RPM files or directories to test...>`

The parameters specifying RPM files can either be RPM file paths or directories.
In case of directories, the tool recursively searches for `.rpm` files found
inside.

### Optional flags
* **`-c, --config-file`** -- File path of a configuration source, can be specified multiple times
* **`-u, --config-uri`** -- URI of a configuration source, can be specified multiple times
* **`-r, --color`** -- Display debugging output
* **`-x, --debug`** -- Display colored output

## Checks
* **`BytecodeVersionCheck`** -- Inspects the `.class` entries inside `.jar`
  archives inside test rpms and resolves their version against provided
  configuration.

* **`DuplicateFileCheck`** -- Checks whether the set of rpms contains duplicate
  file entries. Ignores duplicate directories as well as duplicate entries of
  rpms in case each rpm is of a unique architecture.

* **`FilesCheck`** -- Inspects the files entries found inside each rpm and
  resolves their path against the provided configuration.

* **`JavadocNoarchCheck`** -- Checks whether all javadoc subpackages have
  BuildArch `noarch`.

* **`JavaExclusiveArchCheck`** -- Checks whether packages follow Fedora policy
  of having `%{java_arches}` field in their `ExclusiveArch` attribute.

* **`RpmFilesizeCheck`** -- Resolves the rpm file size against provided
  configuration.

* **`SymlinkCheck`** -- Inspects the rpm for symbolic links and resolves their
  targets against provided configuration.

* **`attribute.*`** -- Inspects the rpm attributes and resolves them against
  provided configuration.
	* **`ConflictsCheck`**
	* **`EnhancesCheck`**
	* **`ObsoletesCheck`**
	* **`OrderWithRequiresCheck`**
	* **`ProvidesCheck`**
	* **`RecommendsCheck`**
	* **`RequiresCheck`**
	* **`SuggestsCheck`**
	* **`SupplementsCheck`**

# javapackages-validator
Validator runs a single check on a single check on a set of `rpm` files.

## Usage
`run.sh <simple class name of the check> [optional flags] <RPM files or directories to test...>`

The parameters specifying RPM files can either be RPM file paths or directories.
In case of directories, the tool recursively searches for `.rpm` files found
inside.

### Optional flags
* `--config-src [/mnt/config/src] - directory containing configuration sources`
* `--config-bin [/mnt/config/bin] - directory where compiled class files will be put`

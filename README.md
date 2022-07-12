# javapackages-validator
Validator runs a single check on a single check on a set of `rpm` files.

## Usage
`run.sh <simple class name of the check> [optional flags] <RPM files or directories to test...>`

### Optional flags
`--config-src [/mnt/config/src] - directory containing configuration sources`
`--config-bin [/mnt/config/bin] - directory where compiled class files will be put`
`--envroot [/] - root directory to resolve symbolic links against`

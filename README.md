# javapackages-validator

Javapackages-validator is a tool used to test `.rpm` files. It executes checks implemented as Java classes.

## Usage

`Main [optional flags] <validator class name> [validator flags] [-f|-u RPM files or directories to test]...`

      -h, --help - Print help message

Options for specifying validators:

      -sp, --source-path - File path of a source file
      -cp, --class-path - Additional class path entry

The tool recursively finds all `.java` files present in the *source path* directory, compiles them and places the results under the path specified as the *class path*. The entries on this class path are used to add additional validator classes to the tool.

Validator arguments can be immediately followed by space-separated square parentheses the contents of which will be passed as arguments to the validator.

Options for specifying tested RPM files, can be specified multiple times:

      -f, --file - File path of an .rpm file
      -u, --uri - URI of an .rpm file

Optional flags:

      -x, --debug - Display debugging output
      -r, --color - Display colored output


The parameters specifying RPM files can either be RPM file paths or directories. In case of directories, the tool recursively searches for `.rpm` files found inside.

## TMT

The tool contains another main class `MainTmt` which is intended to be invoked  from within Tmt tests. The tool reads the output of discovered tests and  executes validators whose test name matches the discovered tests.

In order to be able to run it in this mode, the environment variables: `TMT_TEST_DATA` and `TMT_TREE` need to be defined.

The optional flags for the validator can be specified in the test plan YAML file under the context in the following form:

      context:
        /name/of/the/test: ["arg1", "arg2", ...]

The tool executes matching validators and creates results in the form of HTML files.

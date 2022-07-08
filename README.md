# javapackages-validator
Validator runs a single check on a single package consisting of possibly
multiple `rpm` files.

## Running from container
First you need to mount two directory paths:

* `/mnt/package/${package_name}` -- directory named after the package and containing the tested `rpm` files
* `/mnt/config/src` -- directory containing `.java` configuration files

Running from container is done as follows:

`$ docker run jp-validator <simple class name of the check>`

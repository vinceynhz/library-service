# Library μService

The purpose of this service is to provide simple access to the database that will host all of the library related 
entities.

## Content
- [Installing](#installing)
- [Data Model](#data-model)

## Installing

For development purposes, the application can be built and run using maven as follows

_build_
```
mvn clean package
```

_run_
```
java -Xms64M -Xmx1024M -jar .\target\library-service-<version>.jar
```

For installing on a server, we have provided a linux shell control script, that can be registered as a service under
/etc/init.d on the server. 

In order to install the following commands can be executed in the server:

1. Get a copy of control_script.sh
```bash
curl https://raw.githubusercontent.com/vinceynhz/library-service/master/src/main/resources/control_script.sh > /tmp/control_script.sh
```

2. Get execution permissions
```bash
sudo chmod +x /tmp/control_script.sh
```

3. Install application
```bash
sudo ./tmp/control_script.sh install
```

The install function of the control script will try to clone the repo from GitHub, build it, installing the built
version of control script on `/etc/init.d` and make the jar available.

The following requisites are needed:

- Maven
- Python 3

## Data Model

See [library data model](https://github.com/vinceynhz/library-service/tree/master/src/main/java/app/tandv/services/data)
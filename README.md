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

The following entities are defined in the data model for this service.

![ERD](https://raw.githubusercontent.com/vinceynhz/library-service/master/doc/ERD.png)

### BOOK
This represents a single book in any given format: paperback, hardback, audiobook, ebook. Other attributes are provided
as well to use as metadata. 

Ordering is an important function of any library as it helps with indexing and organizing books. The ordering for books 
obeys standard library rules:

- book title is normalized to remove any non alphanumeric characters
- first title article is ignored (a, an, the)

Each book has a unique id issued by the database. Plan is to create a surrogate idempotent composite id that can be used
to uniquely identify a book from its title and other attributes.

### AUTHOR
This represents a person that writes a book. We provide a sha256 value derived out of the normalized name of the author 
to uniquely identify it.

Just as with books, the authors also provide an ordering string for indexing and search. The ordering for authors obeys
also standard library rules:

- name is normalized to remove any non alphanumeric characters, honorifics and roman numerals
- first word is considered last in the order of words that integrate a name. For example:
  
  - `Neil deGrasse Tyson` is ordered as `degrasse tyson, neil`
  - `Mary Higgins Clark` is ordered as `higgins clark, mary`
  - `Sir Arthur Conan Doyle` is ordered as `conan doyle, arthur`

### BOOK_AUTHOR
This intermediate table is used to capture the many to many relationship existing between books and authors:

- One author can have many authors
- One author can have many books

And currently we use the database provided ids to match them.

### AUTHOR_ALIAS
This intermediate table is used to capture the one to many relationship existing between authors and other possible
authors.

- One author can have many aliases
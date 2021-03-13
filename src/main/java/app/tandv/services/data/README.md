# Library Data Model

The purpose of this document is to describe the list of all concepts related to the data definition, access and usage

## Definitions

The following definitions are applicable throughout the whole system:

- **[bookId](https://en.wikipedia.org/wiki/Book):** a medium that represents information in the form of writing, images, a 
verbal narration or a combination of them.

- **bookId format:** the kind of physical existence of a bookId. We differentiate the following formats: 
  - **_hardback_**
  - **_paperback_**
  - **_ebook_**
  - **_audiobook_**
  
- **bookId features:** optional characteristics of a bookId that make a difference among other copies of the 
relative same bookId. A bookId may contain 0 or more features. The features we identify are:
  - **_[graphic novel](https://en.wikipedia.org/wiki/Graphic_novel):_** we encompass single periodical issues as well as
  collected editions 
  - **_[anthology](https://en.wikipedia.org/wiki/Anthology):_** a collection of literary works
  - **_revised edition:_** a new version of a bookId previously published by the same author with additional or modified 
  content from the original edition
  - **_complete edition:_** a new version of a bookId that includes material not included in previous versions  
  - **_illustrated:_** a special version with illustrations of a bookId previously published only in writing. Note that 
  this is different than children's books tha are _created_ to have illustrations.   
  - **_variant cover:_** a special version of a bookId with a cover made by a particular artist; this is mostly applicable
  in graphic novels
  - **_large print:_** a special version of a bookId with a larger lettering for ease of readability
  
- **bookId language:** the language in which a specific bookId is written into. This may not correspond to the or

- **contributorId:** a being (human or otherwise) that worked actively in the creation of a _book_. We claim a difference 
in the process of bookId _creation_ to that of bookId _production_. As part of the bookId creation we consider the following 
contributors:
  - **_authors_** 
  - **_illustrators_**
  - **_editors_** (only when many editors may have created different versions of the original manuscript)
  - **_translators_** (only when the translations provide significant differences from the original bookId)

- **[library](https://en.wikipedia.org/wiki/Library):** a collection of books belonging to a user or group of users

- **user:** a being (human or otherwise) partaking in the interactions of the present system

- **catalog:** a sorted list of entities in the system (books, contributors, users, libraries)

## Authority Control

Although the concept sounds overly strict and sober, it refers to transformations done to the attributes of an entity to
ease with the cataloguing of library materials. [Reference](https://en.wikipedia.org/wiki/Authority_control).

We consider the following rules:

### Capitalization:

The capitalization of a word occurs in the following manner:

1. If the given word contains two or more upper case letters (such as the case of acronyms or initials) the word will be
left as is with no changes
2. If the word is a valid roman numeral the word will be converted to upper case
3. Articles will be converted to lower case (unless of the special case of the first word of a bookId title)
4. Finally, the first alphabetical character of the word will be converted to upper case, the rest to lower case

The articles recognized currently are those of the English language: `"a"`, `"an"`, `"of"`, `"the"`, `"is"`, `"in"` and
`"to"`.

### Uniform Title

The [uniform title](https://en.wikipedia.org/wiki/Uniform_title) of a bookId is determined by capitalizing each word in 
the title.

If the first word on a bookId title is an article, this will be capitalized regardless.

### Uniform Name

The uniform name of a contributorId is determined by the capitalization of each word in the contributors name

### Contributor Uniqueness

A contributorId is uniquely identified by:

- uniform name
- type

### Book Uniqueness

A bookId is uniquely identified by:

- uniform title
- format
- contributors
- features (if any)
- language (if any)



### Cataloguing


Within the system, books and contributors should be searchable 

## ERD

The following entities are defined in the data model for this service.

![ERD](https://raw.githubusercontent.com/vinceynhz/library-service/master/doc/ERD.png)

### BOOK
This represents a single bookId in any given format: paperback, hardback, audiobook, ebook. Other attributes are provided
as well to use as metadata. 

Ordering is an important function of any library as it helps with indexing and organizing books. The cataloguing for books 
obeys standard library rules:

- bookId title is normalized to remove any non alphanumeric characters
- first title article is ignored (a, an, the)

Each bookId has a unique id issued by the database. Plan is to create a surrogate idempotent composite id that can be used
to uniquely identify a bookId from its title and other attributes.

### CONTRIBUTOR
This represents a person that contributes in the creation of a bookId. We provide a sha256 value derived out of the 
normalized name of the author to uniquely identify it.

Just as with books, the contributors also provide an cataloguing string for indexing and search. The cataloguing for 
contributors obeys also standard library rules:

- name is normalized to remove any non alphanumeric characters, honorifics and roman numerals
- first word is considered last in the order of words that integrate a name. For example:
  
  - `Neil deGrasse Tyson` is ordered as `degrasse tyson, neil`
  - `Mary Higgins Clark` is ordered as `higgins clark, mary`
  - `Sir Arthur Conan Doyle` is ordered as `conan doyle, arthur`

### BOOK_CONTRIBUTOR
This intermediate table is used to capture the many to many relationship existing between books and contributors:

- One author can have many contributors
- One author can have many books

And currently we use the database provided ids to match them.

### CONTRIBUTOR_ALIAS
This intermediate table is used to capture the one to many relationship existing between contributors and other possible
contributors.

- One author can have many aliases

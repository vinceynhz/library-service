drop table if exists CONTRIBUTOR;
drop table if exists BOOK;
drop table if exists BOOK_CONTRIBUTOR;
drop table if exists CONTRIBUTOR_ALIAS;

create table CONTRIBUTOR
(
	ID LONG primary key,
  SHA256 VARCHAR(64) unique not null,
	NAME VARCHAR(255) not null,
  CATALOGUING VARCHAR(255) not null
)
;

create table BOOK
(
	ID LONG primary key,
  SHA256 VARCHAR(64) unique not null,
	TITLE VARCHAR(255) not null,
  CATALOGUING VARCHAR(255) not null,
	ISBN VARCHAR(20),
	YEAR VARCHAR(4),
	LANGUAGE VARCHAR(5),
	FORMAT VARCHAR(12) not null,
	PAGES SMALLINT
)
;

create table BOOK_CONTRIBUTOR
(
	BOOK_ID LONG not null,
	CONTRIBUTOR_ID LONG not null,
	TYPE VARCHAR(12) not null,
	constraint BOOK_CONTRIBUTOR_CONTRIBUTOR_ID_FK
		foreign key (CONTRIBUTOR_ID) references CONTRIBUTOR,
	constraint BOOK_CONTRIBUTOR_BOOK_ID_FK
		foreign key (BOOK_ID) references BOOK
)
;

create table CONTRIBUTOR_ALIAS
(
	CONTRIBUTOR_ID LONG not null,
	ALSO_KNOWN_AS LONG not null,
	constraint CONTRIBUTOR_ALIAS_CONTRIBUTOR_ID_FK
	foreign key (CONTRIBUTOR_ID) references CONTRIBUTOR
	on update cascade,
	constraint CONTRIBUTOR_ALIAS_CONTRIBUTOR_ID_FK_2
	foreign key (ALSO_KNOWN_AS) references CONTRIBUTOR
	on update cascade
)
;

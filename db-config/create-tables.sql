CREATE DATABASE IF NOT EXISTS documents;
USE documents;


CREATE TABLE doc_user (
    user_id                                 INTEGER,
    username                                VARCHAR(255) NOT NULL,
    email                                   VARCHAR(255) NOT NULL UNIQUE,
    password_hash                           VARCHAR(255) NOT NULL,
    CONSTRAINT doc_user_pk                  PRIMARY KEY ( user_id )
);

CREATE TABLE parent_folder(
    parent_folder_id                        INTEGER,
    folder_path                             VARCHAR(5000),
    CONSTRAINT parent_folder_id_pk          PRIMARY KEY ( parent_folder_id )
);

CREATE TABLE folder (
    folder_id                               INTEGER,
    owner_id                                INTEGER,
    parent_folder_id                        INTEGER,
    created_by                              INTEGER,
    date_created                            TIMESTAMP NOT NULL,
    date_modified                           TIMESTAMP NOT NULL,
    folder_type                             VARCHAR(5) NOT NULL,
    folder_name                             VARCHAR(255) DEFAULT 'Untitled',
    CONSTRAINT folder_parent_folder_id_fk   FOREIGN KEY ( parent_folder_id ) REFERENCES parent_folder ( parent_folder_id ) ON DELETE CASCADE,
    CONSTRAINT folder_created_by_fk         FOREIGN KEY ( created_by ) REFERENCES doc_user ( user_id ) ON DELETE CASCADE,
    CONSTRAINT folder_owner_id_fk           FOREIGN KEY ( owner_id ) REFERENCES doc_user ( user_id ) ON DELETE CASCADE,
    CONSTRAINT folder_pk                    PRIMARY KEY ( folder_id ),
    CONSTRAINT unique_folder_in_folder      UNIQUE ( parent_folder_id,folder_name )
);

CREATE TABLE document (
    file_id                                 INTEGER,
    owner_id                                INTEGER,
    parent_folder_id                        INTEGER,
    created_by                              INTEGER,
    file_size                               INTEGER NOT NULL,
    date_created                            TIMESTAMP NOT NULL,
    date_modified                           TIMESTAMP NOT NULL,
    file_type                               VARCHAR(5) NOT NULL,
    file_name                               VARCHAR(255) DEFAULT 'Untitled',
    CONSTRAINT file_parent_folder_id_fk     FOREIGN KEY ( parent_folder_id ) REFERENCES parent_folder ( parent_folder_id ) ON DELETE CASCADE,
    CONSTRAINT file_created_by_fk           FOREIGN KEY ( created_by ) REFERENCES doc_user ( user_id ) ON DELETE CASCADE,
    CONSTRAINT file_owner_id_fk             FOREIGN KEY ( owner_id ) REFERENCES doc_user ( user_id ) ON DELETE CASCADE,
    CONSTRAINT document_pk                  PRIMARY KEY ( file_id ),
    CONSTRAINT unique_file_in_folder        UNIQUE ( parent_folder_id,file_name )
);

CREATE TABLE team (
    team_id                                 INTEGER,
    owner_id                                INTEGER,
    team_name                               VARCHAR(255) NOT NULL,
    team_description                        VARCHAR(255) NOT NULL,
    CONSTRAINT team_owner_id_fk             FOREIGN KEY ( owner_id ) REFERENCES doc_user ( user_id ),
    CONSTRAINT team_pk                      PRIMARY KEY ( team_id )
);

-- For the nxm relationship between users and groups
CREATE TABLE user_team (
    user_team_id                            INTEGER,
    team_id                                 INTEGER,
    user_id                                 INTEGER,
    CONSTRAINT user_team_id_pk              PRIMARY KEY ( user_team_id ),
    CONSTRAINT user_team_fk_team            FOREIGN KEY ( team_id ) REFERENCES team ( team_id ) ON DELETE CASCADE,
    CONSTRAINT user_team_fk_user            FOREIGN KEY ( user_id ) REFERENCES doc_user ( user_id ) ON DELETE CASCADE
);

CREATE TABLE shortcut (
    shortcut_id                             INTEGER,
    created_by                              INTEGER,
    destination_id                          INTEGER,
    parent_folder_id                        INTEGER,
    destination_path                        VARCHAR(255),
    CONSTRAINT shortcut_created_by_fk       FOREIGN KEY ( created_by ) REFERENCES doc_user ( user_id ),
    CONSTRAINT destination_id_fk            FOREIGN KEY ( destination_id ) REFERENCES document ( file_id ),
    CONSTRAINT shortcut_parent_folder       FOREIGN KEY ( parent_folder_id ) REFERENCES parent_folder(parent_folder_id),
    CONSTRAINT shortcut_pk                  PRIMARY KEY ( shortcut_id )
);

CREATE TABLE doc_comment (
    comment_id                              INTEGER,
    file_id                                 INTEGER,
    created_by                              INTEGER,
    content                                 VARCHAR(255) NOT NULL,
    time_posted                             TIMESTAMP NOT NULL,
    CONSTRAINT comment_file_id_fk           FOREIGN KEY ( file_id ) REFERENCES document ( file_id ) ON DELETE CASCADE,
    CONSTRAINT comment_created_by_fk        FOREIGN KEY ( created_by ) REFERENCES doc_user ( user_id ) ON DELETE CASCADE,
    CONSTRAINT doc_comment_pk               PRIMARY KEY ( comment_id )
);

CREATE TABLE permission
(
   permission_id                            INTEGER,
   file_id                                  INTEGER,
   folder_id                                INTEGER,
   user_id                                  INTEGER,
   team_id                                  INTEGER,

   -- 1 represents view-only, 2 represents comment, 3 represents edit perms
   abilities                                INTEGER CHECK(abilities BETWEEN 1 AND 3),

   CONSTRAINT file_id_fk                    FOREIGN KEY (file_id) REFERENCES document(file_id) ON DELETE CASCADE,
   CONSTRAINT folder_id_fk                  FOREIGN KEY (folder_id) REFERENCES folder(folder_id) ON DELETE CASCADE,
   CONSTRAINT user_id_fk                    FOREIGN KEY (user_id) REFERENCES doc_user(user_id) ON DELETE CASCADE,
   CONSTRAINT group_id_fk                   FOREIGN KEY (team_id) REFERENCES team(team_id) ON DELETE CASCADE,
   CONSTRAINT permission_pk                 PRIMARY KEY (permission_id)
);

CREATE TABLE doc_version (
    version_id                              INTEGER,
    file_id                                 INTEGER,
    owner_id                                INTEGER,
    version_number                          INTEGER NOT NULL,
    date_modified                           TIMESTAMP NOT NULL,
    content                                 BLOB,
    CONSTRAINT version_file_id_fk           FOREIGN KEY ( file_id ) REFERENCES document ( file_id ) ON DELETE CASCADE,
    CONSTRAINT version_owner_id_fk          FOREIGN KEY ( owner_id ) REFERENCES doc_user ( user_id ) ON DELETE CASCADE,
    CONSTRAINT version_pk                   PRIMARY KEY ( version_id )
);

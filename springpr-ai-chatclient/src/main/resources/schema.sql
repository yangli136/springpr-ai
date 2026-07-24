DROP
    TABLE
        IF EXISTS person;

CREATE
    TABLE
        IF NOT EXISTS person(
            id serial PRIMARY KEY,
            name text NOT NULL,
            email text NULL,
            description text NOT NULL
        );

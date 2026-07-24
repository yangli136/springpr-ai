INSERT
    INTO
        person(
            id,
            name,
            description
        )
    VALUES(
        97,
        'Rocky',
        'A farmer from Philadelphia.'
    ),
    (
        87,
        'Bailey',
        'A lawyer from Seattle.'
    ),
    (
        89,
        'Charlie',
        'A maintenance worker from Denver.'
    ),
    (
        67,
        'Cooper',
        'A actor from Hollywood.'
    ),
    (
        73,
        'Max',
        'A chemists from Dallas.'
    ),
    (
        3,
        'Buddy',
        'A pilot from Austin.'
    ),
    (
        93,
        'Duke',
        'An architect from Chicago.'
    ),
    (
        63,
        'Jasper',
        'A dentist from Jackson.'
    ),
    (
        69,
        'Toby',
        'An economist from Boston.'
    ),
    (
        101,
        'Nala',
        'A driver from Kansas City.'
    ),
    (
        61,
        'Penny',
        'A cook from Salt Lake City.'
    ),
    (
        1,
        'Bella',
        'A firefighter from Pittsburgh.'
    ),
    (
        91,
        'Willow',
        'A machinist from New York.'
    ),
    (
        5,
        'Daisy',
        'A nurse from Cincinnati.'
    ),
    (
        95,
        'Mia',
        'A wind energy engineer from Miami.'
    ),
    (
        71,
        'Molly',
        'A spa manager from Phoenix.'
    ),
    (
        65,
        'Ruby',
        'A radiologist from St. Louis'
    ),
    (
        45,
        'Prancer',
        'A wind energy engineer Indianapolis.'
    ) ON
    CONFLICT(id) DO UPDATE
    SET
        name = EXCLUDED.name,
        description = EXCLUDED.description;

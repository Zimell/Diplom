
-- ───────────── СТРУКТУРА ─────────────

CREATE TABLE IF NOT EXISTS organizers (
    id   BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS volunteers (
    id            BIGSERIAL PRIMARY KEY,
    fio           TEXT NOT NULL,
    birth_date    DATE,
    phone         TEXT,
    email         TEXT,
    registered_at DATE DEFAULT CURRENT_DATE
);

CREATE TABLE IF NOT EXISTS events (
    id             BIGSERIAL PRIMARY KEY,
    title          TEXT        NOT NULL,
    datetime       TIMESTAMP,
    location       TEXT,
    type           TEXT,
    organizer_id   BIGINT      NOT NULL REFERENCES organizers(id) ON DELETE RESTRICT
    -- убрали TEXT organizer → FK на таблицу organizers
);

CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL PRIMARY KEY,
    username      TEXT    UNIQUE NOT NULL,
    password_hash TEXT    NOT NULL,
    full_name     TEXT    NOT NULL,
    role          TEXT    NOT NULL DEFAULT 'user'
                          CHECK (role IN ('admin', 'curator', 'user')),
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    DATE    DEFAULT CURRENT_DATE
);

CREATE TABLE IF NOT EXISTS participations (
    id                   BIGSERIAL PRIMARY KEY,
    volunteer_id         BIGINT     NOT NULL REFERENCES volunteers(id) ON DELETE CASCADE,
    event_id             BIGINT     NOT NULL REFERENCES events(id)     ON DELETE CASCADE,
    hours_worked         NUMERIC(4,1) NOT NULL CHECK (hours_worked >= 0),
    confirmed            BOOLEAN    NOT NULL DEFAULT FALSE,
    confirmed_by_user_id BIGINT     REFERENCES users(id) ON DELETE SET NULL,
    -- было: confirmed_by TEXT → теперь FK → users(id)
    confirmed_at         TIMESTAMP  NULL
);

CREATE TABLE IF NOT EXISTS audit_log (
    id            BIGSERIAL PRIMARY KEY,
    occurred_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor_user_id BIGINT    REFERENCES users(id) ON DELETE SET NULL,
    -- было: actor TEXT → теперь FK → users(id)
    action        TEXT      NOT NULL,
    details       TEXT
);

-- Индексы
CREATE INDEX IF NOT EXISTS idx_part_volunteer ON participations(volunteer_id);
CREATE INDEX IF NOT EXISTS idx_part_event     ON participations(event_id);
CREATE INDEX IF NOT EXISTS idx_part_confirmed_by ON participations(confirmed_by_user_id);
CREATE INDEX IF NOT EXISTS idx_events_organizer  ON events(organizer_id);
CREATE INDEX IF NOT EXISTS idx_audit_actor        ON audit_log(actor_user_id);


-- ───────────── SEED DATA ─────────────
-- Каждый INSERT идемпотентен (ON CONFLICT DO NOTHING).

-- Организаторы (новая таблица, ранее хранились как TEXT в events)
INSERT INTO organizers (id, name) VALUES
    (1, 'Эко-центр «Чистый город»'),
    (2, 'Фонд «Дом друзей»'),
    (3, 'Гор. центр волонтёров'),
    (4, 'Студ. совет ВолГУ'),
    (5, 'Спорткомитет')
ON CONFLICT (id) DO NOTHING;

INSERT INTO volunteers (id, fio, birth_date, phone, email, registered_at) VALUES
    (1, 'Иванова Анна Сергеевна',        DATE '2002-04-12', '+7 (911) 100-12-34', 'ivanova@example.com',   DATE '2025-09-01'),
    (2, 'Петров Дмитрий Алексеевич',     DATE '1998-07-23', '+7 (911) 222-45-78', 'petrov@example.com',    DATE '2025-09-14'),
    (3, 'Смирнова Ольга Игоревна',       DATE '2000-11-05', '+7 (911) 333-90-01', 'smirnova@example.com',  DATE '2025-10-02'),
    (4, 'Кузнецов Артём Павлович',       DATE '2003-01-30', '+7 (911) 444-55-66', 'kuznetsov@example.com', DATE '2026-01-10'),
    (5, 'Михайлова Екатерина Олеговна',  DATE '1996-09-19', '+7 (911) 555-22-11', 'mikhailova@example.com',DATE '2026-02-20')
ON CONFLICT (id) DO NOTHING;

-- Учётные записи (вставляем ДО participations/audit_log, т.к. на них есть FK)
-- Логин / пароль (открытый) / роль:
--   admin    / admin123   / admin
--   semenov  / curator123 / curator  ← подтверждает участия (id=2)
--   vasileva / curator123 / curator
--   user     / user123    / user
--   petrov   / user123    / user
INSERT INTO users (id, username, password_hash, full_name, role) VALUES
    (1, 'admin',    '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'Администратор системы',     'admin'),
    (2, 'semenov',  '958fd30f64ac34d82850e17dd0d816130c61fd42c64c8e5782515108da1b2eff', 'Куратор Семёнов',           'curator'),
    (3, 'vasileva', '958fd30f64ac34d82850e17dd0d816130c61fd42c64c8e5782515108da1b2eff', 'Куратор Васильева',         'curator'),
    (4, 'user',     'e606e38b0d8c19b24cf0ee3808183162ea7cd63ff7912dbb22b5e803286b4446', 'Иванова Анна Сергеевна',    'user'),
    (5, 'petrov',   'e606e38b0d8c19b24cf0ee3808183162ea7cd63ff7912dbb22b5e803286b4446', 'Петров Дмитрий Алексеевич', 'user')
ON CONFLICT (id) DO NOTHING;

INSERT INTO events (id, title, datetime, location, type, organizer_id) VALUES
    (1, 'Уборка парка «Сосновый»',             TIMESTAMP '2026-01-18 10:00:00', 'Парк «Сосновый», вход с ул. Лесной', 'экология',     1),
    (2, 'Помощь приюту для животных',           TIMESTAMP '2026-02-08 12:00:00', 'Приют «Лапа», Промышленная 5',       'соцподдержка', 2),
    (3, 'Сбор гуманитарной помощи',             TIMESTAMP '2026-02-22 09:30:00', 'ДК «Юность», ул. Гагарина 7',        'соцподдержка', 3),
    (4, 'Высадка деревьев на набережной',       TIMESTAMP '2026-03-15 11:00:00', 'Набережная, причал №3',              'экология',     1),
    (5, 'Образовательный фестиваль «Знание+»',  TIMESTAMP '2026-04-12 10:00:00', 'Университет, корпус А',              'образование',  4),
    (6, 'Помощь в забеге «Майский кросс»',      TIMESTAMP '2026-05-25 08:00:00', 'Стадион «Динамо»',                   'спорт',        5)
ON CONFLICT (id) DO NOTHING;

-- confirmed_by_user_id=2 соответствует users.id=2 (semenov / Куратор Семёнов)
INSERT INTO participations (id, volunteer_id, event_id, hours_worked, confirmed, confirmed_by_user_id, confirmed_at) VALUES
    ( 1, 1, 1, 4.0, TRUE,  2, TIMESTAMP '2026-01-18 16:00:00'),
    ( 2, 2, 1, 3.5, TRUE,  2, TIMESTAMP '2026-01-18 16:00:00'),
    ( 3, 3, 1, 4.0, TRUE,  2, TIMESTAMP '2026-01-18 16:00:00'),
    ( 4, 1, 2, 5.0, TRUE,  2, TIMESTAMP '2026-02-08 18:30:00'),
    ( 5, 2, 2, 5.0, TRUE,  2, TIMESTAMP '2026-02-08 18:30:00'),
    ( 6, 4, 3, 6.0, TRUE,  2, TIMESTAMP '2026-02-22 16:00:00'),
    ( 7, 5, 3, 3.0, TRUE,  2, TIMESTAMP '2026-02-22 16:00:00'),
    ( 8, 1, 4, 4.5, TRUE,  2, TIMESTAMP '2026-03-15 16:30:00'),
    ( 9, 3, 4, 4.5, TRUE,  2, TIMESTAMP '2026-03-15 16:30:00'),
    (10, 4, 5, 6.0, FALSE, NULL, NULL),
    (11, 5, 5, 6.0, FALSE, NULL, NULL),
    (12, 2, 6, 4.0, FALSE, NULL, NULL),
    (13, 3, 6, 4.0, FALSE, NULL, NULL)
ON CONFLICT (id) DO NOTHING;

-- actor_user_id=2 соответствует users.id=2 (semenov / Куратор Семёнов)
INSERT INTO audit_log (id, actor_user_id, action, details) VALUES
    (1, 2, 'participation.confirm', 'Иванова Анна / Уборка парка «Сосновый» (4ч)'),
    (2, 2, 'participation.confirm', 'Петров Дмитрий / Уборка парка «Сосновый» (3.5ч)'),
    (3, 2, 'event.create',          'Образовательный фестиваль «Знание+»')
ON CONFLICT (id) DO NOTHING;

-- Сдвиг sequence
SELECT setval(pg_get_serial_sequence('organizers',     'id'), GREATEST(COALESCE((SELECT MAX(id) FROM organizers),     1), 1));
SELECT setval(pg_get_serial_sequence('volunteers',     'id'), GREATEST(COALESCE((SELECT MAX(id) FROM volunteers),     1), 1));
SELECT setval(pg_get_serial_sequence('events',         'id'), GREATEST(COALESCE((SELECT MAX(id) FROM events),         1), 1));
SELECT setval(pg_get_serial_sequence('users',          'id'), GREATEST(COALESCE((SELECT MAX(id) FROM users),          1), 1));
SELECT setval(pg_get_serial_sequence('participations', 'id'), GREATEST(COALESCE((SELECT MAX(id) FROM participations), 1), 1));
SELECT setval(pg_get_serial_sequence('audit_log',      'id'), GREATEST(COALESCE((SELECT MAX(id) FROM audit_log),      1), 1));
-- ==================== DOCTORS ====================
INSERT INTO doctors (name, specialization, experience_years, description) VALUES
    ('Dr. Emily Smith', 'Orthodontist', 15, 'Specializes in Invisalign and traditional braces. Certified by the American Board of Orthodontics.'),
    ('Dr. Michael Johnson', 'Pediatric Dentist', 8, 'Gentle approach for children, specializes in early cavity prevention and dental education.'),
    ('Dr. Sarah Williams', 'Endodontist', 12, 'Root canal specialist with advanced training in microsurgical endodontics.'),
    ('Dr. David Brown', 'Periodontist', 20, 'Expert in gum disease treatment and dental implants, over 2000 successful implant procedures.'),
    ('Dr. Laura Davis', 'General Dentist', 6, 'Focuses on preventive care, cleanings, and cosmetic dentistry including teeth whitening.'),
    ('Dr. Robert Wilson', 'Oral Surgeon', 18, 'Specializes in tooth extractions, wisdom teeth removal, and dental implant surgery. Over 3000 successful extractions performed.');

-- ==================== SLOTS ====================
-- Working hours: Mon-Fri 8:00-18:00, Sat 9:00-14:00, Sun — closed
-- Each doctor works half-day shifts:
--   Morning doctors (id=1,2,4): Mon-Fri 8:00-12:00, Sat 9:00-11:00
--   Afternoon doctors (id=3,5,6): Mon-Fri 13:00-17:00, Sat 12:00-14:00
--
-- Availability gradient: day 1 ~20% free, increasing to day 7 ~80% free
-- Formula: slot is available when MOD(...) >= threshold
--   threshold for day d.x: 5 (day1=~20%), 4 (day2=~33%), 3 (day3=~50%), 2 (day4=~67%), 1 (day5-7=~80%)
--   Implemented as: is_available = MOD(...) >= GREATEST(1, 6 - d.x)

-- Dr. Emily Smith (id=1) — MORNING, weekdays 8:00-12:00
INSERT INTO slots (doctor_id, slot_date, slot_time, is_available)
SELECT 1, DATEADD('DAY', d.x, CURRENT_DATE), t.slot_time,
       MOD(d.x * 3 + EXTRACT(HOUR FROM t.slot_time) + 1, 6) >= GREATEST(1, 6 - d.x)
FROM SYSTEM_RANGE(1, 7) d
CROSS JOIN (
    SELECT TIME '08:00:00' AS slot_time UNION ALL
    SELECT TIME '09:00:00' UNION ALL
    SELECT TIME '10:00:00' UNION ALL
    SELECT TIME '11:00:00' UNION ALL
    SELECT TIME '12:00:00'
) t
WHERE EXTRACT(DAY_OF_WEEK FROM DATEADD('DAY', d.x, CURRENT_DATE)) BETWEEN 1 AND 5;

-- Dr. Emily Smith (id=1) — MORNING, Saturday 9:00-11:00
INSERT INTO slots (doctor_id, slot_date, slot_time, is_available)
SELECT 1, DATEADD('DAY', d.x, CURRENT_DATE), t.slot_time,
       MOD(d.x * 3 + EXTRACT(HOUR FROM t.slot_time) + 1, 6) >= GREATEST(1, 6 - d.x)
FROM SYSTEM_RANGE(1, 7) d
CROSS JOIN (
    SELECT TIME '09:00:00' AS slot_time UNION ALL
    SELECT TIME '10:00:00' UNION ALL
    SELECT TIME '11:00:00'
) t
WHERE EXTRACT(DAY_OF_WEEK FROM DATEADD('DAY', d.x, CURRENT_DATE)) = 6;

-- Dr. Michael Johnson (id=2) — MORNING, weekdays 8:00-12:00
INSERT INTO slots (doctor_id, slot_date, slot_time, is_available)
SELECT 2, DATEADD('DAY', d.x, CURRENT_DATE), t.slot_time,
       MOD(d.x * 2 + EXTRACT(HOUR FROM t.slot_time) + 3, 6) >= GREATEST(1, 6 - d.x)
FROM SYSTEM_RANGE(1, 7) d
CROSS JOIN (
    SELECT TIME '08:00:00' AS slot_time UNION ALL
    SELECT TIME '09:00:00' UNION ALL
    SELECT TIME '10:00:00' UNION ALL
    SELECT TIME '11:00:00' UNION ALL
    SELECT TIME '12:00:00'
) t
WHERE EXTRACT(DAY_OF_WEEK FROM DATEADD('DAY', d.x, CURRENT_DATE)) BETWEEN 1 AND 5;

-- Dr. Michael Johnson (id=2) — MORNING, Saturday 9:00-11:00
INSERT INTO slots (doctor_id, slot_date, slot_time, is_available)
SELECT 2, DATEADD('DAY', d.x, CURRENT_DATE), t.slot_time,
       MOD(d.x * 2 + EXTRACT(HOUR FROM t.slot_time) + 3, 6) >= GREATEST(1, 6 - d.x)
FROM SYSTEM_RANGE(1, 7) d
CROSS JOIN (
    SELECT TIME '09:00:00' AS slot_time UNION ALL
    SELECT TIME '10:00:00' UNION ALL
    SELECT TIME '11:00:00'
) t
WHERE EXTRACT(DAY_OF_WEEK FROM DATEADD('DAY', d.x, CURRENT_DATE)) = 6;

-- Dr. Sarah Williams (id=3) — AFTERNOON, weekdays 13:00-17:00
INSERT INTO slots (doctor_id, slot_date, slot_time, is_available)
SELECT 3, DATEADD('DAY', d.x, CURRENT_DATE), t.slot_time,
       MOD(d.x * 4 + EXTRACT(HOUR FROM t.slot_time) + 2, 6) >= GREATEST(1, 6 - d.x)
FROM SYSTEM_RANGE(1, 7) d
CROSS JOIN (
    SELECT TIME '13:00:00' AS slot_time UNION ALL
    SELECT TIME '14:00:00' UNION ALL
    SELECT TIME '15:00:00' UNION ALL
    SELECT TIME '16:00:00' UNION ALL
    SELECT TIME '17:00:00'
) t
WHERE EXTRACT(DAY_OF_WEEK FROM DATEADD('DAY', d.x, CURRENT_DATE)) BETWEEN 1 AND 5;

-- Dr. Sarah Williams (id=3) — AFTERNOON, Saturday 12:00-14:00
INSERT INTO slots (doctor_id, slot_date, slot_time, is_available)
SELECT 3, DATEADD('DAY', d.x, CURRENT_DATE), t.slot_time,
       MOD(d.x * 4 + EXTRACT(HOUR FROM t.slot_time) + 2, 6) >= GREATEST(1, 6 - d.x)
FROM SYSTEM_RANGE(1, 7) d
CROSS JOIN (
    SELECT TIME '12:00:00' AS slot_time UNION ALL
    SELECT TIME '13:00:00'
) t
WHERE EXTRACT(DAY_OF_WEEK FROM DATEADD('DAY', d.x, CURRENT_DATE)) = 6;

-- Dr. David Brown (id=4) — MORNING, weekdays 8:00-12:00
INSERT INTO slots (doctor_id, slot_date, slot_time, is_available)
SELECT 4, DATEADD('DAY', d.x, CURRENT_DATE), t.slot_time,
       MOD(d.x * 3 + EXTRACT(HOUR FROM t.slot_time) + 5, 6) >= GREATEST(1, 6 - d.x)
FROM SYSTEM_RANGE(1, 7) d
CROSS JOIN (
    SELECT TIME '08:00:00' AS slot_time UNION ALL
    SELECT TIME '09:00:00' UNION ALL
    SELECT TIME '10:00:00' UNION ALL
    SELECT TIME '11:00:00' UNION ALL
    SELECT TIME '12:00:00'
) t
WHERE EXTRACT(DAY_OF_WEEK FROM DATEADD('DAY', d.x, CURRENT_DATE)) BETWEEN 1 AND 5;

-- Dr. David Brown (id=4) — MORNING, Saturday 9:00-11:00
INSERT INTO slots (doctor_id, slot_date, slot_time, is_available)
SELECT 4, DATEADD('DAY', d.x, CURRENT_DATE), t.slot_time,
       MOD(d.x * 3 + EXTRACT(HOUR FROM t.slot_time) + 5, 6) >= GREATEST(1, 6 - d.x)
FROM SYSTEM_RANGE(1, 7) d
CROSS JOIN (
    SELECT TIME '09:00:00' AS slot_time UNION ALL
    SELECT TIME '10:00:00' UNION ALL
    SELECT TIME '11:00:00'
) t
WHERE EXTRACT(DAY_OF_WEEK FROM DATEADD('DAY', d.x, CURRENT_DATE)) = 6;

-- Dr. Laura Davis (id=5) — AFTERNOON, weekdays 13:00-17:00
INSERT INTO slots (doctor_id, slot_date, slot_time, is_available)
SELECT 5, DATEADD('DAY', d.x, CURRENT_DATE), t.slot_time,
       MOD(d.x * 2 + EXTRACT(HOUR FROM t.slot_time) + 4, 6) >= GREATEST(1, 6 - d.x)
FROM SYSTEM_RANGE(1, 7) d
CROSS JOIN (
    SELECT TIME '13:00:00' AS slot_time UNION ALL
    SELECT TIME '14:00:00' UNION ALL
    SELECT TIME '15:00:00' UNION ALL
    SELECT TIME '16:00:00' UNION ALL
    SELECT TIME '17:00:00'
) t
WHERE EXTRACT(DAY_OF_WEEK FROM DATEADD('DAY', d.x, CURRENT_DATE)) BETWEEN 1 AND 5;

-- Dr. Laura Davis (id=5) — AFTERNOON, Saturday 12:00-14:00
INSERT INTO slots (doctor_id, slot_date, slot_time, is_available)
SELECT 5, DATEADD('DAY', d.x, CURRENT_DATE), t.slot_time,
       MOD(d.x * 2 + EXTRACT(HOUR FROM t.slot_time) + 4, 6) >= GREATEST(1, 6 - d.x)
FROM SYSTEM_RANGE(1, 7) d
CROSS JOIN (
    SELECT TIME '12:00:00' AS slot_time UNION ALL
    SELECT TIME '13:00:00'
) t
WHERE EXTRACT(DAY_OF_WEEK FROM DATEADD('DAY', d.x, CURRENT_DATE)) = 6;

-- Dr. Robert Wilson (id=6) — AFTERNOON, weekdays 13:00-17:00
INSERT INTO slots (doctor_id, slot_date, slot_time, is_available)
SELECT 6, DATEADD('DAY', d.x, CURRENT_DATE), t.slot_time,
       MOD(d.x * 5 + EXTRACT(HOUR FROM t.slot_time), 6) >= GREATEST(1, 6 - d.x)
FROM SYSTEM_RANGE(1, 7) d
CROSS JOIN (
    SELECT TIME '13:00:00' AS slot_time UNION ALL
    SELECT TIME '14:00:00' UNION ALL
    SELECT TIME '15:00:00' UNION ALL
    SELECT TIME '16:00:00' UNION ALL
    SELECT TIME '17:00:00'
) t
WHERE EXTRACT(DAY_OF_WEEK FROM DATEADD('DAY', d.x, CURRENT_DATE)) BETWEEN 1 AND 5;

-- Dr. Robert Wilson (id=6) — AFTERNOON, Saturday 12:00-14:00
INSERT INTO slots (doctor_id, slot_date, slot_time, is_available)
SELECT 6, DATEADD('DAY', d.x, CURRENT_DATE), t.slot_time,
       MOD(d.x * 5 + EXTRACT(HOUR FROM t.slot_time), 6) >= GREATEST(1, 6 - d.x)
FROM SYSTEM_RANGE(1, 7) d
CROSS JOIN (
    SELECT TIME '12:00:00' AS slot_time UNION ALL
    SELECT TIME '13:00:00'
) t
WHERE EXTRACT(DAY_OF_WEEK FROM DATEADD('DAY', d.x, CURRENT_DATE)) = 6;

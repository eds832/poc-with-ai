-- ==================== DOCTORS (обязательно первыми!) ====================
INSERT INTO doctors (name, specialization, experience_years, description) VALUES
                                                                              ('Dr. Emily Smith', 'Orthodontist', 15, 'Specializes in Invisalign and traditional braces. Certified by the American Board of Orthodontics.'),
                                                                              ('Dr. Michael Johnson', 'Pediatric Dentist', 8, 'Gentle approach for children, specializes in early cavity prevention and dental education.'),
                                                                              ('Dr. Sarah Williams', 'Endodontist', 12, 'Root canal specialist with advanced training in microsurgical endodontics.'),
                                                                              ('Dr. David Brown', 'Periodontist', 20, 'Expert in gum disease treatment and dental implants, over 2000 successful implant procedures.'),
                                                                              ('Dr. Laura Davis', 'General Dentist', 6, 'Focuses on preventive care, cleanings, and cosmetic dentistry including teeth whitening.');

-- ==================== SLOTS ====================

-- Dr. Emily Smith (id=1)
INSERT INTO slots (doctor_id, slot_date, slot_time, is_available) VALUES
                                                                      (1, DATEADD('DAY', 1, CURRENT_DATE), '09:00:00', TRUE),
                                                                      (1, DATEADD('DAY', 1, CURRENT_DATE), '10:00:00', FALSE),
                                                                      (1, DATEADD('DAY', 1, CURRENT_DATE), '11:00:00', TRUE),
                                                                      (1, DATEADD('DAY', 2, CURRENT_DATE), '09:00:00', TRUE),
                                                                      (1, DATEADD('DAY', 2, CURRENT_DATE), '14:00:00', TRUE);

-- Dr. Michael Johnson (id=2)
INSERT INTO slots (doctor_id, slot_date, slot_time, is_available) VALUES
                                                                      (2, DATEADD('DAY', 1, CURRENT_DATE), '09:00:00', TRUE),
                                                                      (2, DATEADD('DAY', 1, CURRENT_DATE), '13:00:00', TRUE),
                                                                      (2, DATEADD('DAY', 2, CURRENT_DATE), '10:00:00', FALSE),
                                                                      (2, DATEADD('DAY', 3, CURRENT_DATE), '09:00:00', TRUE);

-- Dr. Sarah Williams (id=3)
INSERT INTO slots (doctor_id, slot_date, slot_time, is_available) VALUES
                                                                      (3, DATEADD('DAY', 1, CURRENT_DATE), '15:00:00', TRUE),
                                                                      (3, DATEADD('DAY', 2, CURRENT_DATE), '11:00:00', TRUE),
                                                                      (3, DATEADD('DAY', 2, CURRENT_DATE), '15:00:00', FALSE),
                                                                      (3, DATEADD('DAY', 4, CURRENT_DATE), '09:00:00', TRUE);

-- Dr. David Brown (id=4)
INSERT INTO slots (doctor_id, slot_date, slot_time, is_available) VALUES
                                                                      (4, DATEADD('DAY', 1, CURRENT_DATE), '08:00:00', TRUE),
                                                                      (4, DATEADD('DAY', 3, CURRENT_DATE), '13:00:00', TRUE),
                                                                      (4, DATEADD('DAY', 3, CURRENT_DATE), '14:00:00', FALSE),
                                                                      (4, DATEADD('DAY', 4, CURRENT_DATE), '10:00:00', TRUE);

-- Dr. Laura Davis (id=5)
INSERT INTO slots (doctor_id, slot_date, slot_time, is_available) VALUES
                                                                      (5, DATEADD('DAY', 1, CURRENT_DATE), '09:00:00', TRUE),
                                                                      (5, DATEADD('DAY', 1, CURRENT_DATE), '12:00:00', TRUE),
                                                                      (5, DATEADD('DAY', 2, CURRENT_DATE), '09:00:00', TRUE),
                                                                      (5, DATEADD('DAY', 4, CURRENT_DATE), '13:00:00', FALSE);

-- Dr. Robert Wilson (id=6) - Oral Surgeon
INSERT INTO doctors (name, specialization, experience_years, description) VALUES
    ('Dr. Robert Wilson', 'Oral Surgeon', 18, 'Specializes in tooth extractions, wisdom teeth removal, and dental implant surgery. Over 3000 successful extractions performed.');

INSERT INTO slots (doctor_id, slot_date, slot_time, is_available) VALUES
                                                                      (6, DATEADD('DAY', 1, CURRENT_DATE), '08:00:00', TRUE),
                                                                      (6, DATEADD('DAY', 1, CURRENT_DATE), '10:00:00', TRUE),
                                                                      (6, DATEADD('DAY', 2, CURRENT_DATE), '09:00:00', FALSE),
                                                                      (6, DATEADD('DAY', 3, CURRENT_DATE), '11:00:00', TRUE),
                                                                      (6, DATEADD('DAY', 4, CURRENT_DATE), '14:00:00', TRUE);
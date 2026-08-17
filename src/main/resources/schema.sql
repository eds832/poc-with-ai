SET IGNORECASE TRUE;

DROP TABLE IF EXISTS slots;
DROP TABLE IF EXISTS doctors;

CREATE TABLE doctors (
                         id INT PRIMARY KEY AUTO_INCREMENT,
                         name VARCHAR(100) NOT NULL,
                         specialization VARCHAR(100) NOT NULL,
                         experience_years INT NOT NULL,
                         description VARCHAR(500)
);

CREATE TABLE slots (
                       id INT PRIMARY KEY AUTO_INCREMENT,
                       doctor_id INT NOT NULL,
                       slot_date DATE NOT NULL,
                       slot_time TIME NOT NULL,
                       is_available BOOLEAN NOT NULL DEFAULT TRUE,
                       FOREIGN KEY (doctor_id) REFERENCES doctors(id)
);
CREATE DATABASE blood_bank;
USE blood_bank;

CREATE TABLE donors (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    age INT NOT NULL,
    blood_group VARCHAR(5) NOT NULL,
    city VARCHAR(50) NOT NULL,
    phone VARCHAR(15) NOT NULL,
    last_donated DATE
);

CREATE TABLE requests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    patient_name VARCHAR(50) NOT NULL,
    blood_group VARCHAR(5) NOT NULL,
    city VARCHAR(50) NOT NULL,
    contact VARCHAR(15) NOT NULL,
    status VARCHAR(15) DEFAULT 'Open'
);

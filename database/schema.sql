-- =========================================================
-- SKILLSYNC DATABASE
-- =========================================================

CREATE DATABASE IF NOT EXISTS skillsync;

USE skillsync;


-- =========================================================
-- RESET TABLES
-- =========================================================
-- Child tables first because of foreign keys

DROP TABLE IF EXISTS training_sessions;
DROP TABLE IF EXISTS training_requests;
DROP TABLE IF EXISTS assessments;
DROP TABLE IF EXISTS manager_employees;
DROP TABLE IF EXISTS employee_job_roles;
DROP TABLE IF EXISTS required_skills;
DROP TABLE IF EXISTS employee_skills;
DROP TABLE IF EXISTS job_roles;
DROP TABLE IF EXISTS skills;
DROP TABLE IF EXISTS users;


-- =========================================================
-- 1. USERS
-- =========================================================

CREATE TABLE users (

    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    name VARCHAR(100) NOT NULL,

    email VARCHAR(150) NOT NULL UNIQUE,

    password VARCHAR(255) NOT NULL,

    role ENUM(
        'EMPLOYEE',
        'SME',
        'MANAGER',
        'ADMIN'
    ) NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP

) ENGINE=InnoDB;


-- =========================================================
-- 2. SKILLS
-- =========================================================

CREATE TABLE skills (

    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    name VARCHAR(100) NOT NULL UNIQUE,

    category VARCHAR(100),

    description TEXT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP

) ENGINE=InnoDB;


-- =========================================================
-- 3. JOB ROLES
-- =========================================================

CREATE TABLE job_roles (

    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    name VARCHAR(150) NOT NULL UNIQUE,

    description TEXT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP

) ENGINE=InnoDB;


-- =========================================================
-- 4. EMPLOYEE SKILLS
-- =========================================================

CREATE TABLE employee_skills (

    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    employee_id BIGINT NOT NULL,

    skill_id BIGINT NOT NULL,

    proficiency INT NOT NULL,

    years_of_experience DECIMAL(4,1) DEFAULT 0,

    last_validated_at TIMESTAMP NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_proficiency
        CHECK (proficiency BETWEEN 0 AND 10),

    CONSTRAINT fk_employee_skill_employee
        FOREIGN KEY (employee_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_employee_skill_skill
        FOREIGN KEY (skill_id)
        REFERENCES skills(id)
        ON DELETE CASCADE,

    CONSTRAINT unique_employee_skill
        UNIQUE (employee_id, skill_id)

) ENGINE=InnoDB;


-- =========================================================
-- 5. EMPLOYEE JOB ROLES
-- =========================================================
-- Which role is the employee currently associated with?

CREATE TABLE employee_job_roles (

    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    employee_id BIGINT NOT NULL,

    job_role_id BIGINT NOT NULL,

    is_current BOOLEAN DEFAULT TRUE,

    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_employee_job_role_employee
        FOREIGN KEY (employee_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_employee_job_role_role
        FOREIGN KEY (job_role_id)
        REFERENCES job_roles(id)
        ON DELETE CASCADE,

    CONSTRAINT unique_employee_role
        UNIQUE (employee_id, job_role_id)

) ENGINE=InnoDB;


-- =========================================================
-- 6. REQUIRED SKILLS
-- =========================================================
-- Skills required for a particular job role

CREATE TABLE required_skills (

    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    job_role_id BIGINT NOT NULL,

    skill_id BIGINT NOT NULL,

    required_level INT NOT NULL,

    importance ENUM(
        'LOW',
        'MEDIUM',
        'HIGH'
    ) DEFAULT 'MEDIUM',

    CONSTRAINT chk_required_level
        CHECK (required_level BETWEEN 1 AND 10),

    CONSTRAINT fk_required_role
        FOREIGN KEY (job_role_id)
        REFERENCES job_roles(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_required_skill
        FOREIGN KEY (skill_id)
        REFERENCES skills(id)
        ON DELETE CASCADE,

    CONSTRAINT unique_role_skill
        UNIQUE (job_role_id, skill_id)

) ENGINE=InnoDB;


-- =========================================================
-- 7. MANAGER EMPLOYEES
-- =========================================================
-- Defines which employees belong to which manager

CREATE TABLE manager_employees (

    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    manager_id BIGINT NOT NULL,

    employee_id BIGINT NOT NULL,

    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_manager
        FOREIGN KEY (manager_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_managed_employee
        FOREIGN KEY (employee_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT unique_manager_employee
        UNIQUE (manager_id, employee_id)

) ENGINE=InnoDB;


-- =========================================================
-- 8. ASSESSMENTS
-- =========================================================

CREATE TABLE assessments (

    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    employee_id BIGINT NOT NULL,

    skill_id BIGINT NOT NULL,

    score INT NOT NULL,

    assessment_type ENUM(
        'SELF',
        'SME_VALIDATED',
        'MANAGER_VALIDATED'
    ) DEFAULT 'SELF',

    assessed_by BIGINT NULL,

    feedback TEXT,

    assessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_assessment_score
        CHECK (score BETWEEN 0 AND 10),

    CONSTRAINT fk_assessment_employee
        FOREIGN KEY (employee_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_assessment_skill
        FOREIGN KEY (skill_id)
        REFERENCES skills(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_assessment_assessor
        FOREIGN KEY (assessed_by)
        REFERENCES users(id)
        ON DELETE SET NULL

) ENGINE=InnoDB;


-- =========================================================
-- 9. TRAINING REQUESTS
-- =========================================================

CREATE TABLE training_requests (

    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    employee_id BIGINT NOT NULL,

    skill_id BIGINT NOT NULL,

    requested_level INT,

    reason TEXT,

    priority ENUM(
        'LOW',
        'MEDIUM',
        'HIGH'
    ) DEFAULT 'MEDIUM',

    status ENUM(
        'OPEN',
        'CLAIMED',
        'IN_PROGRESS',
        'COMPLETED',
        'CANCELLED'
    ) DEFAULT 'OPEN',

    claimed_by BIGINT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_requested_level
        CHECK (
            requested_level IS NULL
            OR requested_level BETWEEN 1 AND 10
        ),

    CONSTRAINT fk_training_employee
        FOREIGN KEY (employee_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_training_skill
        FOREIGN KEY (skill_id)
        REFERENCES skills(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_training_sme
        FOREIGN KEY (claimed_by)
        REFERENCES users(id)
        ON DELETE SET NULL

) ENGINE=InnoDB;


-- =========================================================
-- 10. TRAINING SESSIONS
-- =========================================================

CREATE TABLE training_sessions (

    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    training_request_id BIGINT NOT NULL,

    sme_id BIGINT NOT NULL,

    session_title VARCHAR(200) NOT NULL,

    scheduled_at DATETIME,

    completed_at DATETIME,

    employee_score_before INT,

    employee_score_after INT,

    feedback TEXT,

    status ENUM(
        'SCHEDULED',
        'COMPLETED',
        'CANCELLED'
    ) DEFAULT 'SCHEDULED',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_before_score
        CHECK (
            employee_score_before IS NULL
            OR employee_score_before BETWEEN 0 AND 10
        ),

    CONSTRAINT chk_after_score
        CHECK (
            employee_score_after IS NULL
            OR employee_score_after BETWEEN 0 AND 10
        ),

    CONSTRAINT fk_session_request
        FOREIGN KEY (training_request_id)
        REFERENCES training_requests(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_session_sme
        FOREIGN KEY (sme_id)
        REFERENCES users(id)
        ON DELETE CASCADE

) ENGINE=InnoDB;


-- =========================================================
-- SAMPLE DATA
-- =========================================================


-- =========================================================
-- USERS
-- =========================================================

INSERT INTO users
(name, email, password, role)
VALUES

(
    'Rahul Sharma',
    'rahul@skillsync.com',
    '$2a$10$vegXhLE3ulqpQoqduEyOFOS3zWX3yBA91pYsD/V5mKLY/o38MR8w6',
    'EMPLOYEE'
),

(
    'Amit Verma',
    'amit@skillsync.com',
    '$2a$10$vegXhLE3ulqpQoqduEyOFOS3zWX3yBA91pYsD/V5mKLY/o38MR8w6',
    'SME'
),

(
    'Priya Singh',
    'priya@skillsync.com',
    '$2a$10$vegXhLE3ulqpQoqduEyOFOS3zWX3yBA91pYsD/V5mKLY/o38MR8w6',
    'MANAGER'
),

(
    'Admin User',
    'admin@skillsync.com',
    '$2a$10$vegXhLE3ulqpQoqduEyOFOS3zWX3yBA91pYsD/V5mKLY/o38MR8w6',
    'ADMIN'
);


-- =========================================================
-- SKILLS
-- =========================================================

INSERT INTO skills
(name, category, description)
VALUES

(
    'Java',
    'Programming',
    'Java programming language'
),

(
    'Spring Boot',
    'Backend',
    'Java backend framework'
),

(
    'AWS',
    'Cloud',
    'Amazon Web Services'
),

(
    'Docker',
    'DevOps',
    'Containerization technology'
),

(
    'Kafka',
    'Backend',
    'Distributed event streaming platform'
),

(
    'SQL',
    'Database',
    'Structured Query Language'
),

(
    'Python',
    'Programming',
    'Python programming language'
),

(
    'Machine Learning',
    'AI',
    'Machine learning concepts'
);


-- =========================================================
-- JOB ROLES
-- =========================================================

INSERT INTO job_roles
(name, description)
VALUES

(
    'Backend Developer',
    'Develops scalable backend applications and APIs'
),

(
    'Cloud Engineer',
    'Designs and manages cloud infrastructure'
),

(
    'Data Engineer',
    'Builds data pipelines and data platforms'
),

(
    'ML Engineer',
    'Builds and deploys machine learning systems'
);


-- =========================================================
-- EMPLOYEE SKILLS
-- =========================================================
-- Rahul's current skills

INSERT INTO employee_skills
(
    employee_id,
    skill_id,
    proficiency,
    years_of_experience
)
VALUES

(
    1,
    1,
    8,
    2.0
),

(
    1,
    2,
    7,
    1.5
),

(
    1,
    3,
    4,
    0.5
),

(
    1,
    4,
    3,
    0.5
),

(
    1,
    5,
    2,
    0.2
);


-- =========================================================
-- EMPLOYEE JOB ROLE
-- =========================================================
-- Rahul is currently a Backend Developer

INSERT INTO employee_job_roles
(
    employee_id,
    job_role_id,
    is_current
)
VALUES

(
    1,
    1,
    TRUE
);


-- =========================================================
-- REQUIRED SKILLS
-- =========================================================
-- Skills required for Backend Developer

INSERT INTO required_skills
(
    job_role_id,
    skill_id,
    required_level,
    importance
)
VALUES

(
    1,
    1,
    8,
    'HIGH'
),

(
    1,
    2,
    8,
    'HIGH'
),

(
    1,
    3,
    7,
    'HIGH'
),

(
    1,
    4,
    6,
    'MEDIUM'
),

(
    1,
    5,
    6,
    'MEDIUM'
);


-- =========================================================
-- MANAGER → EMPLOYEE
-- =========================================================
-- Priya manages Rahul

INSERT INTO manager_employees
(
    manager_id,
    employee_id
)
VALUES

(
    3,
    1
);


-- =========================================================
-- ASSESSMENTS
-- =========================================================

INSERT INTO assessments
(
    employee_id,
    skill_id,
    score,
    assessment_type,
    assessed_by,
    feedback
)
VALUES

(
    1,
    1,
    8,
    'SME_VALIDATED',
    2,
    'Strong Java fundamentals'
),

(
    1,
    2,
    7,
    'SELF',
    NULL,
    'Comfortable with Spring Boot'
),

(
    1,
    3,
    4,
    'SELF',
    NULL,
    'Basic AWS knowledge'
),

(
    1,
    4,
    3,
    'SELF',
    NULL,
    'Beginner level'
),

(
    1,
    5,
    2,
    'SELF',
    NULL,
    'Limited Kafka exposure'
);


-- =========================================================
-- TRAINING REQUEST
-- =========================================================

INSERT INTO training_requests
(
    employee_id,
    skill_id,
    requested_level,
    reason,
    priority
)
VALUES

(
    1,
    5,
    6,
    'Need Kafka skills for backend event-driven projects',
    'HIGH'
);


-- =========================================================
-- END
-- =========================================================
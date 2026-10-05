CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('EMPLOYEE', 'SME', 'MANAGER', 'ADMIN') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    job_title VARCHAR(120),
    department VARCHAR(120),
    career_goal VARCHAR(500),
    bio VARCHAR(1000)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS skills (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(100),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS job_roles (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS employee_skills (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    proficiency INT NOT NULL,
    years_of_experience DECIMAL(4,1) DEFAULT 0,
    last_validated_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_proficiency CHECK (proficiency BETWEEN 0 AND 10),
    CONSTRAINT fk_employee_skill_employee FOREIGN KEY (employee_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_employee_skill_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE,
    CONSTRAINT unique_employee_skill UNIQUE (employee_id, skill_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS required_skills (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    job_role_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    required_level INT NOT NULL,
    importance ENUM('LOW', 'MEDIUM', 'HIGH') DEFAULT 'MEDIUM',
    CONSTRAINT chk_required_level CHECK (required_level BETWEEN 1 AND 10),
    CONSTRAINT fk_required_role FOREIGN KEY (job_role_id) REFERENCES job_roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_required_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE,
    CONSTRAINT unique_role_skill UNIQUE (job_role_id, skill_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS manager_employees (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    manager_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_manager FOREIGN KEY (manager_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_managed_employee FOREIGN KEY (employee_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT unique_manager_employee UNIQUE (manager_id, employee_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS assessments (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    score INT NOT NULL,
    assessment_type ENUM('SELF', 'SME_VALIDATED', 'MANAGER_VALIDATED') DEFAULT 'SELF',
    assessed_by BIGINT NULL,
    feedback TEXT,
    assessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_assessment_score CHECK (score BETWEEN 0 AND 10),
    CONSTRAINT fk_assessment_employee FOREIGN KEY (employee_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_assessment_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE,
    CONSTRAINT fk_assessment_assessor FOREIGN KEY (assessed_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS training_requests (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    requested_level INT,
    reason TEXT,
    priority ENUM('LOW', 'MEDIUM', 'HIGH') DEFAULT 'MEDIUM',
    status ENUM('OPEN', 'CLAIMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') DEFAULT 'OPEN',
    claimed_by BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_requested_level CHECK (requested_level IS NULL OR requested_level BETWEEN 1 AND 10),
    CONSTRAINT fk_training_employee FOREIGN KEY (employee_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_training_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE,
    CONSTRAINT fk_training_sme FOREIGN KEY (claimed_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS training_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    training_request_id BIGINT NOT NULL,
    sme_id BIGINT NOT NULL,
    session_title VARCHAR(200) NOT NULL,
    scheduled_at DATETIME NULL,
    completed_at DATETIME NULL,
    employee_score_before INT NULL,
    employee_score_after INT NULL,
    feedback TEXT,
    status ENUM('SCHEDULED', 'COMPLETED', 'CANCELLED') DEFAULT 'SCHEDULED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_before_score CHECK (employee_score_before IS NULL OR employee_score_before BETWEEN 0 AND 10),
    CONSTRAINT chk_after_score CHECK (employee_score_after IS NULL OR employee_score_after BETWEEN 0 AND 10),
    CONSTRAINT fk_session_request FOREIGN KEY (training_request_id) REFERENCES training_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_session_sme FOREIGN KEY (sme_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

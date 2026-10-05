# SkillSync

### AI-Powered Workforce Skill Intelligence & Career Development Platform

SkillSync is an AI-powered workforce intelligence platform designed to help organizations understand employee skills, identify skill gaps, recommend personalized learning paths, and provide AI-driven career development guidance.

The platform combines a modern web application with a Spring Boot backend, MySQL database, JWT-based authentication, and an AI agent system powered by Gemini through an OpenAI-compatible API interface.

---

## 🚀 Overview

Organizations often have difficulty understanding:

- What skills their employees currently possess
- Which skills are missing for a particular role
- What employees should learn next
- Which career paths are suitable for an employee
- How employees can improve their skills
- How to assess employee readiness for a particular role

SkillSync addresses these challenges through a centralized workforce skill intelligence platform.

The system allows employees to maintain their profiles and skills while enabling AI agents to analyze their skill sets, identify gaps, recommend learning paths, assess readiness, and provide career-oriented recommendations.

---

# ✨ Key Features

## 1. User Authentication

SkillSync provides secure authentication using:

- User registration
- User login
- JWT-based authentication
- Password handling
- Role-based access concepts
- Protected backend APIs
- JWT authentication filter

Users can securely access their profile and workforce-related information after authentication.

---

## 2. Employee Profile Management

Employees can maintain their professional profiles containing information such as:

- Name
- Email
- Role
- Skills
- Experience
- Professional information
- Career-related information

The backend provides APIs for creating, retrieving, and updating user information.

---

## 3. Skill Management

SkillSync enables the system to maintain employee skill information.

The platform can be used to:

- Add skills
- Update skills
- Analyze existing skills
- Compare skills against target roles
- Identify missing capabilities
- Generate skill improvement recommendations

---

# 🤖 AI-Powered Workforce Intelligence

One of the core capabilities of SkillSync is its multi-agent AI architecture.

Instead of using a single AI prompt for every task, SkillSync separates responsibilities across specialized AI agents.

The system contains multiple agents that focus on specific workforce intelligence tasks.

---

## 🧠 AI Agent Architecture

### Supervisor Agent

The Supervisor Agent acts as the central coordinator of the AI system.

Its responsibilities include:

- Understanding the user's request
- Determining which specialized agent should handle the request
- Coordinating AI analysis
- Combining results from different agents
- Providing a unified response to the user

Conceptually:

```text
                    User
                     |
                     v
              Supervisor Agent
                     |
        +------------+------------+
        |            |            |
        v            v            v
   Skill Gap     Career       Learning
     Agent       Agent         Agent
        |            |            |
        +------------+------------+
                     |
                     v
              Final AI Response

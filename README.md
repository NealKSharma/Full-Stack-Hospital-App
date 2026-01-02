# Cy Saint’s Hospital

<div align="center">
  <a href="https://www.youtube.com/watch?v=8IttIGWHdNU">
    <img src="https://img.youtube.com/vi/8IttIGWHdNU/0.jpg" alt="Watch the Demo">
  </a>
  <br>
  <br>
  <a href="https://youtu.be/8IttIGWHdNU?si=Prx053JU29lgodqB">
    <b>▶️ Watch the Project Demo on YouTube</b>
  </a>
</div>  
<br>
Cy Saint’s Hospital is a full-scale Android application designed to simulate the operations of a modern hospital system. It brings together patient care, medical staff management, pharmacy operations, appointment scheduling, messaging, and administrative control into one unified platform.

The app is built to demonstrate how a real hospital could manage its internal workflows digitally across multiple roles: **Patient**, **Doctor**, **Admin**, and **Pharmacist**.

---

## Overview

Cy Saint’s Hospital provides different experiences depending on the user’s role. Each role has access to the tools and information needed for their responsibilities within the hospital system.
The backend powers authentication, secure messaging, pharmacy management, appointments, notifications, and medical records using a Spring Boot API connected to a MySQL database.

The system supports:
- Real-time messaging
- Pharmacy orders and prescription handling
- Medical reports
- Appointment scheduling
- Role-specific permissions
- Error monitoring for administrators
- JWT-based authentication
- Complete database-driven workflows

This project demonstrates how large-scale healthcare systems can be organized digitally while maintaining clarity and separation between different user types.

---

## Features

### **Patients**
Patients interact with the hospital through the mobile app. They can:
- Create an account and log in
- Browse pharmacy products with detailed info
- Add items to their cart and checkout
- Schedule appointments with doctors
- Send direct or group messages to doctors
- Receive notifications about appointments, prescriptions, lab results, and messages
- View personal reports, medical records, treatment plans, and history

The patient interface focuses on usability, clear navigation, and transparency.

### **Doctors**
Doctors use the app to manage patient care. They can:
- Log into their doctor portal
- Write and manage prescriptions for patients
- View and update patient medical histories
- Access patient reports
- Chat with patients through secure messaging
- Manage their appointment schedules (view, approve, cancel)
- Receive notifications about upcoming appointments and new messages

This allows doctors to handle administrative and medical tasks in one place.

### **Admins**
Admins oversee the entire system from a control perspective. They can:
- Perform full CRUD operations on all user accounts
- Assign and modify user roles (Patient, Doctor, Pharmacist, Admin)
- Manage pharmacy products (add, update, delete items)
- View all backend error logs directly in the admin portal
- Receive notifications when critical backend errors occur
- Oversee global hospital data and system health

Admins effectively act as system supervisors and have the highest level of access.

### **Pharmacists**
Pharmacists manage all pharmacy-related operations. They can:
- View incoming pharmacy orders
- Process orders and update order status (received, in-progress, shipped)
- Manage prescriptions issued by doctors
- Edit, update, and manage inventory for pharmacy products
- Adjust product dosage, price, general name, and more
- Participate in message threads related to prescriptions when needed

The pharmacist role completes the full pharmacy pipeline from doctor → patient → order → shipment.

---

## System Progress

The Cy Saint’s Hospital Management System has reached a stable and feature-complete stage designed to support patients, doctors, pharmacists, and administrators. The platform provides secure authentication, communication tools, pharmacy workflows, appointment handling, and system-wide logging to ensure reliability and ease of use.

### **Current Functional Features**
1. **Authentication & User Accounts**
   - Secure login and signup system
   - JWT-based authentication
   - Role separation for Patients, Doctors, Pharmacists, and Admins

2. **Messaging System**
   - Real-time direct messaging between users
   - Group chat support for multi-user communication
   - Message persistence and delivery handling

3. **Notification System**
   - System-wide notifications for appointments, messages, and pharmacy updates
   - Role-specific notifications to ensure relevant users are informed

4. **Pharmacy & Prescription Management**
   - Browsing, searching, and purchasing pharmacy products
   - Prescription workflows between doctors and pharmacists
   - Pharmacist dashboard for verifying and fulfilling prescriptions
   - Shopping cart and checkout system

5. **Appointment Scheduling**
   - Patients can schedule appointments with doctors
   - Doctors receive appointment requests and notifications
   - Automatic reminders and system updates

6. **Logging & Error Handling**
   - Global error logging to track backend issues
   - Consistent and structured logs for debugging and analysis

7. **System Architecture**
   - Spring Boot backend with REST APIs
   - React frontend (undergoing UI improvements)
   - WebSockets for real-time communication
   - Modular service-based backend structure

### **Ongoing Improvements**
1. Support for message attachments
2. Frontend UI/UX modernization
3. Admin-only error and system health alerts
4. A message queue for safer offline message handling

---

## Database Overview

Cy Saint’s Hospital uses a relational database to organize all hospital data.
Below is an overview of each table and its purpose:

### **Core Tables**
- **users** – Contains all registered accounts with role, credentials, and base identity.
- **patients** – Additional patient-specific details such as medical info, DOB, symptoms.
- **doctors** – Doctor details including speciality and availability.
- **admins** – Admin-specific info such as permissions and last login.
- **pharmacists** – Pharmacist identity and operational data.

### **Hospital Operations**
- **appointments** – Links patients and doctors for scheduled visits.
- **prescriptions** – Prescriptions created by doctors, linked to pharmacy and reports.
- **reports** – Medical reports issued for patients, including outcomes, results, and dates.

### **Pharmacy System**
- **pharmacy_products** – Full list of pharmacy items with pricing, dosage, and more.
- **pharmacy_orders** – Orders placed by patients and fulfilled by pharmacists.
- **cart** – Items currently in each patient’s shopping cart.

### **Communication**
- **chat_messages** – Stores all messages (direct and group).
- **notifications** – Logs notifications for each user with timestamps.

### **System & Monitoring**
- **error_log** – Stores backend errors including file, method, line number, message, and stack trace.

### **Relationships**
- One-to-one: User ↔ Patient, User ↔ Doctor, User ↔ Admin, User ↔ Pharmacist
- One-to-many: Doctor ↔ Reports, Patient ↔ Reports
- Many-to-many: User ↔ Notifications, User ↔ Cart Items

The database is designed for clarity, extensibility, and strong separation across roles.

---

## Contributors (3_hosen_5)

### Frontend
- **Trice Buchanan** — Junior, Software Engineering
- **Quinn Beckman** — Junior, Software Engineering

### Backend
- **Neal Sharma** — Sophomore, Computer Science
- **Devank Uppal** — Junior, Computer Science

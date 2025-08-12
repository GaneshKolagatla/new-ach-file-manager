# 📂 new-ach-file-manager

![Java](https://img.shields.io/badge/Java-17-orange?logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?logo=postgresql)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)
![Status](https://img.shields.io/badge/Status-Active-success)
![Build](https://img.shields.io/badge/Build-Maven-blue?logo=apache-maven)

---

<p align="center">
  <img src="https://dummyimage.com/800x200/0d1117/ffffff&text=ACH+File+Manager" alt="ACH File Manager Banner">
</p>

**A Java Spring Boot application for generating, uploading, downloading, and managing ACH files via SFTP with complete database logging.**

---

## 🚀 Overview

This project automates the **ACH (Automated Clearing House)** file lifecycle:

- **Generate** `.ach` files
- **Encrypt** to `.pgp`
- **Upload** to SFTP server
- **Download** `.pgp` files
- **Decrypt** to `.ach`
- **Parse** into structured records
- **Log** each operation with unique sequence IDs

---

## ✨ Features

- 🔄 **SFTP Integration** (Upload & Download)
- 🔐 **ACH File Encryption/Decryption** using `.asc` private keys
- 📂 **File Parsing** into File Header, Batch Header, and Entry Detail records
- 🗄 **Database Logging** (`remote_file_tbl`)
- 🆔 **Unique Sequence Tracking** (`U101`, `D101`, …)
- ⚠ **Error Handling & Rollback**
- ⚙ **Configurable Credentials** via `application.properties`

---

## 🏗 Tech Stack

- **Java 17**
- **Spring Boot 3.x**
- **Spring Data JPA**
- **PostgreSQL (Supabase)**
- **JSCH** (SFTP)
- **BouncyCastle** (PGP)

---

📜 **Project Structure**

### 🖼 File Structure Diagram
![File Structure](A_digital_diagram_displays_the_file_structure_of_a.png)

## 🔄 Workflow Diagram

```mermaid
flowchart TD
    A([📄 Generate .ach File]):::start --> B([🔐 Encrypt to .pgp]):::process
    B --> C([☁ Upload to SFTP Server]):::upload
    C --> D([📥 Download .pgp from SFTP]):::download
    D --> E([🔓 Decrypt to .ach]):::process
    E --> F([🧩 Parse into File Header, Batch Header, Entry Details]):::process
    F --> G([💾 Insert into DB]):::db
    G --> H([📝 Log Event in remote_file_tbl]):::log

    classDef start fill:#f9f871,stroke:#333,stroke-width:2px;
    classDef process fill:#a0e7e5,stroke:#333,stroke-width:2px;
    classDef upload fill:#a4f9c8,stroke:#333,stroke-width:2px;
    classDef download fill:#ffb6b9,stroke:#333,stroke-width:2px;
    classDef db fill:#caffbf,stroke:#333,stroke-width:2px;
    classDef log fill:#ffd6a5,stroke:#333,stroke-width:2px;


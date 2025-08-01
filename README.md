# Meli Discount Challenge

This repository contains a **two‑service ecosystem** designed to simulate and solve a simplified discount rule problem for Mercado Libre items.  

---

## ⚙️ Services Overview

### 1️⃣ Items API (`items/`)

A lightweight **Go API** that provides:

- **`GET /items?ids=MLA1,MLA2`**  
  Returns a list of item metadata, e.g.:

  ```json
  [
    {
      "seller_id": "SELLER_1",
      "title": "Basic Shoes",
      "category_id": "MLA010400",
      "price": 409.69,
      "date_created": "2024-11-19T18:05:50.164451Z",
      "last_updated": "2024-11-24T18:05:50.164451Z",
      "id": "MLA1"
    }
  ]
  ```

- **`GET /categories?ids=MLA3,MLA5`**  
  Returns the mapping between **root categories** and items:

  ```json
  [
    {
      "root_category_id": "MLA1005",
      "item_ids": ["MLA3"]
    }
  ]
  ```

> ✅ **Data requirement:** Before running, you must generate random test data via `main.py`.  
> The generated JSON files will be placed inside `items/data/`.

---

### 2️⃣ MeliDiscount API (`melidiscount/`)

A **Java 21 Spring Boot** microservice that consumes the `items/` API to apply the **Meli Discount Rule**:  
It computes the **maximum set of items whose active periods do not overlap** using a **greedy interval scheduling algorithm**.

Endpoints:

- **`GET /meli_discount?item_ids=MLA1,MLA2,ML1999`**

  Returns a minimal non‑overlapping subset **globally**:

  ```json
  {
    "item_ids": [
      "MLA1",
      "MLA2"
    ]
  }
  ```

- **`GET /meli_discount/categories?item_ids=MLA1,MLA2,MLA5`**

  Returns the maximal subset **per root category**:

  ```json
  [
    {
      "root_category_id": "MLA1001",
      "item_ids": ["MLA1"]
    },
    {
      "root_category_id": "MLA1004",
      "item_ids": ["MLA145","MLA2"]
    },
    {
      "root_category_id": "MLA1005",
      "item_ids": ["MLA845","MLA9","MLA5"]
    },
    {
      "root_category_id": "MLA1002",
      "item_ids": ["MLA61"]
    },
    {
      "root_category_id": "MLA1003",
      "item_ids": ["MLA400","MLA99","MLA10"]
    }
  ]
  ```

---

## 🚀 How to Run Manually

### 1. Generate Test Data

Go to the **items service** and generate random mock data:

```bash
cd items/
python3 main.py
```

This will create JSON files inside `items/data/`.

---

### 2. Start the Items API

From the same folder:

```bash
go run main.go
```

This starts the Go server on **`http://localhost:8080`**.

---

### 3. Start the MeliDiscount API

In a new terminal:

```bash
cd melidiscount/
./mvnw spring-boot:run
```

The service will run on **`http://localhost:9090`**.

---

## 🧪 Quick Test

Once both services are running:

1. Test a **global selection**:
   ```bash
   curl --location "http://localhost:9090/meli_discount?item_ids=MLA1,MLA2,ML1999"
   ```
   ✅ Returns:
   ```json
   { "item_ids": ["MLA1", "MLA2"] }
   ```

2. Test a **per-category selection**:
   ```bash
   curl --location "http://localhost:9090/meli_discount/categories?item_ids=MLA1,MLA2,MLA5"
   ```
   ✅ Returns a list grouped by root categories.

---

## 🐳 Run Everything with Docker Compose

You can also run the entire ecosystem with a **single command** using Docker Compose.

1. **Generate test data first**  
   (this creates the JSON files under `items/data` that the Go API will serve):

   ```bash
   cd items
   python3 main.py
   ```

2. **Build & start all services**:

   ```bash
   docker compose up --build
   ```

   This will:
   - Build and start `items-api` on port `8080`.
   - Build and start `melidiscount-api` on port `9090`.
   - Automatically configure the `melidiscount-api` to call `http://items-api:8080` internally.

3. Test them:

   ```bash
   curl http://localhost:9090/meli_discount?item_ids=MLA1,MLA2,MLA5
   ```

   ```bash
   curl http://localhost:9090/meli_discount/categories?item_ids=MLA1,MLA2,MLA5
   ```

> ✅ **Note:** The `docker-compose.yml` injects the `EXTERNAL_ITEMS_SERVICE_BASE_URL` environment variable into the `melidiscount-api`, so it can resolve the Go service by its container name (`items-api`).

---

## 📊 Load Testing

A basic load test script is provided:

```bash
cd items/
python3 load_test.py
```

This simulates concurrent load against the **Items API**.

---

## 🏗️ Technical Highlights

- **Go** microservice serving static JSON data.
- **Java Spring Boot 3+ (Java 21)** with caching (`@Cacheable`) for efficient repeated lookups.
- **Greedy interval scheduling algorithm** for optimal non‑overlapping selections.
- Clean Code & SOLID principles applied to service and controller design.
- Compatible with **Docker Compose** for easy multi-service orchestration.

---

## ✅ Requirements

- **Go ≥1.20**  
- **Java 21**  
- **Maven ≥3.9**  
- **Python ≥3.10** (only for test-data & load testing)
- **Docker & Docker Compose** (optional but recommended)

---

## 📌 Summary

- `items/` acts as a **mock catalog service**.  
- `melidiscount/` acts as a **business logic layer** selecting optimal discounts.  
- Together, they simulate a real‑world microservice integration scenario.  
- You can run both **manually** or with **Docker Compose** for a seamless setup.

---

> 💡 **Tip:** Modify the data generation script (`items/main.py`) to test with larger datasets, then measure performance with `items/load_test.py`.

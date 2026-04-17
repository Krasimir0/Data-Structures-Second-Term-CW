CREATE TABLE Customer (
    customer_id INT PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100),
    password VARCHAR(100),
    phone VARCHAR(20),
    address VARCHAR(200)
);

CREATE TABLE Car (
    car_id INT PRIMARY KEY,
    brand VARCHAR(50),
    model VARCHAR(50),
    year INT,
    price DECIMAL(10,2),
    status VARCHAR(20)
);

CREATE TABLE Orders (
    order_id INT PRIMARY KEY,
    order_date DATE,
    total_amount DECIMAL(10,2),
    status VARCHAR(20),
    customer_id INT,
    car_id INT,
    FOREIGN KEY (customer_id) REFERENCES Customer(customer_id),
    FOREIGN KEY (car_id) REFERENCES Car(car_id)
);

INSERT INTO Customer VALUES
(1, 'John Doe', 'john@test.com', '123', '123456789', 'NY');

INSERT INTO Car VALUES
(1, 'BMW', 'X5', 2022, 55000, 'available'),
(2, 'Tesla', 'Model 3', 2023, 45000, 'available');

INSERT INTO Orders VALUES
(1, CURRENT_DATE, 55000, 'completed', 1, 1);
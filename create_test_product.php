<?php
// Enable error reporting for debugging
ini_set('display_errors', 1);
error_reporting(E_ALL);

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

// Database configuration
$servername = "localhost";
$username = "root";
$password = "";
$dbname = "agroyard";

// Create connection
$conn = new mysqli($servername, $username, $password, $dbname);

// Check connection
if ($conn->connect_error) {
    echo json_encode([
        "success" => false,
        "message" => "Connection failed: " . $conn->connect_error
    ]);
    exit;
}

// Check if database exists
$db_check = $conn->query("SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '$dbname'");
if ($db_check->num_rows == 0) {
    // Create database
    if (!$conn->query("CREATE DATABASE IF NOT EXISTS $dbname")) {
        echo json_encode([
            "success" => false,
            "message" => "Error creating database: " . $conn->error
        ]);
        exit;
    }
    
    echo "Database created successfully.<br>";
    
    // Select the database
    $conn->select_db($dbname);
} else {
    // Database exists, select it
    $conn->select_db($dbname);
}

// Check if table exists
$table_check = $conn->query("SHOW TABLES LIKE 'products'");
if ($table_check->num_rows == 0) {
    // Create products table
    $create_table_sql = "CREATE TABLE products (
        id INT(11) AUTO_INCREMENT PRIMARY KEY,
        farmer_name VARCHAR(255) NOT NULL,
        farmer_mobile VARCHAR(20) NOT NULL,
        product_name VARCHAR(255) NOT NULL,
        harvesting_date DATE NOT NULL,
        farming_type VARCHAR(50) NOT NULL,
        quantity DECIMAL(10,2) NOT NULL,
        price DECIMAL(10,2) NOT NULL,
        expected_price DECIMAL(10,2) NOT NULL,
        description TEXT NOT NULL,
        image_filename VARCHAR(255) DEFAULT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )";
    
    if (!$conn->query($create_table_sql)) {
        echo json_encode([
            "success" => false,
            "message" => "Error creating table: " . $conn->error
        ]);
        exit;
    }
    
    echo "Table created successfully.<br>";
}

// Define multiple test products
$test_products = [
    [
        'farmer_name' => 'Rajesh Kumar',
        'farmer_mobile' => '9876543210',
        'product_name' => 'Premium Organic Tomatoes',
        'harvesting_date' => '2023-08-15',
        'farming_type' => 'Organic',
        'quantity' => 50.00,
        'price' => 45.50,
        'expected_price' => 50.00,
        'description' => 'Fresh organic tomatoes grown without pesticides. Rich in flavor and nutrients. Harvested from our family farm in Maharashtra.',
        'image_filename' => 'placeholder.jpg'
    ],
    [
        'farmer_name' => 'Sunita Patel',
        'farmer_mobile' => '8765432109',
        'product_name' => 'Premium Quality Wheat',
        'harvesting_date' => '2023-07-20',
        'farming_type' => 'Traditional',
        'quantity' => 200.00,
        'price' => 32.75,
        'expected_price' => 35.00,
        'description' => 'High-quality wheat with excellent protein content. Perfect for making chapatis and bread. Grown in Gujarat using traditional farming methods.',
        'image_filename' => 'placeholder.jpg'
    ],
    [
        'farmer_name' => 'Mahesh Singh',
        'farmer_mobile' => '7654321098',
        'product_name' => 'Fresh Green Peas',
        'harvesting_date' => '2023-09-05',
        'farming_type' => 'Natural',
        'quantity' => 30.00,
        'price' => 80.00,
        'expected_price' => 90.00,
        'description' => 'Sweet and tender green peas freshly harvested from our farm in Punjab. No chemical fertilizers used, grown with natural compost.',
        'image_filename' => 'placeholder.jpg'
    ],
    [
        'farmer_name' => 'Lakshmi Reddy',
        'farmer_mobile' => '6543210987',
        'product_name' => 'Basmati Rice',
        'harvesting_date' => '2023-08-25',
        'farming_type' => 'Organic',
        'quantity' => 100.00,
        'price' => 120.00,
        'expected_price' => 130.00,
        'description' => 'Premium long-grain basmati rice with authentic aroma. Grown in Haryana using organic farming practices. Aged for extra flavor.',
        'image_filename' => 'placeholder.jpg'
    ]
];

// Insert test products
$success_count = 0;
$product_ids = [];

foreach ($test_products as $product) {
    $insert_sql = "INSERT INTO products (
        farmer_name, 
        farmer_mobile, 
        product_name, 
        harvesting_date, 
        farming_type, 
        quantity, 
        price, 
        expected_price, 
        description, 
        image_filename
    ) VALUES (
        '{$product['farmer_name']}', 
        '{$product['farmer_mobile']}', 
        '{$product['product_name']}', 
        '{$product['harvesting_date']}', 
        '{$product['farming_type']}', 
        {$product['quantity']}, 
        {$product['price']}, 
        {$product['expected_price']}, 
        '{$product['description']}', 
        '{$product['image_filename']}'
    )";

    if ($conn->query($insert_sql)) {
        $success_count++;
        $product_ids[] = $conn->insert_id;
    } else {
        echo "Error inserting product '{$product['product_name']}': " . $conn->error . "<br>";
    }
}

$conn->close();

if ($success_count > 0) {
    echo json_encode([
        "success" => true,
        "message" => "$success_count test products inserted successfully.",
        "product_ids" => $product_ids
    ]);
} else {
    echo json_encode([
        "success" => false,
        "message" => "No products were inserted. Please check the error messages."
    ]);
}
?> 
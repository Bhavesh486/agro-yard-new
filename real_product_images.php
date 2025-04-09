<?php
// Enable error reporting for debugging
ini_set('display_errors', 1);
error_reporting(E_ALL);

header('Content-Type: text/html');

// Define the upload directory
$upload_dir = 'C:/xampp/htdocs/agroyard/api/uploads/';

// Ensure the directory exists and is writable
if (!is_dir($upload_dir)) {
    mkdir($upload_dir, 0777, true);
    echo "<p>Created directory: $upload_dir</p>";
}

// Set proper permissions
chmod($upload_dir, 0777);
echo "<p>Directory permissions updated.</p>";

// Product data with URLs to real images
$products = [
    [
        'name' => 'Fresh Apples',
        'type' => 'fruit',
        'farmer' => 'Sunny Farms',
        'image_url' => 'https://images.unsplash.com/photo-1570913149827-d2ac84ab3f9a?w=800&auto=format&fit=crop'
    ],
    [
        'name' => 'Organic Tomatoes',
        'type' => 'vegetable',
        'farmer' => 'Green Gardens',
        'image_url' => 'https://images.unsplash.com/photo-1582284540020-8acbe03f4924?w=800&auto=format&fit=crop'
    ],
    [
        'name' => 'Premium Mangoes',
        'type' => 'fruit',
        'farmer' => 'Tropical Harvests',
        'image_url' => 'https://images.unsplash.com/photo-1601493700631-2b16ec4b4716?w=800&auto=format&fit=crop'
    ],
    [
        'name' => 'Russet Potatoes',
        'type' => 'vegetable',
        'farmer' => 'Root Crops Inc',
        'image_url' => 'https://images.unsplash.com/photo-1518977676601-b53f82aba655?w=800&auto=format&fit=crop'
    ],
    [
        'name' => 'Organic Bananas',
        'type' => 'fruit',
        'farmer' => 'Fruit Express',
        'image_url' => 'https://images.unsplash.com/photo-1603833665858-e61d17a86224?w=800&auto=format&fit=crop'
    ],
    [
        'name' => 'Fresh Carrots',
        'type' => 'vegetable',
        'farmer' => 'Organic Valley',
        'image_url' => 'https://images.unsplash.com/photo-1582515073490-39981397c445?w=800&auto=format&fit=crop'
    ],
    [
        'name' => 'Red Grapes',
        'type' => 'fruit',
        'farmer' => 'Vineyard Fresh',
        'image_url' => 'https://images.unsplash.com/photo-1596363505729-8092738108ed?w=800&auto=format&fit=crop'
    ]
];

// Database configuration
$servername = "localhost";
$username = "root";
$password = "";
$dbname = "agroyard";

// Create connection
$conn = new mysqli($servername, $username, $password);

// Check connection
if ($conn->connect_error) {
    die("<p>Connection failed: " . $conn->connect_error . "</p>");
}

// Check if database exists, if not create it
if (!$conn->select_db($dbname)) {
    $sql = "CREATE DATABASE IF NOT EXISTS $dbname";
    if ($conn->query($sql) === TRUE) {
        echo "<p>Database created successfully</p>";
        $conn->select_db($dbname);
    } else {
        die("<p>Error creating database: " . $conn->error . "</p>");
    }
}

// Check if products table exists
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
    
    if ($conn->query($create_table_sql) === TRUE) {
        echo "<p>Table created successfully</p>";
    } else {
        echo "<p>Error creating table: " . $conn->error . "</p>";
    }
}

echo "<h2>Downloading Real Product Images</h2>";
echo "<div style='display: flex; flex-wrap: wrap;'>";

// Function to download image
function downloadImage($url, $filepath) {
    $ch = curl_init($url);
    $fp = fopen($filepath, 'wb');
    curl_setopt($ch, CURLOPT_FILE, $fp);
    curl_setopt($ch, CURLOPT_HEADER, 0);
    curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 30);
    curl_exec($ch);
    $httpcode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    fclose($fp);
    return $httpcode == 200;
}

// Generate products
foreach ($products as $product) {
    // Create a unique filename
    $filename = strtolower(str_replace(' ', '_', $product['name'])) . '_' . time() . rand(100, 999) . '.jpg';
    $filepath = $upload_dir . $filename;
    
    // Download the image
    $download_success = downloadImage($product['image_url'], $filepath);
    
    if ($download_success) {
        // Set proper permissions
        chmod($filepath, 0644);
        
        // Generate random product details
        $farmer_name = $product['farmer'];
        $farmer_mobile = "98765" . rand(10000, 99999);
        $harvesting_date = date('Y-m-d');
        $farming_type = rand(0, 1) == 0 ? "Organic" : "Natural";
        $quantity = rand(100, 1000);
        $price = rand(30, 100);
        $expected_price = $price + rand(5, 20);
        $description = "Fresh " . $product['name'] . " available for sale. High quality " . $farming_type . " farming.";
        
        // Insert into database
        $sql = "INSERT INTO products (
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
            '$farmer_name', 
            '$farmer_mobile', 
            '{$product['name']}', 
            '$harvesting_date', 
            '$farming_type', 
            $quantity, 
            $price, 
            $expected_price, 
            '$description', 
            '$filename'
        )";
        
        $insert_success = $conn->query($sql) === TRUE;
        
        // URLs for the image
        $emulator_url = "http://10.0.2.2/agroyard/api/uploads/" . $filename;
        $browser_url = "http://localhost/agroyard/api/uploads/" . $filename;
        
        // Show the downloaded image
        echo "<div style='margin: 10px; border: 1px solid #ccc; padding: 10px; border-radius: 5px; width: 300px;'>";
        echo "<h3>{$product['name']}</h3>";
        echo "<div style='position: relative;'>";
        echo "<img src='http://localhost/agroyard/api/uploads/$filename' style='width: 100%; height: auto; object-fit: cover;' alt='{$product['name']}' />";
        
        if ($insert_success) {
            echo "<div style='position: absolute; top: 10px; right: 10px; background-color: green; color: white; padding: 5px; border-radius: 3px;'>Added to DB</div>";
        } else {
            echo "<div style='position: absolute; top: 10px; right: 10px; background-color: red; color: white; padding: 5px; border-radius: 3px;'>DB Error</div>";
        }
        
        echo "</div>";
        echo "<p><strong>Filename:</strong> $filename</p>";
        echo "<p><strong>Farmer:</strong> {$product['farmer']}</p>";
        echo "<p><strong>Price:</strong> ₹$price/kg</p>";
        echo "<p><strong>App URL:</strong> $emulator_url</p>";
        echo "</div>";
    } else {
        echo "<div style='margin: 10px; border: 1px solid #ccc; padding: 10px; border-radius: 5px; width: 300px;'>";
        echo "<h3>{$product['name']}</h3>";
        echo "<p style='color: red;'>Failed to download image from: {$product['image_url']}</p>";
        echo "</div>";
    }
}

echo "</div>";

echo "<h2>Android App Access</h2>";
echo "<p>To view these products in your Android app, please make sure:</p>";
echo "<ol>";
echo "<li>Your app is using <code>http://10.0.2.2/agroyard/api/uploads/</code> as the base URL for image access</li>";
echo "<li>The database connection in your PHP files points to the same database</li>";
echo "<li>Restart your app to refresh the product listing</li>";
echo "</ol>";

$conn->close();
?> 
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

// Product data to generate
$products = [
    ['name' => 'Apples', 'type' => 'fruit', 'color' => '#FF5722', 'farmer' => 'Sunny Farms'],
    ['name' => 'Tomatoes', 'type' => 'vegetable', 'color' => '#E53935', 'farmer' => 'Green Gardens'],
    ['name' => 'Mangoes', 'type' => 'fruit', 'color' => '#FF9800', 'farmer' => 'Tropical Harvests'],
    ['name' => 'Potatoes', 'type' => 'vegetable', 'color' => '#8D6E63', 'farmer' => 'Root Crops Inc'],
    ['name' => 'Bananas', 'type' => 'fruit', 'color' => '#FFD600', 'farmer' => 'Fruit Express']
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

echo "<h2>Generated Product Images</h2>";
echo "<div style='display: flex; flex-wrap: wrap;'>";

// Generate products
foreach ($products as $product) {
    // Create a unique filename
    $filename = strtolower(str_replace(' ', '_', $product['name'])) . '_' . time() . rand(100, 999) . '.jpg';
    $filepath = $upload_dir . $filename;
    
    // Create a canvas for the image (400x300)
    $img = imagecreatetruecolor(400, 300);
    
    // Parse color hex to RGB
    $color = $product['color'];
    $r = hexdec(substr($color, 1, 2));
    $g = hexdec(substr($color, 3, 2));
    $b = hexdec(substr($color, 5, 2));
    
    // Define colors
    $bg_color = imagecolorallocate($img, $r, $g, $b);
    $text_color = imagecolorallocate($img, 255, 255, 255);
    $border_color = imagecolorallocate($img, 255, 255, 255);
    
    // Fill background
    imagefilledrectangle($img, 0, 0, 399, 299, $bg_color);
    
    // Add a border
    imagerectangle($img, 5, 5, 394, 294, $border_color);
    
    // Add product name text
    $font_size = 5; // Largest built-in font
    $text_width = imagefontwidth($font_size) * strlen($product['name']);
    $text_x = (400 - $text_width) / 2;
    imagestring($img, $font_size, $text_x, 80, $product['name'], $text_color);
    
    // Add farmer name
    $farmer_text = "by " . $product['farmer'];
    $farmer_width = imagefontwidth(3) * strlen($farmer_text);
    $farmer_x = (400 - $farmer_width) / 2;
    imagestring($img, 3, $farmer_x, 140, $farmer_text, $text_color);
    
    // Add date text
    $date_text = "Harvested: " . date("Y-m-d");
    $date_width = imagefontwidth(2) * strlen($date_text);
    $date_x = (400 - $date_width) / 2;
    imagestring($img, 2, $date_x, 180, $date_text, $text_color);
    
    // Save the image
    imagejpeg($img, $filepath, 90);
    imagedestroy($img);
    
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
    
    // Show the generated image
    echo "<div style='margin: 10px; border: 1px solid #ccc; padding: 10px; border-radius: 5px; width: 300px;'>";
    echo "<h3>{$product['name']}</h3>";
    echo "<div style='position: relative;'>";
    echo "<img src='http://localhost/agroyard/api/uploads/$filename' style='width: 100%; height: auto;' alt='{$product['name']}' />";
    
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
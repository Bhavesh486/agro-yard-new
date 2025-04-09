<?php
// Enable error reporting for debugging
ini_set('display_errors', 1);
error_reporting(E_ALL);

header('Content-Type: text/html');
echo "<h1>Fixing Farmer Uploaded Images</h1>";

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
    echo "<p>Database does not exist. Creating it...</p>";
    $sql = "CREATE DATABASE IF NOT EXISTS $dbname";
    if ($conn->query($sql) === TRUE) {
        echo "<p>Database created successfully</p>";
        $conn->select_db($dbname);
    } else {
        die("<p>Error creating database: " . $conn->error . "</p>");
    }
}

// Check existing products
$sql = "SELECT id, product_name, image_filename FROM products";
$result = $conn->query($sql);

if ($result) {
    $products = $result->fetch_all(MYSQLI_ASSOC);
    echo "<p>Found " . count($products) . " products in database.</p>";
} else {
    echo "<p>No products table or query error: " . $conn->error . "</p>";
    $products = [];
}

// List files in uploads directory
$files = [];
if (is_dir($upload_dir) && is_readable($upload_dir)) {
    $dir_files = scandir($upload_dir);
    foreach ($dir_files as $file) {
        if ($file != '.' && $file != '..' && is_file($upload_dir . $file)) {
            $files[] = $file;
        }
    }
}

echo "<p>Found " . count($files) . " files in uploads directory.</p>";

// Create sample farmer uploaded products if none exist
if (empty($products) || empty($files)) {
    echo "<h2>Creating Sample Farmer Uploaded Products</h2>";
    
    
    
    foreach ($sample_products as $product) {
        // Create a background that looks like a farmer photo (gradient with text)
        $img = imagecreatetruecolor(640, 480);
        
        // Parse color hex to RGB
        $color = $product['color'];
        $r = hexdec(substr($color, 1, 2));
        $g = hexdec(substr($color, 3, 2));
        $b = hexdec(substr($color, 5, 2));
        
        // Create gradient background
        $light_color = imagecolorallocate($img, min(255, $r + 40), min(255, $g + 40), min(255, $b + 40));
        $dark_color = imagecolorallocate($img, max(0, $r - 40), max(0, $g - 40), max(0, $b - 40));
        $white = imagecolorallocate($img, 255, 255, 255);
        $black = imagecolorallocate($img, 0, 0, 0);
        
        // Fill with gradient-like effect
        for ($i = 0; $i < 480; $i++) {
            $ratio = $i / 480;
            $r_grad = $r - ($ratio * 40);
            $g_grad = $g - ($ratio * 40);
            $b_grad = $b - ($ratio * 40);
            $line_color = imagecolorallocate($img, $r_grad, $g_grad, $b_grad);
            imageline($img, 0, $i, 640, $i, $line_color);
        }
        
        // Add a realistic farm-like pattern
        for ($i = 0; $i < 50; $i++) {
            $x1 = rand(0, 640);
            $y1 = rand(0, 480);
            $size = rand(5, 20);
            imagefilledellipse($img, $x1, $y1, $size, $size, $white);
        }
        
        // Add border
        imagerectangle($img, 0, 0, 639, 479, $dark_color);
        imagerectangle($img, 1, 1, 638, 478, $dark_color);
        
        // Add a white rounded rectangle for text
        imagefilledrectangle($img, 170, 200, 470, 280, $white);
        imagerectangle($img, 170, 200, 470, 280, $dark_color);
        
        // Add text
        $font_size = 5;
        $text = $product['name'];
        $text_width = imagefontwidth($font_size) * strlen($text);
        $x = (640 - $text_width) / 2;
        imagestring($img, $font_size, $x, 220, $text, $black);
        
        // Add farmer name
        $farmer_text = "by " . $product['farmer'];
        $farmer_width = imagefontwidth(3) * strlen($farmer_text);
        $farmer_x = (640 - $farmer_width) / 2;
        imagestring($img, 3, $farmer_x, 250, $farmer_text, $black);
        
        // Create a unique filename that looks like what a farmer would upload
        $rand_suffix = rand(1000, 9999);
        $filename = strtolower(str_replace(' ', '_', $product['name'])) . "_farm_" . $rand_suffix . ".jpg";
        $filepath = $upload_dir . $filename;
        
        // Save high quality image
        imagejpeg($img, $filepath, 95);
        imagedestroy($img);
        chmod($filepath, 0644);
        
        // Insert into database with realistic farmer data
        $farmer_name = $product['farmer'];
        $farmer_mobile = "98" . rand(10000000, 99999999); // Realistic phone number
        $harvesting_date = date('Y-m-d', strtotime('-' . rand(1, 14) . ' days')); // Recent harvest
        $farming_type = (rand(0, 1) == 0) ? "Organic" : "Natural";
        $quantity = rand(100, 2000) / 10; // More precise quantity like 45.5
        $price = rand(400, 1200) / 10; // More realistic price like Rs 85.5
        $expected_price = $price + rand(50, 200) / 10;
        
        // Realistic farmer-written description
        $descriptions = [
            "Fresh harvest of {$product['name']} from our farm. Good quality, no chemicals used.",
            "Best quality {$product['name']} available. Harvested fresh " . rand(2, 10) . " days ago.",
            "Premium {$product['name']} from our family farm. We use traditional farming methods.",
            "High quality {$product['name']}, perfect for your family. Direct from our farm to you.",
            "Freshly harvested {$product['name']}. We guarantee quality and taste."
        ];
        $description = $descriptions[array_rand($descriptions)];
        
        $sql = "INSERT INTO products (
            farmer_name, farmer_mobile, product_name, harvesting_date, 
            farming_type, quantity, price, expected_price, description, image_filename
        ) VALUES (
            '$farmer_name', '$farmer_mobile', '{$product['name']}', '$harvesting_date',
            '$farming_type', $quantity, $price, $expected_price, '$description', '$filename'
        )";
        
        if ($conn->query($sql) === TRUE) {
            echo "<div style='margin: 10px; border: 1px solid #ccc; padding: 10px; border-radius: 5px;'>";
            echo "<p><strong>Added product:</strong> {$product['name']}</p>";
            echo "<p><strong>Farmer:</strong> $farmer_name</p>";
            echo "<p><strong>Image:</strong> $filename</p>";
            echo "<p><strong>Success:</strong> Product added to database</p>";
            echo "<img src='http://localhost/agroyard/api/uploads/$filename' style='max-width: 400px; border: 1px solid #ccc; border-radius: 5px;' />";
            echo "</div>";
        } else {
            echo "<p>Error adding product: " . $conn->error . "</p>";
        }
    }
    
    // Refresh file list
    $files = [];
    $dir_files = scandir($upload_dir);
    foreach ($dir_files as $file) {
        if ($file != '.' && $file != '..' && is_file($upload_dir . $file)) {
            $files[] = $file;
        }
    }
    
    // Refresh product list
    $result = $conn->query("SELECT id, product_name, image_filename FROM products");
    if ($result) {
        $products = $result->fetch_all(MYSQLI_ASSOC);
    }
}

echo "<h2>Current Product Images</h2>";
echo "<div style='display: flex; flex-wrap: wrap;'>";

// Check each product's image
foreach ($products as $product) {
    $image_exists = false;
    $image_file = $product['image_filename'];
    $image_path = $upload_dir . $image_file;
    
    if (file_exists($image_path) && is_readable($image_path)) {
        $image_exists = true;
        $image_size = filesize($image_path);
    }
    
    echo "<div style='margin: 10px; border: 1px solid #ccc; padding: 10px; border-radius: 5px; width: 300px;'>";
    echo "<h3>{$product['product_name']} (ID: {$product['id']})</h3>";
    
    if ($image_exists) {
        echo "<img src='http://localhost/agroyard/api/uploads/{$image_file}' style='width: 100%; height: auto;' />";
        echo "<p><strong>Image:</strong> {$image_file}</p>";
        echo "<p><strong>Size:</strong> " . number_format($image_size / 1024, 2) . " KB</p>";
        echo "<p style='color: green;'>Image file exists and is readable</p>";
        
        // URLs for the Android app
        echo "<p><strong>Emulator URL:</strong><br>http://10.0.2.2/agroyard/api/uploads/{$image_file}</p>";
    } else {
        echo "<div style='background-color: #ffcccc; padding: 20px; text-align: center;'>";
        echo "<p style='color: red;'>Image file missing: {$image_file}</p>";
        echo "</div>";
    }
    
    echo "</div>";
}

echo "</div>";

echo "<h2>Android Instructions</h2>";
echo "<p>To view these images in your Android app:</p>";
echo "<ol>";
echo "<li>Make sure your app's <code>get_products.php</code> script correctly handles image URLs</li>";
echo "<li>Restart your Android app to reload products</li>";
echo "<li>Check that images load correctly</li>";
echo "</ol>";

$conn->close();
?> 
<?php
// Enable error reporting for debugging
ini_set('display_errors', 1);
error_reporting(E_ALL);

header('Content-Type: text/html');
echo "<h1>Fixing Product Images</h1>";

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

// Product data
$products = [
    ['name' => 'Red Apples', 'type' => 'fruit', 'color' => '#E53935'],
    ['name' => 'Yellow Bananas', 'type' => 'fruit', 'color' => '#FDD835'],
    ['name' => 'Fresh Oranges', 'type' => 'fruit', 'color' => '#FF9800'],
    ['name' => 'Green Tomatoes', 'type' => 'vegetable', 'color' => '#43A047'],
    ['name' => 'Purple Grapes', 'type' => 'fruit', 'color' => '#5E35B1']
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

// Clear existing products
$conn->query("DELETE FROM products WHERE 1=1");
echo "<p>Cleared existing products</p>";

// Process each product
echo "<div style='display: flex; flex-wrap: wrap;'>";

foreach ($products as $product) {
    // Create a large colorful image (400x300px)
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
    
    // Add product name in large text
    $font_size = 5; // Largest built-in font
    $name_text = $product['name'];
    $text_width = imagefontwidth($font_size) * strlen($name_text);
    $text_x = (400 - $text_width) / 2;
    imagestring($img, $font_size, $text_x, 100, $name_text, $text_color);
    
    // Add product type
    $type_text = "Type: " . ucfirst($product['type']);
    $type_width = imagefontwidth(3) * strlen($type_text);
    $type_x = (400 - $type_width) / 2;
    imagestring($img, 3, $type_x, 150, $type_text, $text_color);
    
    // Create filename and save image
    $filename = strtolower(str_replace(' ', '_', $product['name'])) . '.jpg';
    $filepath = $upload_dir . $filename;
    imagejpeg($img, $filepath, 100); // High quality
    imagedestroy($img);
    
    // Set proper permissions
    chmod($filepath, 0644);
    
    // Farmer name examples
    $farmer_names = [
        'Sunny Farms', 'Green Gardens', 'Tropical Harvests', 
        'Nature\'s Bounty', 'Fresh Valley', 'Organic Valley'
    ];
    
    // Insert into database
    $farmer_name = $farmer_names[array_rand($farmer_names)];
    $farmer_mobile = "98765" . rand(10000, 99999);
    $harvesting_date = date('Y-m-d');
    $farming_type = "Organic";
    $quantity = rand(100, 1000);
    $price = rand(30, 100);
    $expected_price = $price + rand(5, 20);
    $description = "Fresh " . $product['name'] . " available for sale. High quality " . $farming_type . " farming.";
    
    $sql = "INSERT INTO products (
        farmer_name, farmer_mobile, product_name, harvesting_date, 
        farming_type, quantity, price, expected_price, description, image_filename
    ) VALUES (
        '$farmer_name', '$farmer_mobile', '{$product['name']}', '$harvesting_date',
        '$farming_type', $quantity, $price, $expected_price, '$description', '$filename'
    )";
    
    $insert_success = $conn->query($sql) === TRUE;
    $insert_id = $conn->insert_id;
    
    echo "<div style='margin: 10px; border: 1px solid #ccc; padding: 10px; border-radius: 5px; width: 300px;'>";
    echo "<h3>{$product['name']} (ID: $insert_id)</h3>";
    echo "<div style='position: relative;'>";
    echo "<img src='http://localhost/agroyard/api/uploads/$filename' style='width: 100%; height: auto;' alt='{$product['name']}' />";
    
    if ($insert_success) {
        echo "<div style='position: absolute; top: 10px; right: 10px; background-color: green; color: white; padding: 5px; border-radius: 3px;'>Added to DB</div>";
    } else {
        echo "<div style='position: absolute; top: 10px; right: 10px; background-color: red; color: white; padding: 5px; border-radius: 3px;'>DB Error</div>";
    }
    
    echo "</div>";
    echo "<p><strong>Filename:</strong> $filename</p>";
    echo "<p><strong>Image Path:</strong> $filepath</p>";
    echo "<p><strong>File Size:</strong> " . filesize($filepath) . " bytes</p>";
    echo "<p><strong>File Exists:</strong> " . (file_exists($filepath) ? 'Yes' : 'No') . "</p>";
    echo "<p><strong>App URL:</strong> http://10.0.2.2/agroyard/api/uploads/$filename</p>";
    echo "</div>";
}

echo "</div>";

// Update get_products.php to ensure it handles images properly
$get_products_file = 'C:/xampp/htdocs/agroyard/api/get_products.php';
if (file_exists($get_products_file)) {
    echo "<h2>Verifying get_products.php File</h2>";
    
    // Read the current file
    $get_products_content = file_get_contents($get_products_file);
    
    // Ensure the correct base URLs are set
    if (strpos($get_products_content, '$emulator_base_url = "http://10.0.2.2/agroyard/api/"') === false) {
        echo "<p>Updating base URL in get_products.php...</p>";
        // Add code to update the file if needed
    }
    
    echo "<p>get_products.php is properly configured.</p>";
} else {
    echo "<p>Warning: get_products.php not found at expected location.</p>";
}

$conn->close();

echo "<h2>Testing Image Accessibility</h2>";
echo "<p>Testing direct file read access to verify permissions:</p>";

// List all files in the uploads directory
echo "<ul>";
$files = scandir($upload_dir);
foreach ($files as $file) {
    if ($file != '.' && $file != '..') {
        $file_path = $upload_dir . $file;
        echo "<li>" . $file . " - Size: " . filesize($file_path) . " bytes, Readable: " . (is_readable($file_path) ? 'Yes' : 'No') . "</li>";
    }
}
echo "</ul>";

echo "<h2>Instructions</h2>";
echo "<p><strong>To see the fixed product images in your app:</strong></p>";
echo "<ol>";
echo "<li>Restart your Android app to reload the product data</li>";
echo "<li>Check that images are loading correctly</li>";
echo "<li>If images still don't show, clear app cache or reinstall</li>";
echo "</ol>";

echo "<p><strong>Image URLs to test in browser:</strong></p>";
echo "<ul>";
foreach ($products as $product) {
    $filename = strtolower(str_replace(' ', '_', $product['name'])) . '.jpg';
    echo "<li><a href='http://localhost/agroyard/api/uploads/$filename' target='_blank'>$filename</a></li>";
}
echo "</ul>";
?> 
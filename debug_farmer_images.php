<?php
// Enable error reporting for debugging
ini_set('display_errors', 1);
error_reporting(E_ALL);

header('Content-Type: text/html');

echo "<html>
<head>
    <title>Farmer Uploaded Images Debugger</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .image-card { border: 1px solid #ddd; margin: 10px; padding: 15px; border-radius: 5px; display: inline-block; width: 350px; vertical-align: top; }
        .image-preview { width: 100%; height: 250px; object-fit: cover; border: 1px solid #ccc; }
        .success { color: green; font-weight: bold; }
        .error { color: red; font-weight: bold; }
        .warning { color: orange; font-weight: bold; }
        pre { background: #f5f5f5; padding: 10px; border-radius: 5px; overflow: auto; }
    </style>
</head>
<body>
    <h1>Farmer Uploaded Images Debugger</h1>";

// Define the upload directory
$upload_dir = 'C:/xampp/htdocs/agroyard/api/uploads/';
$image_base_url = 'http://localhost/agroyard/api/uploads/';
$android_base_url = 'http://10.0.2.2/agroyard/api/uploads/';

// Check if the directory exists
if (!is_dir($upload_dir)) {
    echo "<div class='error'>The uploads directory does not exist at: $upload_dir</div>";
    echo "<p>Please make sure the directory exists and has correct permissions.</p>";
    exit;
}

// Check directory permissions
if (!is_readable($upload_dir)) {
    echo "<div class='error'>Cannot read the uploads directory. Check permissions.</div>";
    exit;
}

// Connect to MySQL
$servername = "localhost";
$username = "root";
$password = "";
$dbname = "agroyard";

$conn = new mysqli($servername, $username, $password);
if ($conn->connect_error) {
    echo "<div class='error'>MySQL Connection failed: " . $conn->connect_error . "</div>";
} else {
    echo "<div class='success'>MySQL connection successful!</div>";
}

// Check if database exists
if (!$conn->select_db($dbname)) {
    echo "<div class='warning'>Database '$dbname' does not exist.</div>";
    echo "<p>You need to create the database first.</p>";
    $conn->close();
    exit;
}

// Get all files from the uploads directory
$files = scandir($upload_dir);
$image_files = [];

foreach ($files as $file) {
    // Skip . and .. directories
    if ($file == '.' || $file == '..') continue;
    
    // Only include image files
    $extension = strtolower(pathinfo($file, PATHINFO_EXTENSION));
    $image_extensions = ['jpg', 'jpeg', 'png', 'gif'];
    
    if (in_array($extension, $image_extensions)) {
        $file_path = $upload_dir . $file;
        $file_size = filesize($file_path);
        $file_modified = date("Y-m-d H:i:s", filemtime($file_path));
        
        $image_files[] = [
            'name' => $file,
            'path' => $file_path,
            'size' => $file_size,
            'modified' => $file_modified,
            'web_url' => $image_base_url . $file,
            'android_url' => $android_base_url . $file
        ];
    }
}

echo "<h2>Found " . count($image_files) . " image files in uploads directory</h2>";

// Get entries from the database that have images
$sql = "SELECT id, product_name, farmer_name, price, image_filename FROM products WHERE image_filename IS NOT NULL AND image_filename != ''";
$result = $conn->query($sql);

$db_images = [];
if ($result && $result->num_rows > 0) {
    while ($row = $result->fetch_assoc()) {
        $db_images[$row['image_filename']] = $row;
    }
    echo "<div class='success'>Found " . count($db_images) . " products with images in database</div>";
} else {
    echo "<div class='warning'>No products with images found in database</div>";
}

// Display all image files with their details
echo "<h2>Images in Upload Directory</h2>";

foreach ($image_files as $image) {
    echo "<div class='image-card'>";
    echo "<h3>" . htmlspecialchars($image['name']) . "</h3>";
    
    // Show image preview
    echo "<img src='" . $image['web_url'] . "' class='image-preview' alt='Preview of " . htmlspecialchars($image['name']) . "' />";
    
    // Show image details
    echo "<p><strong>Size:</strong> " . round($image['size'] / 1024, 2) . " KB</p>";
    echo "<p><strong>Last Modified:</strong> " . $image['modified'] . "</p>";
    
    // URLs
    echo "<p><strong>Web URL:</strong><br><a href='" . $image['web_url'] . "' target='_blank'>" . $image['web_url'] . "</a></p>";
    echo "<p><strong>Android URL:</strong><br>" . $image['android_url'] . "</p>";
    
    // Check if image is linked to a product in the database
    if (isset($db_images[$image['name']])) {
        $product = $db_images[$image['name']];
        echo "<div class='success'>✓ Used by product in database</div>";
        echo "<p><strong>Product ID:</strong> " . $product['id'] . "</p>";
        echo "<p><strong>Product Name:</strong> " . htmlspecialchars($product['product_name']) . "</p>";
        echo "<p><strong>Farmer:</strong> " . htmlspecialchars($product['farmer_name']) . "</p>";
        echo "<p><strong>Price:</strong> ₹" . $product['price'] . "/kg</p>";
    } else {
        echo "<div class='warning'>⚠ Not used by any product in database</div>";
    }
    
    echo "</div>";
}

// Show products that have missing images
echo "<h2>Products with Missing Images</h2>";

$missing_images = false;

foreach ($db_images as $filename => $product) {
    $found = false;
    foreach ($image_files as $image) {
        if ($image['name'] == $filename) {
            $found = true;
            break;
        }
    }
    
    if (!$found) {
        $missing_images = true;
        echo "<div class='image-card'>";
        echo "<h3>Missing: " . htmlspecialchars($filename) . "</h3>";
        echo "<div class='error'>❌ File not found in uploads directory</div>";
        echo "<p><strong>Product ID:</strong> " . $product['id'] . "</p>";
        echo "<p><strong>Product Name:</strong> " . htmlspecialchars($product['product_name']) . "</p>";
        echo "<p><strong>Farmer:</strong> " . htmlspecialchars($product['farmer_name']) . "</p>";
        echo "</div>";
    }
}

if (!$missing_images) {
    echo "<p class='success'>✓ All products have their images in the uploads directory</p>";
}

// Android App Instructions
echo "<h2>How to Fix Image Display in Android App</h2>";
echo "<ol>
    <li>Make sure image filenames in the database match the actual files</li>
    <li>The Android app should be accessing images via <code>http://10.0.2.2/agroyard/api/uploads/[filename]</code></li>
    <li>Images should be accessible directly in a browser at <code>http://localhost/agroyard/api/uploads/[filename]</code></li>
    <li>If images are not showing, run the create_test_upload.php script to generate sample products with images</li>
</ol>";

$conn->close();
echo "</body></html>";
?> 
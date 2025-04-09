<?php
// Enable error reporting for debugging
ini_set('display_errors', 1);
error_reporting(E_ALL);

header('Content-Type: text/html');

echo "<html>
<head>
    <title>Fix Product Images</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .product-card { border: 1px solid #ddd; margin: 10px; padding: 15px; border-radius: 5px; max-width: 600px; }
        .image-preview { max-width: 300px; border: 1px solid #ccc; margin: 10px 0; }
        .success { color: green; font-weight: bold; }
        .error { color: red; font-weight: bold; }
        .warning { color: orange; font-weight: bold; }
    </style>
</head>
<body>
    <h1>Fix Product Images</h1>";

// Database configuration
$servername = "localhost";
$username = "root";
$password = "";
$dbname = "agroyard";

// Path configuration
$upload_dir = 'C:/xampp/htdocs/agroyard/api/uploads/';
$app_base_url = 'http://10.0.2.2/agroyard/api/';
$web_base_url = 'http://localhost/agroyard/api/';

// Check if uploads directory exists and is writable
if (!is_dir($upload_dir)) {
    mkdir($upload_dir, 0777, true);
    echo "<p class='success'>Created uploads directory at: $upload_dir</p>";
} elseif (!is_writable($upload_dir)) {
    echo "<p class='error'>Uploads directory exists but is not writable. Please check permissions.</p>";
    exit;
} else {
    echo "<p class='success'>Uploads directory exists and is writable.</p>";
}

// Connect to database
$conn = new mysqli($servername, $username, $password, $dbname);
if ($conn->connect_error) {
    die("<p class='error'>Connection failed: " . $conn->connect_error . "</p>");
}
echo "<p class='success'>Connected to MySQL database successfully.</p>";

// Get all products
$sql = "SELECT id, product_name, farmer_name, image_path FROM products";
$result = $conn->query($sql);

if ($result && $result->num_rows > 0) {
    echo "<p>Found " . $result->num_rows . " products in database.</p>";
    
    echo "<h2>Product Images</h2>";
    
    while ($row = $result->fetch_assoc()) {
        $id = $row['id'];
        $product_name = $row['product_name'];
        $farmer_name = $row['farmer_name'];
        $image_path = $row['image_path'];
        
        echo "<div class='product-card'>";
        echo "<h3>Product ID: $id - $product_name</h3>";
        echo "<p>Farmer: $farmer_name</p>";
        echo "<p>Image path in DB: $image_path</p>";
        
        // Generate URLs for testing
        $full_image_path = $upload_dir . basename($image_path);
        $android_url = $app_base_url . $image_path;
        $web_url = $web_base_url . $image_path;
        
        // Check if image exists
        if (file_exists($full_image_path)) {
            echo "<p class='success'>Image file exists: " . basename($image_path) . "</p>";
            echo "<p>File size: " . round(filesize($full_image_path) / 1024, 2) . " KB</p>";
            echo "<img src='$web_url' class='image-preview' alt='$product_name'>";
        } else {
            echo "<p class='error'>Image file does not exist: $full_image_path</p>";
            
            // Try to fix by checking if file exists with different case
            $found = false;
            if (is_dir(dirname($full_image_path))) {
                $files = scandir(dirname($full_image_path));
                foreach ($files as $file) {
                    if (strtolower($file) === strtolower(basename($image_path))) {
                        $correct_path = 'uploads/' . $file;
                        echo "<p class='warning'>Found file with different case: $file</p>";
                        
                        // Update database with correct path
                        $update_sql = "UPDATE products SET image_path = ? WHERE id = ?";
                        $stmt = $conn->prepare($update_sql);
                        $stmt->bind_param("si", $correct_path, $id);
                        
                        if ($stmt->execute()) {
                            echo "<p class='success'>Updated database with correct path: $correct_path</p>";
                            $image_path = $correct_path;
                            $web_url = $web_base_url . $correct_path;
                            $android_url = $app_base_url . $correct_path;
                            $found = true;
                        } else {
                            echo "<p class='error'>Failed to update database: " . $conn->error . "</p>";
                        }
                        $stmt->close();
                        break;
                    }
                }
            }
            
            if (!$found) {
                echo "<p>No matching file found in uploads directory.</p>";
            }
        }
        
        echo "<p>URLs for this image:</p>";
        echo "<ul>";
        echo "<li>Web browser URL: <a href='$web_url' target='_blank'>$web_url</a></li>";
        echo "<li>Android app URL: $android_url</li>";
        echo "</ul>";
        
        echo "</div>";
    }
} else {
    echo "<p class='warning'>No products found in database or error: " . $conn->error . "</p>";
}

// Fix get_products.php to handle image_path correctly
$get_products_file = 'C:/xampp/htdocs/agroyard/api/get_products.php';
if (file_exists($get_products_file)) {
    echo "<h2>Checking get_products.php</h2>";
    
    $file_contents = file_get_contents($get_products_file);
    
    // Check if the file contains image_path instead of image_filename
    if (strpos($file_contents, 'image_path') === false && strpos($file_contents, 'image_filename') !== false) {
        echo "<p class='warning'>get_products.php is using 'image_filename' but your database uses 'image_path'</p>";
        echo "<p>You should update get_products.php to use 'image_path' instead of 'image_filename'</p>";
    } else {
        echo "<p class='success'>get_products.php appears to be configured correctly.</p>";
    }
}

// Instructions for Android app
echo "<h2>Android App Configuration</h2>";
echo "<p>To make sure your Android app displays the images correctly:</p>";
echo "<ol>";
echo "<li>Ensure your <strong>ProductsFragment.java</strong> is setting the URL correctly in the model:</li>";
echo "<pre>
// In ProductsFragment.java where you process the JSON response:
Product product = new Product(jsonObject);
</pre>";

echo "<li>Check your <strong>Product.java</strong> model to ensure it's using the correct field name:</li>";
echo "<pre>
// In Product.java constructor:
if (jsonObject.has(\"image_path\")) {
    this.imageFilename = jsonObject.getString(\"image_path\");
    // Either store the full URL or just the filename
}
</pre>";

echo "<li>Ensure your <strong>ProductAdapter.java</strong> is constructing the URL correctly:</li>";
echo "<pre>
// In ProductAdapter.java:
String imageFilename = product.getImageFilename();
if (imageFilename != null && !imageFilename.isEmpty()) {
    // Construct image URL
    String imageUrl = \"http://10.0.2.2/agroyard/api/\" + imageFilename;
    // Load with Glide
    Glide.with(context).load(imageUrl).into(holder.productImage);
}
</pre>";
echo "</ol>";

$conn->close();
echo "</body></html>";
?> 
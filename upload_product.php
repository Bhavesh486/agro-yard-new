<?php
// Enable error reporting for debugging
ini_set('display_errors', 1);
error_reporting(E_ALL);

// Set content type to JSON
header('Content-Type: application/json');

// Define upload directory - Make sure this exists and is writable
$upload_dir = __DIR__ . '/uploads/';

// Create the uploads directory if it doesn't exist
if (!is_dir($upload_dir)) {
    mkdir($upload_dir, 0777, true);
}

// Check if uploads directory is writable
if (!is_writable($upload_dir)) {
    header('HTTP/1.1 500 Internal Server Error');
    echo json_encode([
        'success' => false,
        'message' => 'Upload directory is not writable: ' . $upload_dir,
        'debug_info' => [
            'upload_dir' => $upload_dir,
            'is_dir' => is_dir($upload_dir),
            'writable' => is_writable($upload_dir)
        ]
    ]);
    exit;
}

// Database connection
$servername = "localhost";
$username = "root";
$password = "";
$dbname = "agroyard";

$conn = new mysqli($servername, $username, $password, $dbname);

// Check connection
if ($conn->connect_error) {
    header('HTTP/1.1 500 Internal Server Error');
    echo json_encode([
        'success' => false,
        'message' => 'Database connection failed: ' . $conn->connect_error
    ]);
    exit;
}

// Get the raw POST data
$json_data = file_get_contents('php://input');
$data = json_decode($json_data, true);

// Check if data is valid
if (!$data) {
    header('HTTP/1.1 400 Bad Request');
    echo json_encode([
        'success' => false,
        'message' => 'Invalid JSON data',
        'debug_info' => [
            'received_length' => strlen($json_data),
            'json_error' => json_last_error_msg()
        ]
    ]);
    exit;
}

// Extract data
$farmer_name = $data['farmer_name'] ?? '';
$farmer_mobile = $data['farmer_mobile'] ?? '';
$product_name = $data['product_name'] ?? '';
$harvesting_date = $data['harvesting_date'] ?? '';
$farming_type = $data['farming_type'] ?? '';
$quantity = floatval($data['quantity'] ?? 0);
$price = floatval($data['price'] ?? 0);
$expected_price = floatval($data['expected_price'] ?? 0);
$description = $data['description'] ?? '';
$image_base64 = $data['image_base64'] ?? '';
$register_for_bidding = true;

// Validate required fields
if (empty($farmer_name) || empty($product_name) || empty($harvesting_date) || empty($farming_type) || 
    $quantity <= 0 || $price <= 0 || empty($description) || empty($image_base64)) {
    header('HTTP/1.1 400 Bad Request');
    echo json_encode([
        'success' => false,
        'message' => 'Missing required fields',
        'debug_info' => [
            'farmer_name' => empty($farmer_name) ? 'Missing' : 'OK',
            'product_name' => empty($product_name) ? 'Missing' : 'OK',
            'harvesting_date' => empty($harvesting_date) ? 'Missing' : 'OK',
            'farming_type' => empty($farming_type) ? 'Missing' : 'OK',
            'quantity' => $quantity <= 0 ? 'Invalid' : 'OK',
            'price' => $price <= 0 ? 'Invalid' : 'OK',
            'description' => empty($description) ? 'Missing' : 'OK',
            'image_base64' => empty($image_base64) ? 'Missing' : 'OK'
        ]
    ]);
    exit;
}

// Process and save the image
$image_data = base64_decode(preg_replace('#^data:image/\w+;base64,#i', '', $image_base64));
if (!$image_data) {
    header('HTTP/1.1 400 Bad Request');
    echo json_encode([
        'success' => false,
        'message' => 'Invalid image data'
    ]);
    exit;
}

// Generate a unique filename
$image_filename = uniqid() . '.jpg';
$image_path = 'uploads/' . $image_filename;
$full_path = $upload_dir . $image_filename;

// Save the file
$file_saved = file_put_contents($full_path, $image_data);
if (!$file_saved) {
    header('HTTP/1.1 500 Internal Server Error');
    echo json_encode([
        'success' => false,
        'message' => 'Failed to save image',
        'debug_info' => [
            'full_path' => $full_path,
            'error' => error_get_last()
        ]
    ]);
    exit;
}

// Insert product into database
$stmt = $conn->prepare("INSERT INTO products (farmer_name, farmer_mobile, product_name, harvesting_date, farming_type, quantity, price, expected_price, description, image_path, register_for_bidding, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())");

if (!$stmt) {
    header('HTTP/1.1 500 Internal Server Error');
    echo json_encode([
        'success' => false,
        'message' => 'Prepare statement failed: ' . $conn->error
    ]);
    unlink($full_path); // Delete the uploaded file
    exit;
}

$register_for_bidding_int = $register_for_bidding ? 1 : 0;
$stmt->bind_param("sssssdddssi", $farmer_name, $farmer_mobile, $product_name, $harvesting_date, $farming_type, $quantity, $price, $expected_price, $description, $image_path, $register_for_bidding_int);

if (!$stmt->execute()) {
    header('HTTP/1.1 500 Internal Server Error');
    echo json_encode([
        'success' => false,
        'message' => 'Database insert failed: ' . $stmt->error
    ]);
    unlink($full_path); // Delete the uploaded file
    exit;
}

// Success response
echo json_encode([
    'success' => true,
    'message' => 'Product uploaded successfully',
    'product_id' => $conn->insert_id,
    'image_path' => $image_path
]);

// Close connection
$stmt->close();
$conn->close();
?> 
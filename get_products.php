<?php
// Enable error reporting for debugging
ini_set('display_errors', 1);
error_reporting(E_ALL);

// Set content type to JSON
header('Content-Type: application/json');

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

// Get products from database
$query = "SELECT * FROM products ORDER BY created_at DESC";
$result = $conn->query($query);

if (!$result) {
    echo json_encode([
        'success' => false,
        'message' => 'Error fetching products: ' . $conn->error
    ]);
    exit;
}

$products = [];
while ($row = $result->fetch_assoc()) {
    // Check if image_path exists, if not use older field names for backwards compatibility
    if (empty($row['image_path']) && !empty($row['image_filename'])) {
        $row['image_path'] = 'uploads/' . $row['image_filename'];
    }
    
    // Convert numeric values to proper types
    $row['id'] = (int) $row['id'];
    $row['quantity'] = (float) $row['quantity'];
    $row['price'] = (float) $row['price'];
    $row['expected_price'] = (float) $row['expected_price'];
    
    // Handle register_for_bidding field (default to true if not set)
    $row['register_for_bidding'] = isset($row['register_for_bidding']) ? (bool) $row['register_for_bidding'] : true;
    
    // Add product to array
    $products[] = $row;
}

// Return successful response
echo json_encode([
    'success' => true,
    'count' => count($products),
    'products' => $products,
    'server_time' => date('Y-m-d H:i:s')
]);

// Close connection
$conn->close();
?> 

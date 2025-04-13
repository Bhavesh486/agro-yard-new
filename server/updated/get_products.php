<?php
// Enable error reporting for development (disable in production)
ini_set('display_errors', 1);
error_reporting(E_ALL);

// Set JSON content type
header('Content-Type: application/json');

use 'Product.php';

try {
    $productObj = new Product();
    $products = $productObj->getAllProducts();

    echo json_encode([
        'success' => true,
        'count' => count($products),
        'products' => $products,
        'server_time' => date('Y-m-d H:i:s')
    ]);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => $e->getMessage()
    ]);
}
?>

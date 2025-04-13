<?php
ini_set('display_errors', 1);
error_reporting(E_ALL);
header('Content-Type: application/json');

use 'ProductUploader.php';

try {
    $json_data = file_get_contents('php://input');
    $data = json_decode($json_data, true);

    if (!$data) {
        throw new Exception('Invalid JSON: ' . json_last_error_msg());
    }

    $uploader = new ProductUploader();
    $result = $uploader->handleUpload($data);

    echo json_encode([
        'success' => true,
        'message' => 'Product uploaded successfully.',
        'data' => $result
    ]);

} catch (Exception $e) {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'message' => $e->getMessage()
    ]);
}

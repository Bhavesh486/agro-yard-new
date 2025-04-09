<?php
// Enable error reporting for debugging
ini_set('display_errors', 1);
error_reporting(E_ALL);

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

// Get the image filename from the request
$filename = isset($_GET['filename']) ? $_GET['filename'] : '';

if (empty($filename)) {
    echo json_encode([
        'success' => false,
        'message' => 'No filename provided'
    ]);
    exit;
}

// Define paths
$upload_dir = "uploads/";
$physical_upload_path = $_SERVER['DOCUMENT_ROOT'] . "/agroyard/api/" . $upload_dir;
$emulator_base_url = "http://10.0.2.2/agroyard/api/" . $upload_dir;
$browser_base_url = "http://localhost/agroyard/api/" . $upload_dir;

// Check if the directory exists
if (!is_dir($physical_upload_path)) {
    echo json_encode([
        'success' => false,
        'message' => 'Upload directory does not exist: ' . $physical_upload_path,
        'directory_info' => [
            'path' => $physical_upload_path,
            'parent_exists' => is_dir(dirname($physical_upload_path)),
            'parent_readable' => is_readable(dirname($physical_upload_path))
        ]
    ]);
    exit;
}

// Full path to the image file
$image_path = $physical_upload_path . $filename;

// Check if the file exists
$file_exists = file_exists($image_path);

// Get file details if it exists
$file_details = [];
if ($file_exists) {
    $file_details = [
        'size' => filesize($image_path),
        'readable' => is_readable($image_path),
        'permissions' => substr(sprintf('%o', fileperms($image_path)), -4),
        'mime_type' => mime_content_type($image_path),
        'last_modified' => date('Y-m-d H:i:s', filemtime($image_path))
    ];
}

// Construct URLs for accessing the image
$emulator_url = $emulator_base_url . $filename;
$browser_url = $browser_base_url . $filename;

// Return the results
echo json_encode([
    'success' => $file_exists,
    'filename' => $filename,
    'file_exists' => $file_exists,
    'physical_path' => $image_path,
    'emulator_url' => $emulator_url,
    'browser_url' => $browser_url,
    'file_details' => $file_details,
    'directory_info' => [
        'path' => $physical_upload_path,
        'exists' => is_dir($physical_upload_path),
        'readable' => is_readable($physical_upload_path),
        'writable' => is_writable($physical_upload_path),
        'permissions' => substr(sprintf('%o', fileperms($physical_upload_path)), -4),
        'files_count' => count(glob($physical_upload_path . "*")),
        'files_list' => array_map('basename', glob($physical_upload_path . "*"))
    ]
]);
?> 
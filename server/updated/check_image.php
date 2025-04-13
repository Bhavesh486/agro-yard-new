<?php
ini_set('display_errors', 1);
error_reporting(E_ALL);

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

// ==== CONFIG ====
define('UPLOAD_DIR', 'uploads/');
define('API_BASE_PATH', '/agroyard/api/');
define('PHYSICAL_UPLOAD_PATH', $_SERVER['DOCUMENT_ROOT'] . API_BASE_PATH . UPLOAD_DIR);
define('EMULATOR_URL', 'http://10.0.2.2' . API_BASE_PATH . UPLOAD_DIR);
define('BROWSER_URL', 'http://agroyard.42web.io' . API_BASE_PATH . UPLOAD_DIR);

// ==== INPUT ====
$filename = isset($_GET['filename']) ? basename($_GET['filename']) : '';

if (empty($filename)) {
    respond(false, 'No filename provided');
}

// ==== VALIDATE DIRECTORY ====
if (!is_dir(PHYSICAL_UPLOAD_PATH)) {
    respond(false, 'Upload directory does not exist', [
        'path' => PHYSICAL_UPLOAD_PATH,
        'parent_exists' => is_dir(dirname(PHYSICAL_UPLOAD_PATH)),
        'parent_readable' => is_readable(dirname(PHYSICAL_UPLOAD_PATH))
    ]);
}

// ==== FILE LOGIC ====
$image_path = PHYSICAL_UPLOAD_PATH . $filename;
$file_exists = file_exists($image_path);

$response = [
    'filename' => $filename,
    'file_exists' => $file_exists,
    'physical_path' => $image_path,
    'emulator_url' => EMULATOR_URL . $filename,
    'browser_url' => BROWSER_URL . $filename,
    'directory_info' => getDirectoryInfo()
];

if ($file_exists) {
    $response['file_details'] = [
        'size' => filesize($image_path),
        'readable' => is_readable($image_path),
        'permissions' => substr(sprintf('%o', fileperms($image_path)), -4),
        'mime_type' => mime_content_type($image_path),
        'last_modified' => date('Y-m-d H:i:s', filemtime($image_path))
    ];
}

respond($file_exists, $file_exists ? 'File found' : 'File not found', $response);

// ==== FUNCTIONS ====
function respond($success, $message, $data = []) {
    echo json_encode(array_merge([
        'success' => $success,
        'message' => $message
    ], $data));
    exit;
}

function getDirectoryInfo() {
    $files = glob(PHYSICAL_UPLOAD_PATH . '*');
    return [
        'path' => PHYSICAL_UPLOAD_PATH,
        'exists' => is_dir(PHYSICAL_UPLOAD_PATH),
        'readable' => is_readable(PHYSICAL_UPLOAD_PATH),
        'writable' => is_writable(PHYSICAL_UPLOAD_PATH),
        'permissions' => substr(sprintf('%o', fileperms(PHYSICAL_UPLOAD_PATH)), -4),
        'files_count' => count($files),
        'files_list' => array_map('basename', $files)
    ];
}

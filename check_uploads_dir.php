<?php
// Enable error reporting for debugging
ini_set('display_errors', 1);
error_reporting(E_ALL);

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

// Define the upload directory
$upload_dir = $_SERVER['DOCUMENT_ROOT'] . '/agroyard/api/uploads/';

// Result array
$result = [
    'success' => false,
    'message' => '',
    'directory_info' => [],
    'test_write' => [],
    'server_info' => []
];

// Check if directory exists
if (!is_dir($upload_dir)) {
    // Try to create it
    $mkdir_result = mkdir($upload_dir, 0777, true);
    $result['directory_info']['created'] = $mkdir_result;
    
    if ($mkdir_result) {
        chmod($upload_dir, 0777);
        $result['message'] = "Created directory: " . $upload_dir;
    } else {
        $result['message'] = "Failed to create directory: " . $upload_dir;
        $result['directory_info']['error'] = error_get_last();
    }
} else {
    $result['message'] = "Directory exists: " . $upload_dir;
    
    // Try to update permissions
    chmod($upload_dir, 0777);
}

// Check directory permissions
$result['directory_info'] = [
    'path' => $upload_dir,
    'exists' => is_dir($upload_dir),
    'readable' => is_readable($upload_dir),
    'writable' => is_writable($upload_dir),
    'permissions' => substr(sprintf('%o', fileperms($upload_dir)), -4),
    'owner' => function_exists('posix_getpwuid') ? 
        posix_getpwuid(fileowner($upload_dir))['name'] : 
        fileowner($upload_dir),
    'group' => function_exists('posix_getgrgid') ? 
        posix_getgrgid(filegroup($upload_dir))['name'] : 
        filegroup($upload_dir)
];

// Try to write a test image
$test_filename = 'test_image_' . time() . '.jpg';
$test_file_path = $upload_dir . $test_filename;

try {
    // Create a simple test image
    $img = imagecreatetruecolor(200, 200);
    $bg_color = imagecolorallocate($img, 255, 100, 100);
    $text_color = imagecolorallocate($img, 255, 255, 255);
    
    imagefilledrectangle($img, 0, 0, 199, 199, $bg_color);
    imagestring($img, 5, 50, 90, 'Test Image', $text_color);
    
    // Save the image
    $save_result = imagejpeg($img, $test_file_path, 90);
    imagedestroy($img);
    
    if ($save_result) {
        chmod($test_file_path, 0644);
        $result['success'] = true;
        $result['test_write'] = [
            'filename' => $test_filename,
            'path' => $test_file_path,
            'success' => true,
            'exists' => file_exists($test_file_path),
            'readable' => is_readable($test_file_path),
            'writable' => is_writable($test_file_path),
            'size' => filesize($test_file_path),
            'permissions' => substr(sprintf('%o', fileperms($test_file_path)), -4),
            'url' => 'http://10.0.2.2/agroyard/api/uploads/' . $test_filename,
            'browser_url' => 'http://localhost/agroyard/api/uploads/' . $test_filename
        ];
    } else {
        $result['test_write'] = [
            'success' => false,
            'error' => 'Failed to save image',
            'error_details' => error_get_last()
        ];
    }
} catch (Exception $e) {
    $result['test_write'] = [
        'success' => false,
        'error' => $e->getMessage()
    ];
}

// List files in the directory
$files = [];
if (is_dir($upload_dir) && is_readable($upload_dir)) {
    $files_list = scandir($upload_dir);
    foreach ($files_list as $file) {
        if ($file != '.' && $file != '..') {
            $file_path = $upload_dir . $file;
            $files[] = [
                'name' => $file,
                'size' => filesize($file_path),
                'modified' => date('Y-m-d H:i:s', filemtime($file_path)),
                'permissions' => substr(sprintf('%o', fileperms($file_path)), -4),
                'readable' => is_readable($file_path),
                'url' => 'http://10.0.2.2/agroyard/api/uploads/' . $file
            ];
        }
    }
}
$result['directory_info']['files'] = $files;
$result['directory_info']['file_count'] = count($files);

// Server information
$result['server_info'] = [
    'document_root' => $_SERVER['DOCUMENT_ROOT'],
    'server_software' => $_SERVER['SERVER_SOFTWARE'],
    'php_version' => phpversion(),
    'gd_installed' => function_exists('imagecreatetruecolor'),
    'gd_version' => function_exists('gd_info') ? gd_info()['GD Version'] : 'Unknown',
    'max_upload_size' => ini_get('upload_max_filesize'),
    'post_max_size' => ini_get('post_max_size'),
    'memory_limit' => ini_get('memory_limit'),
    'open_basedir' => ini_get('open_basedir'),
    'disable_functions' => ini_get('disable_functions')
];

echo json_encode($result, JSON_PRETTY_PRINT);
?> 
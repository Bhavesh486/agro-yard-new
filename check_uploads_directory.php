<?php
// Enable error reporting for debugging
ini_set('display_errors', 1);
error_reporting(E_ALL);

header('Content-Type: application/json');

// Define the upload directory
$upload_dir = $_SERVER['DOCUMENT_ROOT'] . '/agroyard/api/uploads/';
$result = array();

// Check if directory exists
if (!is_dir($upload_dir)) {
    $result['dir_exists'] = false;
    
    // Try to create the directory
    if (mkdir($upload_dir, 0777, true)) {
        $result['dir_created'] = true;
        chmod($upload_dir, 0777); // Ensure it's writable
    } else {
        $result['dir_created'] = false;
        $result['mkdir_error'] = error_get_last();
    }
} else {
    $result['dir_exists'] = true;
}

// Check if directory is writable
$result['is_writable'] = is_writable($upload_dir);

// If not writable, try to make it writable
if (!$result['is_writable']) {
    chmod($upload_dir, 0777);
    $result['chmod_attempt'] = true;
    $result['is_writable_after_chmod'] = is_writable($upload_dir);
}

// List existing files in the directory
$files = glob($upload_dir . '*');
$result['files'] = array();

foreach ($files as $file) {
    $file_info = array(
        'name' => basename($file),
        'size' => filesize($file),
        'is_readable' => is_readable($file),
        'type' => mime_content_type($file),
        'last_modified' => date('Y-m-d H:i:s', filemtime($file))
    );
    $result['files'][] = $file_info;
}

// Create a test image if no placeholder exists
$placeholder_path = $upload_dir . 'placeholder.jpg';
if (!file_exists($placeholder_path)) {
    $result['placeholder_exists'] = false;
    
    // Create a simple colored image
    $width = 400;
    $height = 300;
    $image = imagecreatetruecolor($width, $height);
    
    // Fill with a gradient
    $red = imagecolorallocate($image, 255, 100, 100);
    $blue = imagecolorallocate($image, 100, 100, 255);
    
    for ($i = 0; $i < $width; $i++) {
        $color = imagecolorallocate($image, 
            100 + ($i / $width) * 155,
            100,
            100 + (1 - $i / $width) * 155
        );
        imagefilledrectangle($image, $i, 0, $i, $height, $color);
    }
    
    // Add text
    $white = imagecolorallocate($image, 255, 255, 255);
    $text = "Product Image";
    $font_size = 5;
    
    $text_width = imagefontwidth($font_size) * strlen($text);
    $text_height = imagefontheight($font_size);
    
    $x = ($width - $text_width) / 2;
    $y = ($height - $text_height) / 2;
    
    imagestring($image, $font_size, $x, $y, $text, $white);
    
    // Save the image
    if (imagejpeg($image, $placeholder_path, 90)) {
        $result['placeholder_created'] = true;
        chmod($placeholder_path, 0644); // Make it readable by the web server
    } else {
        $result['placeholder_created'] = false;
        $result['placeholder_error'] = error_get_last();
    }
    
    imagedestroy($image);
} else {
    $result['placeholder_exists'] = true;
}

// Server info
$result['server_info'] = array(
    'document_root' => $_SERVER['DOCUMENT_ROOT'],
    'script_filename' => $_SERVER['SCRIPT_FILENAME'],
    'php_version' => phpversion(),
    'gd_installed' => function_exists('imagecreatetruecolor'),
    'upload_max_filesize' => ini_get('upload_max_filesize'),
    'post_max_size' => ini_get('post_max_size')
);

// Output the result
echo json_encode($result, JSON_PRETTY_PRINT);
?> 
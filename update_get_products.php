<?php
// Enable error reporting for debugging
ini_set('display_errors', 1);
error_reporting(E_ALL);

echo "<h1>Updating get_products.php to use image_path</h1>";

// Define the file path
$file_path = 'C:/xampp/htdocs/agroyard/api/get_products.php';

// Check if file exists
if (!file_exists($file_path)) {
    die("<p style='color: red;'>Error: get_products.php not found at $file_path</p>");
}

// Read the current file content
$file_contents = file_get_contents($file_path);
if ($file_contents === false) {
    die("<p style='color: red;'>Error: Could not read get_products.php</p>");
}

echo "<p>Successfully read get_products.php file.</p>";

// Check if the file already uses image_path
if (strpos($file_contents, 'image_path') !== false) {
    echo "<p style='color: green;'>File is already using image_path. No changes needed.</p>";
    exit;
}

// Make the replacements
$updated_content = str_replace(
    ['$imageFilename', 'image_filename'], 
    ['$imagePath', 'image_path'], 
    $file_contents
);

// Additional replacements for the product array
$updated_content = str_replace(
    '"image_filename" => $imageFilename',
    '"image_path" => $imagePath',
    $updated_content
);

// Create backup of original file
$backup_path = $file_path . '.bak';
if (!file_put_contents($backup_path, $file_contents)) {
    die("<p style='color: red;'>Error: Could not create backup file at $backup_path</p>");
}
echo "<p>Created backup of original file at $backup_path</p>";

// Write the updated content back to the file
if (file_put_contents($file_path, $updated_content)) {
    echo "<p style='color: green;'>Successfully updated get_products.php to use image_path instead of image_filename.</p>";
} else {
    echo "<p style='color: red;'>Error: Could not write updated content to get_products.php</p>";
}

// Show a diff of the changes
echo "<h2>Changes Made:</h2>";
echo "<pre style='background-color: #f5f5f5; padding: 10px;'>";
echo "- Replaced 'image_filename' with 'image_path'\n";
echo "- Replaced '\$imageFilename' with '\$imagePath'\n";
echo "</pre>";

echo "<p>The Android app should now be able to display images correctly using the image_path field from your database.</p>";
echo "<p>You may need to rebuild and restart your Android app for the changes to take effect.</p>";
?> 
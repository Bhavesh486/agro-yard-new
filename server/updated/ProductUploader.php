<?php
use 'Product.php';

class ProductUploader {
    private $uploadDir;

    public function __construct() {
        $this->uploadDir = realpath(__DIR__ . '/../uploads/') . '/';

        if (!is_dir($this->uploadDir)) {
            mkdir($this->uploadDir, 0777, true);
        }

        if (!is_writable($this->uploadDir)) {
            throw new Exception("Upload directory is not writable: {$this->uploadDir}");
        }
    }

    public function handleUpload($data) {
        // Validate
        $requiredFields = ['farmer_name', 'product_name', 'harvesting_date', 'farming_type', 'quantity', 'price', 'expected_price', 'description', 'image_base64'];
        foreach ($requiredFields as $field) {
            if (empty($data[$field])) {
                throw new Exception("Missing or invalid field: {$field}");
            }
        }

        // Save image
        $imagePath = $this->saveImage($data['image_base64']);

        // Prepare product data for insertion
        $productData = [
            'farmer_name' => $data['farmer_name'],
            'farmer_mobile' => $data['farmer_mobile'] ?? '',
            'product_name' => $data['product_name'],
            'harvesting_date' => $data['harvesting_date'],
            'farming_type' => $data['farming_type'],
            'quantity' => (float) $data['quantity'],
            'price' => (float) $data['price'],
            'expected_price' => (float) $data['expected_price'],
            'description' => $data['description'],
            'image_path' => $imagePath,
            'register_for_bidding' => isset($data['register_for_bidding']) ? (int) $data['register_for_bidding'] : 1
        ];

        // Insert into DB
        $productModel = new Product();
        $productId = $productModel->insertProduct($productData);

        return [
            'product_id' => $productId,
            'image_path' => $imagePath,
            'full_image_url' => $this->getFullImageUrl($imagePath)
        ];
    }

    private function saveImage($base64) {
        $imageData = base64_decode(preg_replace('#^data:image/\w+;base64,#i', '', $base64));

        if (!$imageData) {
            throw new Exception("Invalid base64 image data.");
        }

        $filename = uniqid('product_') . '.jpg';
        $fullPath = $this->uploadDir . $filename;

        if (!file_put_contents($fullPath, $imageData)) {
            throw new Exception("Failed to save image.");
        }

        return '/agroyard/api/uploads/' . $filename;
    }

    private function getFullImageUrl($imagePath) {
        $protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https' : 'http';
        return $protocol . '://' . $_SERVER['HTTP_HOST'] . $imagePath;
    }
}

<?php
use 'Database.php';

class Product {
    private $conn;

    public function __construct() {
        $db = new Database();
        $this->conn = $db->getConnection();
    }

    public function getAllProducts() {
        $query = "SELECT * FROM products ORDER BY created_at DESC";
        $result = $this->conn->query($query);

        if (!$result) {
            throw new Exception("Error fetching products: " . $this->conn->error);
        }

        $products = [];
        while ($row = $result->fetch_assoc()) {
            // Handle backward compatibility for image
            if (empty($row['image_path']) && !empty($row['image_filename'])) {
                $row['image_path'] = 'uploads/' . $row['image_filename'];
            }

            // Cast numeric and boolean values
            $row['id'] = (int) $row['id'];
            $row['quantity'] = (float) $row['quantity'];
            $row['price'] = (float) $row['price'];
            $row['expected_price'] = (float) $row['expected_price'];
            $row['register_for_bidding'] = isset($row['register_for_bidding']) ? (bool) $row['register_for_bidding'] : true;

            $products[] = $row;
        }

        return $products;
    }

    public function insertProduct($data) {
        $stmt = $this->conn->prepare("
            INSERT INTO products (
                farmer_name, farmer_mobile, product_name, harvesting_date, farming_type, quantity, 
                price, expected_price, description, image_path, register_for_bidding, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
        ");

        if (!$stmt) {
            throw new Exception("Prepare failed: " . $this->conn->error);
        }

        $stmt->bind_param(
            "sssssdddssi",
            $data['farmer_name'],
            $data['farmer_mobile'],
            $data['product_name'],
            $data['harvesting_date'],
            $data['farming_type'],
            $data['quantity'],
            $data['price'],
            $data['expected_price'],
            $data['description'],
            $data['image_path'],
            $data['register_for_bidding']
        );

        if (!$stmt->execute()) {
            throw new Exception("Execute failed: " . $stmt->error);
        }

        return $this->conn->insert_id;
    }
}
?>

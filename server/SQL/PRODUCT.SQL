CREATE TABLE `products` (
  `id` int(11) NOT NULL,
  `farmer_name` varchar(100) NOT NULL,
  `farmer_mobile` varchar(15) NOT NULL,
  `product_name` varchar(100) NOT NULL,
  `harvesting_date` date NOT NULL,
  `farming_type` varchar(50) NOT NULL,
  `quantity` decimal(10,2) NOT NULL,
  `price` decimal(10,2) NOT NULL,
  `expected_price` decimal(10,2) NOT NULL,
  `description` text DEFAULT NULL,
  `image_path` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


-- Add register_for_bidding (boolean, defaults to TRUE)
ALTER TABLE products
ADD COLUMN register_for_bidding TINYINT(1) NOT NULL DEFAULT 1;

-- Add image_filename for backward compatibility (if needed)
ALTER TABLE products
ADD COLUMN image_filename VARCHAR(255) DEFAULT NULL;

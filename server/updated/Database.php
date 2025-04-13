<?php
class Database {
    private $host = "sql210.infinityfree.com"; // Database host
    private $username = "if0_38457966"; // Database username
    private $password = "igcoWyWmNo"; // Database password
    private $dbname = "if0_38457966_agro_yard"; // Database name
    private $conn;

    // Constructor to connect on object creation
    public function __construct() {
        $this->connectDB();
    }

    // Connect to the database
    private function connectDB() {
        $this->conn = new mysqli($this->host, $this->username, $this->password, $this->dbname);

        if ($this->conn->connect_error) {
            die("Connection failed: " . $this->conn->connect_error);
        }
        echo "Database connected successfully.";
        // Optional: echo "Database connected successfully.";
    }

    // Get the connection object
    public function getConnection() {
        return $this->conn;
    }

    // Close the connection
    public function closeConnection() {
        if ($this->conn) {
            $this->conn->close();
        }
    }
}
?>

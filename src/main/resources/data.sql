INSERT INTO membership_grades (grade_name, acc_rate, min_amount)
VALUES ('NORMAL', 1.00, 0.00)
    ON DUPLICATE KEY UPDATE acc_rate = 1.00, min_amount = 0.00;

INSERT INTO membership_grades (grade_name, acc_rate, min_amount)
VALUES ('VIP', 5.00, 50001.00)
    ON DUPLICATE KEY UPDATE acc_rate = 5.00, min_amount = 50001.00;

INSERT INTO membership_grades (grade_name, acc_rate, min_amount)
VALUES ('VVIP', 10.00, 150000.00)
    ON DUPLICATE KEY UPDATE acc_rate = 10.00, min_amount = 150000.00;
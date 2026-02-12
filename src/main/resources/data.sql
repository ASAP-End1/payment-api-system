-- 멤버십 등급
INSERT INTO membership_grades (grade_name, acc_rate, min_amount)
VALUES ('NORMAL', 1.00, 0.00)
ON DUPLICATE KEY UPDATE acc_rate = 1.00, min_amount = 0.00;

INSERT INTO membership_grades (grade_name, acc_rate, min_amount)
VALUES ('VIP', 5.00, 50001.00)
ON DUPLICATE KEY UPDATE acc_rate = 5.00, min_amount = 50001.00;

INSERT INTO membership_grades (grade_name, acc_rate, min_amount)
VALUES ('VVIP', 10.00, 150000.00)
ON DUPLICATE KEY UPDATE acc_rate = 10.00, min_amount = 150000.00;

-- 상품 데이터
-- 전자제품 카테고리
INSERT INTO products (name, price, stock, category, status, created_at, updated_at)
VALUES ('삼성 갤럭시 S24', 1200000.00, 50, '전자제품', 'FOR_SALE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
                     price = 1200000.00,
                     stock = 50,
                     category = '전자제품',
                     status = 'FOR_SALE',
                     updated_at = NOW();

INSERT INTO products (name, price, stock, category, status, created_at, updated_at)
VALUES ('LG 울트라와이드 모니터 34인치', 450000.00, 30, '전자제품', 'FOR_SALE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
                     price = 450000.00,
                     stock = 30,
                     category = '전자제품',
                     status = 'FOR_SALE',
                     updated_at = NOW();

INSERT INTO products (name, price, stock, category, status, created_at, updated_at)
VALUES ('애플 맥북 프로 16인치', 3500000.00, 15, '전자제품', 'FOR_SALE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
                     price = 3500000.00,
                     stock = 15,
                     category = '전자제품',
                     status = 'FOR_SALE',
                     updated_at = NOW();

INSERT INTO products (name, price, stock, category, status, created_at, updated_at)
VALUES ('소니 WH-1000XM5 무선 헤드폰', 380000.00, 100, '전자제품', 'FOR_SALE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
                     price = 380000.00,
                     stock = 100,
                     category = '전자제품',
                     status = 'FOR_SALE',
                     updated_at = NOW();

-- 가전제품 카테고리
INSERT INTO products (name, price, stock, category, status, created_at, updated_at)
VALUES ('다이슨 청소기 V15', 890000.00, 25, '가전제품', 'FOR_SALE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
                     price = 890000.00,
                     stock = 25,
                     category = '가전제품',
                     status = 'FOR_SALE',
                     updated_at = NOW();

INSERT INTO products (name, price, stock, category, status, created_at, updated_at)
VALUES ('삼성 비스포크 냉장고', 2800000.00, 10, '가전제품', 'FOR_SALE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
                     price = 2800000.00,
                     stock = 10,
                     category = '가전제품',
                     status = 'FOR_SALE',
                     updated_at = NOW();

-- 패션 카테고리
INSERT INTO products (name, price, stock, category, status, created_at, updated_at)
VALUES ('나이키 에어맥스 운동화', 159000.00, 200, '패션', 'FOR_SALE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
                     price = 159000.00,
                     stock = 200,
                     category = '패션',
                     status = 'FOR_SALE',
                     updated_at = NOW();

INSERT INTO products (name, price, stock, category, status, created_at, updated_at)
VALUES ('노스페이스 다운 재킷', 320000.00, 80, '패션', 'FOR_SALE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
                     price = 320000.00,
                     stock = 80,
                     category = '패션',
                     status = 'FOR_SALE',
                     updated_at = NOW();

-- 도서 카테고리
INSERT INTO products (name, price, stock, category, status, created_at, updated_at)
VALUES ('클린 코드 (로버트 C. 마틴)', 32000.00, 150, '도서', 'FOR_SALE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
                     price = 32000.00,
                     stock = 150,
                     category = '도서',
                     status = 'FOR_SALE',
                     updated_at = NOW();

INSERT INTO products (name, price, stock, category, status, created_at, updated_at)
VALUES ('이펙티브 자바 3판', 36000.00, 120, '도서', 'FOR_SALE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
                     price = 36000.00,
                     stock = 120,
                     category = '도서',
                     status = 'FOR_SALE',
                     updated_at = NOW();

-- 생활용품 카테고리
INSERT INTO products (name, price, stock, category, status, created_at, updated_at)
VALUES ('코멧 물티슈 20팩', 15000.00, 500, '생활용품', 'FOR_SALE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
                     price = 15000.00,
                     stock = 500,
                     category = '생활용품',
                     status = 'FOR_SALE',
                     updated_at = NOW();

INSERT INTO products (name, price, stock, category, status, created_at, updated_at)
VALUES ('다우니 섬유유연제 대용량', 18000.00, 300, '생활용품', 'FOR_SALE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
                     price = 18000.00,
                     stock = 300,
                     category = '생활용품',
                     status = 'FOR_SALE',
                     updated_at = NOW();

-- 품절 상품 (테스트용)
INSERT INTO products (name, price, stock, category, status, created_at, updated_at)
VALUES ('품절 상품 (테스트)', 50000.00, 0, '테스트', 'SOLD_OUT', NOW(), NOW())
ON DUPLICATE KEY UPDATE
                     price = 50000.00,
                     stock = 0,
                     category = '테스트',
                     status = 'SOLD_OUT',
                     updated_at = NOW();
-- 1. Create Apartment
INSERT INTO apartments (name, address, city, state, pincode, created_at)
VALUES ('Test Apartment 1234', '123 Test Street', 'Test City', 'TS', '500001', CURRENT_TIMESTAMP);

-- 2. Create 22 Flats
INSERT INTO flats (apartment_id, block_name, flat_number, square_footage, bhk_type, status) VALUES
(1, 'D', '101', 1200.0, '2BHK', 'OCCUPIED'),
(1, 'D', '102', 1200.0, '2BHK', 'OCCUPIED'),
(1, 'D', '103', 1200.0, '2BHK', 'OCCUPIED'),
(1, 'D', '104', 1200.0, '2BHK', 'OCCUPIED'),
(1, 'D', '105', 1200.0, '2BHK', 'OCCUPIED'),
(1, 'D', '106', 1200.0, '2BHK', 'OCCUPIED'),
(1, 'D', '107', 1200.0, '2BHK', 'OCCUPIED'),
(1, 'D', '108', 1200.0, '2BHK', 'OCCUPIED'),
(1, 'D', '109', 1200.0, '2BHK', 'OCCUPIED'),
(1, 'D', '110', 1200.0, '2BHK', 'OCCUPIED'),
(1, 'D', '111', 1200.0, '2BHK', 'OCCUPIED'),
(1, 'D', '112', 1200.0, '2BHK', 'OCCUPIED'),
(1, 'D', '113', 1200.0, '2BHK', 'OCCUPIED'),
(1, 'D', '114', 1200.0, '2BHK', 'OCCUPIED'),
(1, 'D', '115', 1200.0, '2BHK', 'OCCUPIED'),
(1, 'D', '116', 1200.0, '2BHK', 'OCCUPIED'),
(1, 'D', '117', 1200.0, '2BHK', 'OCCUPIED'),
(1, 'D', '118', 1200.0, '2BHK', 'OCCUPIED'),
(1, 'D', '119', 1200.0, '2BHK', 'OCCUPIED'),
(1, 'D', '120', 1200.0, '2BHK', 'OCCUPIED'),
(1, 'D', '121', 1200.0, '2BHK', 'OCCUPIED'),
(1, 'D', '122', 1200.0, '2BHK', 'OCCUPIED');

-- 3. Create Admin and Secretary
INSERT INTO users (apartment_id, flat_id, full_name, phone, email, role, approval_status, user_type, owner_status, created_at) VALUES
(1, (SELECT id FROM flats WHERE apartment_id=1 AND block_name='D' AND flat_number='101'), 'Super Admin', '9888888881', 'admin@test.com', 'ADMIN', 'APPROVED', 'Owner', 'Residing', CURRENT_TIMESTAMP),
(1, (SELECT id FROM flats WHERE apartment_id=1 AND block_name='D' AND flat_number='102'), 'Secretary', '9888888882', 'secretary@test.com', 'ADMIN', 'APPROVED', 'Owner', 'Residing', CURRENT_TIMESTAMP);

-- 4. Create 20 Residents
INSERT INTO users (apartment_id, flat_id, full_name, phone, email, role, approval_status, user_type, owner_status, created_at) VALUES
(1, (SELECT id FROM flats WHERE apartment_id=1 AND block_name='D' AND flat_number='103'), 'Test Resident 1', '9000000001', 'resident1@test.com', 'RESIDENT', 'APPROVED', 'Owner', 'Residing', CURRENT_TIMESTAMP),
(1, (SELECT id FROM flats WHERE apartment_id=1 AND block_name='D' AND flat_number='104'), 'Test Resident 2', '9000000002', 'resident2@test.com', 'RESIDENT', 'APPROVED', 'Owner', 'Residing', CURRENT_TIMESTAMP),
(1, (SELECT id FROM flats WHERE apartment_id=1 AND block_name='D' AND flat_number='105'), 'Test Resident 3', '9000000003', 'resident3@test.com', 'RESIDENT', 'APPROVED', 'Owner', 'Residing', CURRENT_TIMESTAMP),
(1, (SELECT id FROM flats WHERE apartment_id=1 AND block_name='D' AND flat_number='106'), 'Test Resident 4', '9000000004', 'resident4@test.com', 'RESIDENT', 'APPROVED', 'Owner', 'Residing', CURRENT_TIMESTAMP),
(1, (SELECT id FROM flats WHERE apartment_id=1 AND block_name='D' AND flat_number='107'), 'Test Resident 5', '9000000005', 'resident5@test.com', 'RESIDENT', 'APPROVED', 'Owner', 'Residing', CURRENT_TIMESTAMP),
(1, (SELECT id FROM flats WHERE apartment_id=1 AND block_name='D' AND flat_number='108'), 'Test Resident 6', '9000000006', 'resident6@test.com', 'RESIDENT', 'APPROVED', 'Owner', 'Residing', CURRENT_TIMESTAMP),
(1, (SELECT id FROM flats WHERE apartment_id=1 AND block_name='D' AND flat_number='109'), 'Test Resident 7', '9000000007', 'resident7@test.com', 'RESIDENT', 'APPROVED', 'Owner', 'Residing', CURRENT_TIMESTAMP),
(1, (SELECT id FROM flats WHERE apartment_id=1 AND block_name='D' AND flat_number='110'), 'Test Resident 8', '9000000008', 'resident8@test.com', 'RESIDENT', 'APPROVED', 'Owner', 'Residing', CURRENT_TIMESTAMP),
(1, (SELECT id FROM flats WHERE apartment_id=1 AND block_name='D' AND flat_number='111'), 'Test Resident 9', '9000000009', 'resident9@test.com', 'RESIDENT', 'APPROVED', 'Owner', 'Residing', CURRENT_TIMESTAMP),
(1, (SELECT id FROM flats WHERE apartment_id=1 AND block_name='D' AND flat_number='112'), 'Test Resident 10', '9000000010', 'resident10@test.com', 'RESIDENT', 'APPROVED', 'Owner', 'Residing', CURRENT_TIMESTAMP),
(1, (SELECT id FROM flats WHERE apartment_id=1 AND block_name='D' AND flat_number='113'), 'Test Resident 11', '9000000011', 'resident11@test.com', 'RESIDENT', 'APPROVED', 'Owner', 'Residing', CURRENT_TIMESTAMP),
(1, (SELECT id FROM flats WHERE apartment_id=1 AND block_name='D' AND flat_number='114'), 'Test Resident 12', '9000000012', 'resident12@test.com', 'RESIDENT', 'APPROVED', 'Owner', 'Residing', CURRENT_TIMESTAMP),
(1, (SELECT id FROM flats WHERE apartment_id=1 AND block_name='D' AND flat_number='115'), 'Test Resident 13', '9000000013', 'resident13@test.com', 'RESIDENT', 'APPROVED', 'Owner', 'Residing', CURRENT_TIMESTAMP),
(1, (SELECT id FROM flats WHERE apartment_id=1 AND block_name='D' AND flat_number='116'), 'Test Resident 14', '9000000014', 'resident14@test.com', 'RESIDENT', 'APPROVED', 'Owner', 'Residing', CURRENT_TIMESTAMP),
(1, (SELECT id FROM flats WHERE apartment_id=1 AND block_name='D' AND flat_number='117'), 'Test Resident 15', '9000000015', 'resident15@test.com', 'RESIDENT', 'APPROVED', 'Owner', 'Residing', CURRENT_TIMESTAMP),
(1, (SELECT id FROM flats WHERE apartment_id=1 AND block_name='D' AND flat_number='118'), 'Test Resident 16', '9000000016', 'resident16@test.com', 'RESIDENT', 'APPROVED', 'Owner', 'Residing', CURRENT_TIMESTAMP),
(1, (SELECT id FROM flats WHERE apartment_id=1 AND block_name='D' AND flat_number='119'), 'Test Resident 17', '9000000017', 'resident17@test.com', 'RESIDENT', 'APPROVED', 'Owner', 'Residing', CURRENT_TIMESTAMP),
(1, (SELECT id FROM flats WHERE apartment_id=1 AND block_name='D' AND flat_number='120'), 'Test Resident 18', '9000000018', 'resident18@test.com', 'RESIDENT', 'APPROVED', 'Owner', 'Residing', CURRENT_TIMESTAMP),
(1, (SELECT id FROM flats WHERE apartment_id=1 AND block_name='D' AND flat_number='121'), 'Test Resident 19', '9000000019', 'resident19@test.com', 'RESIDENT', 'APPROVED', 'Owner', 'Residing', CURRENT_TIMESTAMP),
(1, (SELECT id FROM flats WHERE apartment_id=1 AND block_name='D' AND flat_number='122'), 'Test Resident 20', '9000000020', 'resident20@test.com', 'RESIDENT', 'APPROVED', 'Owner', 'Residing', CURRENT_TIMESTAMP);

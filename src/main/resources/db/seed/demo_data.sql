-- =============================================================================
-- INVENTORY MANAGEMENT SYSTEM - DEMO & PRODUCTION-READY SEED DATA
-- Purpose: Complete, realistic demo dataset covering all business cases,
--          roles, inventory statuses, units of measure, audit trails, and KPIs.
-- =============================================================================

-- 1. CLEANUP ALL TEST DATA & RESET SEQUENCES
TRUNCATE TABLE 
    registration_otps, 
    app_user_tokens, 
    stock_transactions, 
    products, 
    customers, 
    suppliers, 
    categories, 
    app_users 
RESTART IDENTITY CASCADE;

-- =============================================================================
-- 2. USERS (app_users)
-- Passwords:
--   admin         -> Admin@12345
--   manager       -> Manager@12345
--   staff         -> Staff@12345
--   sopheak.chan  -> Staff@12345
--   dara.roth     -> Staff@12345 (must_change_password = true)
--   bopha.heng    -> Staff@12345 (active = false)
-- =============================================================================
INSERT INTO app_users (id, username, email, password, full_name, phone, role, active, must_change_password, created_at, updated_at) VALUES
(1, 'admin', 'schannleab@gmail.com', '$2a$10$sPWDrrq/trXnSfyamls66O1KbQc.ITmORiAF7Xvi7qr7P4B14dL1.', 'System Administrator', '0883784840', 'ADMIN', true, false, CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP - INTERVAL '60 days'),
(2, 'manager', 'manager@inventory.com', '$2a$10$farhsiIpoTZMx9.cry4RCueq48Cv/0Erg8r14y8XVvicmpiEDhzO.', 'Vannak Seng', '012888999', 'MANAGER', true, false, CURRENT_TIMESTAMP - INTERVAL '55 days', CURRENT_TIMESTAMP - INTERVAL '55 days'),
(3, 'staff', 'channleab2023@gmail.com', '$2a$10$hVFW7sE5Y0R84P5jP8hoXuRcG8xwSgZP0YUEBa6IxyGRrSgVc9ILW', 'Channleab Bee', '012345679', 'STAFF', true, false, CURRENT_TIMESTAMP - INTERVAL '50 days', CURRENT_TIMESTAMP - INTERVAL '50 days'),
(4, 'sopheak.chan', 'sopheak.chan@inventory.com', '$2a$10$hVFW7sE5Y0R84P5jP8hoXuRcG8xwSgZP0YUEBa6IxyGRrSgVc9ILW', 'Sopheak Chan', '077234567', 'STAFF', true, false, CURRENT_TIMESTAMP - INTERVAL '40 days', CURRENT_TIMESTAMP - INTERVAL '40 days'),
(5, 'dara.roth', 'dara.roth@inventory.com', '$2a$10$hVFW7sE5Y0R84P5jP8hoXuRcG8xwSgZP0YUEBa6IxyGRrSgVc9ILW', 'Dara Roth', '098765432', 'STAFF', true, true, CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '10 days'),
(6, 'bopha.heng', 'bopha.heng@inventory.com', '$2a$10$hVFW7sE5Y0R84P5jP8hoXuRcG8xwSgZP0YUEBa6IxyGRrSgVc9ILW', 'Bopha Heng', '015432109', 'STAFF', false, false, CURRENT_TIMESTAMP - INTERVAL '35 days', CURRENT_TIMESTAMP - INTERVAL '5 days');

SELECT setval('app_users_id_seq', (SELECT MAX(id) FROM app_users));

-- =============================================================================
-- 3. CATEGORIES (categories)
-- =============================================================================
INSERT INTO categories (id, name, description, created_at, updated_at) VALUES
(1, 'Electronics & Gadgets', 'Laptops, computer peripherals, smart devices, and accessories', CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP - INTERVAL '60 days'),
(2, 'Beverages & Refreshments', 'Soft drinks, bottled mineral water, energy drinks, and dairy cartons', CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP - INTERVAL '60 days'),
(3, 'Packaged Food & Snacks', 'Instant noodles, canned seafood, pantry staples, and dry snacks', CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP - INTERVAL '60 days'),
(4, 'Office Supplies & Stationery', 'Copier paper, writing pens, desk organizers, and filing materials', CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP - INTERVAL '60 days'),
(5, 'Health & Personal Care', 'Medical face masks, hand sanitizers, hygiene essentials, and disinfectants', CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP - INTERVAL '60 days'),
(6, 'Hardware & Industrial Tools', 'Hand tools, cordless power drills, spanner wrench sets, and protective work gear', CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP - INTERVAL '60 days'),
(7, 'Home & Warehouse Furniture', 'Ergonomic office seating, heavy-duty industrial shelving, and storage units', CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP - INTERVAL '60 days'),
(8, 'Packaging & Shipping Materials', 'Corrugated carton boxes, packaging tape rolls, shipping pallets, and heavy bags', CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP - INTERVAL '60 days');

SELECT setval('categories_id_seq', (SELECT MAX(id) FROM categories));

-- =============================================================================
-- 4. SUPPLIERS (suppliers)
-- =============================================================================
INSERT INTO suppliers (id, name, phone, email, address, created_at, updated_at) VALUES
(1, 'Phnom Penh Electronics Hub Co., Ltd.', '023888123', 'contact@ppelectronics.com.kh', '#45, Russian Federation Blvd (St. 110), Toul Kork, Phnom Penh', CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP - INTERVAL '60 days'),
(2, 'Cambodia Beverage & Bottling Corp.', '023777456', 'sales@cambodiabev.com.kh', 'National Road 4, Chaom Chau, Phnom Penh', CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP - INTERVAL '60 days'),
(3, 'Khmer Global Food Distributors', '023999789', 'orders@khmerfooddist.com', '#12B, St. 271, Sangkat Boeng Tumpun, Phnom Penh', CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP - INTERVAL '60 days'),
(4, 'Deli Stationery & Office Supplies Ltd.', '023666234', 'support@delistationery.com.kh', '#88, Preah Monivong Blvd, Doun Penh, Phnom Penh', CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP - INTERVAL '60 days'),
(5, 'Medix Health & Hygiene Supplies', '023555345', 'info@medixkh.com', '#204, Mao Tse Toung Blvd, Chamkar Mon, Phnom Penh', CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP - INTERVAL '60 days'),
(6, 'Total Tools & Hardware Solutions', '023444567', 'sales@totaltools.com.kh', 'Veng Sreng Blvd, Sangkat Stueng Mean Chey, Phnom Penh', CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP - INTERVAL '60 days'),
(7, 'Modern Space Office Furniture', '023333678', 'inquiry@modernspace.com.kh', '#15, Hanoi Friendship Blvd, Sen Sok, Phnom Penh', CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP - INTERVAL '60 days'),
(8, 'EcoPack Industrial Packaging', '023222789', 'orders@ecopack.com.kh', 'Street 2004, Sangkat Teuk Thla, Sen Sok, Phnom Penh', CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP - INTERVAL '60 days');

SELECT setval('suppliers_id_seq', (SELECT MAX(id) FROM suppliers));

-- =============================================================================
-- 5. CUSTOMERS (customers)
-- =============================================================================
INSERT INTO customers (id, name, phone, email, address, active, created_at, updated_at) VALUES
(1, 'Lucky Supermarket Head Office', '012300001', 'procurement@luckysupermarket.com.kh', 'Russian Federation Blvd, Phnom Penh', true, CURRENT_TIMESTAMP - INTERVAL '50 days', CURRENT_TIMESTAMP - INTERVAL '50 days'),
(2, 'Chip Mong Retail & Mega Mall', '012300002', 'inventory@chipmongretail.com', 'Preah Norodom Blvd, Phnom Penh', true, CURRENT_TIMESTAMP - INTERVAL '48 days', CURRENT_TIMESTAMP - INTERVAL '48 days'),
(3, 'Aeon Mall Distribution Center', '012300003', 'logistics@aeonmallcambodia.com', 'Samdach Hun Sen Blvd (St. 60m), Phnom Penh', true, CURRENT_TIMESTAMP - INTERVAL '45 days', CURRENT_TIMESTAMP - INTERVAL '45 days'),
(4, 'Paragon International University', '012300004', 'purchasing@paragoniu.edu.kh', '#8, St. 315, Boeng Kak 1, Toul Kork, Phnom Penh', true, CURRENT_TIMESTAMP - INTERVAL '42 days', CURRENT_TIMESTAMP - INTERVAL '42 days'),
(5, 'Royal University of Phnom Penh (RUPP)', '012300005', 'procurement@rupp.edu.kh', 'Russian Federation Blvd, Toul Kork, Phnom Penh', true, CURRENT_TIMESTAMP - INTERVAL '40 days', CURRENT_TIMESTAMP - INTERVAL '40 days'),
(6, 'Tela Mart Convenience Stores', '012300006', 'supply@kampucheatela.com', 'National Road 1, Chbar Ampov, Phnom Penh', true, CURRENT_TIMESTAMP - INTERVAL '38 days', CURRENT_TIMESTAMP - INTERVAL '38 days'),
(7, 'Circle K Express Cambodia', '012300007', 'merchandise@circlek.com.kh', 'St. 51, BKK1, Chamkar Mon, Phnom Penh', true, CURRENT_TIMESTAMP - INTERVAL '35 days', CURRENT_TIMESTAMP - INTERVAL '35 days'),
(8, 'Sunrise International School', '012300008', 'admin@sunrise-school.edu.kh', 'St. 337, Toul Kork, Phnom Penh', true, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '30 days'),
(9, 'Borey Peng Huoth Facility Office', '012300009', 'facilities@penghuoth.com', 'Grand Star Platinum, National Road 1, Phnom Penh', true, CURRENT_TIMESTAMP - INTERVAL '25 days', CURRENT_TIMESTAMP - INTERVAL '25 days'),
(10, 'Grand City Mart (Closed Account)', '012300010', 'operations@grandcitymart.com', 'St. 1986, Sen Sok, Phnom Penh', false, CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP - INTERVAL '15 days');

SELECT setval('customers_id_seq', (SELECT MAX(id) FROM customers));

-- =============================================================================
-- 6. PRODUCTS (products)
-- Includes:
--   - Normal In-Stock
--   - Low-Stock (stock <= minStock, stock > 0)
--   - Out-of-Stock (stock = 0, minStock > 0)
--   - Inactive / Discontinued (active = false)
--   - No Supplier Assigned (supplier_id = NULL)
--   - Zero-Stock Fresh Item for live demo
--   - Varied Units of Measure: PCS, BOX, PACK, BOTTLE, CAN, KG, LITER, DOZEN, CASE, CARTON, BUNDLE, SACK, BAG, PALLET, ROLL, SET, PAIR, TRAY
-- =============================================================================
INSERT INTO products (id, sku, name, description, category_id, supplier_id, unit_of_measure, unit_price, stock_quantity, minimum_stock, active, version, created_at, updated_at) VALUES
-- Electronics
(1, 'TECH-MBP-14', 'MacBook Pro 14" M3 Pro 18GB/512GB', 'Space Black, 14.2-inch Liquid Retina XDR display, Apple M3 Pro chip.', 1, 1, 'PCS', 1999.00, 12, 5, true, 2, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '5 days'),
(2, 'TECH-LOG-MX3', 'Logitech MX Master 3S Wireless Mouse', 'Quiet clicks, 8K DPI sensor on glass tracking, USB-C fast charging.', 1, 1, 'PCS', 99.00, 45, 10, true, 2, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '4 days'),
(3, 'TECH-DELL-U27', 'Dell UltraSharp 27" 4K USB-C Hub Monitor (U2723QE)', 'IPS Black technology, 4K UHD 3840 x 2160, 90W Power Delivery.', 1, 1, 'PCS', 580.00, 2, 6, true, 2, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),
(4, 'TECH-USB-C5', 'Anker PowerLine USB-C Fast Charging Cable 5-Pack', 'Braided nylon USB-C to USB-C 60W fast charge cables, 1.8m length.', 1, 1, 'BUNDLE', 24.50, 0, 15, true, 2, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '1 day'),
(5, 'TECH-IPH-13P', 'Apple iPhone 13 Pro 256GB Graphite (Discontinued)', 'Discontinued model retained for warranty replacement records.', 1, 1, 'PCS', 699.00, 0, 0, false, 0, CURRENT_TIMESTAMP - INTERVAL '45 days', CURRENT_TIMESTAMP - INTERVAL '10 days'),

-- Beverages
(6, 'BEV-COKE-330', 'Coca-Cola Original Taste 330ml Cans', 'Classic refreshing cola beverage in 330ml aluminum cans.', 2, 2, 'CAN', 0.65, 480, 100, true, 2, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '3 days'),
(7, 'BEV-WTR-500', 'Angkor Puro Natural Mineral Water 500ml', 'Purified natural mineral drinking water in 500ml PET bottle.', 2, 2, 'BOTTLE', 0.35, 850, 200, true, 2, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '7 days'),
(8, 'BEV-STING-CS', 'Sting Red Energy Drink 330ml (Case of 24)', 'High energy strawberry carbonated beverage in wholesale case pack of 24 cans.', 2, 2, 'CASE', 13.50, 5, 20, true, 2, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '1 day'),
(9, 'BEV-MILK-12L', 'Anchor Full Cream UHT Milk 1L (Carton of 12)', '100% pure New Zealand cows milk, 12 x 1-liter aseptic bricks carton.', 2, 2, 'CARTON', 22.00, 28, 10, true, 2, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '3 days'),

-- Food & Snacks
(10, 'FOOD-MAMA-PK', 'Mama Instant Noodles Tom Yum Shrimp 5-Pack', 'Authentic spicy & sour Thai shrimp broth noodles, 5 packs per bundle.', 3, 3, 'PACK', 2.10, 180, 50, true, 2, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '6 days'),
(11, 'FOOD-TUNA-CAN', 'Ayam Brand Tuna Flakes in Olive Oil 185g', 'Wild-caught tuna chunks steeped in virgin olive oil, rich in Omega-3.', 3, 3, 'CAN', 1.85, 0, 40, true, 2, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),
(12, 'FOOD-RICE-50K', 'Malys Angkor Premium Jasmine Rice 50kg Sack', 'Award-winning Cambodian fragrant jasmine rice, grade A export quality.', 3, 3, 'SACK', 42.00, 35, 15, true, 2, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '8 days'),
(13, 'FOOD-EGG-TRAY', 'Fresh Farm Brown Eggs Large Grade (Tray of 30)', 'Grade-A organic free-range farm eggs in standard cardboard tray of 30.', 3, 3, 'TRAY', 4.80, 8, 25, true, 2, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '1 day'),

-- Office Supplies
(14, 'OFF-PPR-A4', 'Double A Premium Copy Paper A4 80gsm (Box of 5 Reams)', 'High quality jam-free multipurpose copier paper, 500 sheets x 5 reams (2500 sheets).', 4, 4, 'BOX', 26.50, 65, 20, true, 2, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '9 days'),
(15, 'OFF-PEN-BLU', 'Deli Retractable Gel Pen 0.5mm Blue (Box of 12)', 'Smooth writing quick-dry blue ink gel pens with rubberized comfort grip.', 4, 4, 'DOZEN', 5.40, 110, 30, true, 2, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '4 days'),
(16, 'OFF-ORG-DESK', 'Deli Mesh Metal 4-Tier Desk Document Organizer', 'Sturdy black steel wire mesh horizontal tray desktop organizer.', 4, 4, 'SET', 16.00, 3, 10, true, 2, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),

-- Health & Hygiene
(17, 'MED-MSK-50P', 'Medical 4-Ply Surgical Face Mask (Pack of 50)', 'BFE >= 99% bacterial filtration efficiency with soft elastic ear loops.', 5, 5, 'PACK', 3.20, 240, 50, true, 2, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '11 days'),
(18, 'MED-SAN-500', 'SafeGuard Antibacterial Hand Gel Sanitizer 500ml', '75% ethyl alcohol instant hand sanitizer with pump dispenser.', 5, 5, 'BOTTLE', 2.80, 95, 30, true, 2, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '6 days'),
(19, 'MED-ALC-5L', 'Medical Grade 70% Isopropyl Alcohol 5-Liter Gallon', 'High purity surface disinfectant and cleaning alcohol for clinics and facilities.', 5, 5, 'LITER', 14.50, 4, 15, true, 2, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '3 days'),

-- Hardware & Tools
(20, 'TOOL-DRL-20V', 'Total Cordless Brushless Impact Drill 20V Li-Ion', 'Heavy duty brushless motor, 60Nm torque, 2x 2.0Ah batteries and charger case.', 6, 6, 'SET', 85.00, 22, 8, true, 2, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '7 days'),
(21, 'TOOL-GLV-PRO', 'Heavy-Duty Nitrile Coated Work Gloves (Pair)', 'Abrasion-resistant breathable nylon liner with oil-grip textured palm.', 6, 6, 'PAIR', 2.20, 140, 50, true, 2, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '5 days'),
(22, 'TOOL-WRN-42', 'Total 42-Piece Combination Spanner & Socket Tool Set', 'Chrome vanadium steel socket wrench set in heavy duty blow-molded case.', 6, 6, 'SET', 48.00, 0, 5, true, 2, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '1 day'),

-- Furniture
(23, 'FURN-CHR-ERG', 'Ergonomic High-Back Mesh Executive Office Chair', 'Adjustable lumbar support, 3D armrests, breathable Korean mesh, Class 4 gas lift.', 7, 7, 'PCS', 175.00, 16, 5, true, 2, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '10 days'),
(24, 'FURN-RCK-IND', 'Heavy-Duty 4-Tier Metal Warehouse Storage Rack 200kg/Shelf', 'Industrial powder-coated blue and orange boltless steel shelving unit (200x60x200cm).', 7, 7, 'SET', 110.00, 2, 4, true, 2, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '3 days'),

-- Packaging Materials
(25, 'PACK-BOX-MED', 'Corrugated Double-Wall Shipping Carton Box (40x30x30cm)', 'Heavy duty brown kraft corrugated cardboard box for e-commerce dispatch.', 8, 8, 'CARTON', 1.25, 550, 100, true, 2, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '4 days'),
(26, 'PACK-TPE-ROL', 'Heavy-Duty Acrylic Clear Packaging Tape (48mm x 100m)', 'Strong adhesive transparent sealing tape roll for warehouse carton packing.', 8, 8, 'ROLL', 1.10, 160, 50, true, 2, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '5 days'),
(27, 'PACK-PLT-WOD', 'Standard Heat-Treated Euro Wooden Pallet (1200x800mm)', 'IPPC ISPM-15 certified 4-way entry heavy industrial timber pallet.', 8, 8, 'PALLET', 18.00, 25, 10, true, 2, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '8 days'),
(28, 'PACK-BAG-TRH', 'Heavy-Duty Industrial Black Trash Bags 90x120cm (Roll)', 'Tough puncture-resistant LDPE extra-large garbage liners, 25 bags per roll.', 8, 8, 'BAG', 6.50, 48, 20, true, 2, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),

-- Zero-Stock Fresh Item (No supplier, ready for live demo stock-in!)
(29, 'DEMO-NEW-ITEM', 'New Unstocked Product for Live Demo', 'Freshly created catalog entry with zero initial stock and zero transaction history. Ready to demonstrate live Stock-In and Stock-Out on stage.', 1, NULL, 'PCS', 49.99, 0, 5, true, 0, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day');

SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));

-- =============================================================================
-- 7. STOCK TRANSACTIONS (stock_transactions)
-- Strictly preserves: quantityBefore +/- quantity = quantityAfter
-- Timestamp spread across the last 30 days for rich date-range report testing.
-- Audit trail: Admin (1), Manager (2), Staff (3), Sopheak (4).
-- =============================================================================
INSERT INTO stock_transactions (id, product_id, transaction_type, quantity, quantity_before, quantity_after, note, created_by, created_at) VALUES
-- Product 1: MacBook Pro 14" (final: 12)
(1, 1, 'STOCK_IN', 15, 0, 15, 'Initial container shipment PO-2026-001', 1, CURRENT_TIMESTAMP - INTERVAL '20 days'),
(2, 1, 'STOCK_OUT', 3, 15, 12, 'Delivered 3 units to Paragon International University', 3, CURRENT_TIMESTAMP - INTERVAL '5 days'),

-- Product 2: Logitech Mouse (final: 45)
(3, 2, 'STOCK_IN', 60, 0, 60, 'Distributor batch stock receiving #PP-992', 2, CURRENT_TIMESTAMP - INTERVAL '18 days'),
(4, 2, 'STOCK_OUT', 15, 60, 45, 'Sales order SO-883 to Aeon Mall tech zone', 3, CURRENT_TIMESTAMP - INTERVAL '4 days'),

-- Product 3: Dell 4K Monitor (final: 2, LOW STOCK)
(5, 3, 'STOCK_IN', 10, 0, 10, 'Import shipment invoice #INV-771', 1, CURRENT_TIMESTAMP - INTERVAL '15 days'),
(6, 3, 'STOCK_OUT', 8, 10, 2, 'Dispatched 8 units for RUPP computer lab upgrade', 4, CURRENT_TIMESTAMP - INTERVAL '2 days'),

-- Product 4: Anker USB-C Cable (final: 0, OUT OF STOCK)
(7, 4, 'STOCK_IN', 30, 0, 30, 'Initial cable inventory intake', 2, CURRENT_TIMESTAMP - INTERVAL '25 days'),
(8, 4, 'STOCK_OUT', 30, 30, 0, 'Bulk delivery order to Chip Mong retail shop', 3, CURRENT_TIMESTAMP - INTERVAL '1 day'),

-- Product 6: Coca-Cola Cans (final: 480)
(9, 6, 'STOCK_IN', 600, 0, 600, 'Factory delivery direct from Coca-Cola bottling plant', 3, CURRENT_TIMESTAMP - INTERVAL '14 days'),
(10, 6, 'STOCK_OUT', 120, 600, 480, 'Consignment delivery to Circle K Express', 4, CURRENT_TIMESTAMP - INTERVAL '3 days'),

-- Product 7: Mineral Water 500ml (final: 850)
(11, 7, 'STOCK_IN', 1000, 0, 1000, 'Pallet delivery batch #WTR-902', 3, CURRENT_TIMESTAMP - INTERVAL '22 days'),
(12, 7, 'STOCK_OUT', 150, 1000, 850, 'School cafeteria weekly supply to Sunrise School', 3, CURRENT_TIMESTAMP - INTERVAL '7 days'),

-- Product 8: Sting Energy Drink (final: 5, LOW STOCK)
(13, 8, 'STOCK_IN', 40, 0, 40, 'Monthly bulk warehouse replenishment', 2, CURRENT_TIMESTAMP - INTERVAL '12 days'),
(14, 8, 'STOCK_OUT', 35, 40, 5, 'Dispatched 35 cases to Tela Mart retail stations', 3, CURRENT_TIMESTAMP - INTERVAL '1 day'),

-- Product 9: Anchor Milk Carton (final: 28)
(15, 9, 'STOCK_IN', 50, 0, 50, 'Cold chain dairy shipment received', 4, CURRENT_TIMESTAMP - INTERVAL '10 days'),
(16, 9, 'STOCK_OUT', 22, 50, 28, 'Delivered to Lucky Supermarket dairy section', 3, CURRENT_TIMESTAMP - INTERVAL '3 days'),

-- Product 10: Mama Noodles 5-Pack (final: 180)
(17, 10, 'STOCK_IN', 250, 0, 250, 'Warehouse PO-409 container intake', 3, CURRENT_TIMESTAMP - INTERVAL '16 days'),
(18, 10, 'STOCK_OUT', 70, 250, 180, 'Bulk distribution to university mini-mart', 4, CURRENT_TIMESTAMP - INTERVAL '6 days'),

-- Product 11: Ayam Canned Tuna (final: 0, OUT OF STOCK)
(19, 11, 'STOCK_IN', 80, 0, 80, 'Initial stock receiving PO-2026-088', 1, CURRENT_TIMESTAMP - INTERVAL '28 days'),
(20, 11, 'STOCK_OUT', 80, 80, 0, 'Complete stock clearance order by Aeon Supermarket', 3, CURRENT_TIMESTAMP - INTERVAL '2 days'),

-- Product 12: Malys Angkor Jasmine Rice (final: 35)
(21, 12, 'STOCK_IN', 50, 0, 50, 'Rice mill harvest delivery batch #R-2026', 2, CURRENT_TIMESTAMP - INTERVAL '20 days'),
(22, 12, 'STOCK_OUT', 15, 50, 35, 'Institutional catering order to RUPP student cafeteria', 3, CURRENT_TIMESTAMP - INTERVAL '8 days'),

-- Product 13: Fresh Farm Eggs (final: 8, LOW STOCK)
(23, 13, 'STOCK_IN', 60, 0, 60, 'Daily farm delivery batch #EGG-41', 4, CURRENT_TIMESTAMP - INTERVAL '5 days'),
(24, 13, 'STOCK_OUT', 52, 60, 8, 'Restaurant wholesale fulfillment to Lucky Express', 3, CURRENT_TIMESTAMP - INTERVAL '1 day'),

-- Product 14: Double A Copy Paper (final: 65)
(25, 14, 'STOCK_IN', 100, 0, 100, 'Stationery warehouse intake PO-808', 3, CURRENT_TIMESTAMP - INTERVAL '21 days'),
(26, 14, 'STOCK_OUT', 35, 100, 65, 'Quarterly office paper supply to Borey Peng Huoth', 4, CURRENT_TIMESTAMP - INTERVAL '9 days'),

-- Product 15: Deli Gel Pens (final: 110)
(27, 15, 'STOCK_IN', 150, 0, 150, 'Stock receiving from Deli factory consignment', 3, CURRENT_TIMESTAMP - INTERVAL '19 days'),
(28, 15, 'STOCK_OUT', 40, 150, 110, 'Supplied to Paragon IU administrative office', 3, CURRENT_TIMESTAMP - INTERVAL '4 days'),

-- Product 16: Desk Organizer (final: 3, LOW STOCK)
(29, 16, 'STOCK_IN', 20, 0, 20, 'Desk organizer shipment received', 4, CURRENT_TIMESTAMP - INTERVAL '15 days'),
(30, 16, 'STOCK_OUT', 17, 20, 3, 'Corporate furnishing delivery to Chip Mong Tower', 3, CURRENT_TIMESTAMP - INTERVAL '2 days'),

-- Product 17: Medical Face Masks (final: 240)
(31, 17, 'STOCK_IN', 300, 0, 300, 'Medical logistics container delivery #MED-102', 2, CURRENT_TIMESTAMP - INTERVAL '24 days'),
(32, 17, 'STOCK_OUT', 60, 300, 240, 'Delivered to Sunrise International School health room', 3, CURRENT_TIMESTAMP - INTERVAL '11 days'),

-- Product 18: Hand Gel Sanitizer (final: 95)
(33, 18, 'STOCK_IN', 120, 0, 120, 'Warehouse delivery batch #SAN-55', 4, CURRENT_TIMESTAMP - INTERVAL '17 days'),
(34, 18, 'STOCK_OUT', 25, 120, 95, 'Campus hygiene refill order for RUPP facilities', 3, CURRENT_TIMESTAMP - INTERVAL '6 days'),

-- Product 19: Isopropyl Alcohol 5L (final: 4, LOW STOCK)
(35, 19, 'STOCK_IN', 25, 0, 25, 'Direct chemical supply shipment from Medix', 1, CURRENT_TIMESTAMP - INTERVAL '13 days'),
(36, 19, 'STOCK_OUT', 21, 25, 4, 'Disinfectant quota dispatch for Borey Peng Huoth', 3, CURRENT_TIMESTAMP - INTERVAL '3 days'),

-- Product 20: Total Cordless Drill (final: 22)
(37, 20, 'STOCK_IN', 30, 0, 30, 'Total Tools official distributor delivery #DRL-301', 2, CURRENT_TIMESTAMP - INTERVAL '23 days'),
(38, 20, 'STOCK_OUT', 8, 30, 22, 'Workshop maintenance equipment dispatch', 4, CURRENT_TIMESTAMP - INTERVAL '7 days'),

-- Product 21: Work Gloves Pair (final: 140)
(39, 21, 'STOCK_IN', 200, 0, 200, 'Safety inventory initial stock-in', 3, CURRENT_TIMESTAMP - INTERVAL '18 days'),
(40, 21, 'STOCK_OUT', 60, 200, 140, 'Supplied to Aeon logistics warehouse team', 3, CURRENT_TIMESTAMP - INTERVAL '5 days'),

-- Product 22: Combination Spanner Set (final: 0, OUT OF STOCK)
(41, 22, 'STOCK_IN', 15, 0, 15, 'Batch tool import shipment #WRN-88', 1, CURRENT_TIMESTAMP - INTERVAL '27 days'),
(42, 22, 'STOCK_OUT', 15, 15, 0, 'Emergency tools dispatch to maintenance service crew', 4, CURRENT_TIMESTAMP - INTERVAL '1 day'),

-- Product 23: Executive Office Chair (final: 16)
(43, 23, 'STOCK_IN', 25, 0, 25, 'Furniture container unloading #FUR-02', 2, CURRENT_TIMESTAMP - INTERVAL '26 days'),
(44, 23, 'STOCK_OUT', 9, 25, 16, 'Office fit-out order for Paragon IU faculty lounge', 3, CURRENT_TIMESTAMP - INTERVAL '10 days'),

-- Product 24: Warehouse Storage Rack (final: 2, LOW STOCK)
(45, 24, 'STOCK_IN', 8, 0, 8, 'Factory direct heavy rack delivery', 1, CURRENT_TIMESTAMP - INTERVAL '16 days'),
(46, 24, 'STOCK_OUT', 6, 8, 2, 'Installed at Chip Mong branch stockroom', 4, CURRENT_TIMESTAMP - INTERVAL '3 days'),

-- Product 25: Shipping Carton Box (final: 550)
(47, 25, 'STOCK_IN', 800, 0, 800, 'Packaging materials bulk replenishment', 3, CURRENT_TIMESTAMP - INTERVAL '20 days'),
(48, 25, 'STOCK_OUT', 250, 800, 550, 'Warehouse e-commerce dispatch consumption', 3, CURRENT_TIMESTAMP - INTERVAL '4 days'),

-- Product 26: Packaging Tape Roll (final: 160)
(49, 26, 'STOCK_IN', 200, 0, 200, 'Sealing tape stock delivery batch #TPE-12', 4, CURRENT_TIMESTAMP - INTERVAL '15 days'),
(50, 26, 'STOCK_OUT', 40, 200, 160, 'Fulfillment packing stations dispatch', 3, CURRENT_TIMESTAMP - INTERVAL '5 days'),

-- Product 27: Euro Wooden Pallet (final: 25)
(51, 27, 'STOCK_IN', 30, 0, 30, 'Timber warehouse delivery #PLT-55', 2, CURRENT_TIMESTAMP - INTERVAL '22 days'),
(52, 27, 'STOCK_OUT', 5, 30, 25, 'Allocated to external logistics loading bay', 4, CURRENT_TIMESTAMP - INTERVAL '8 days'),

-- Product 28: Heavy Trash Bags (final: 48)
(53, 28, 'STOCK_IN', 60, 0, 60, 'Cleaning & packaging supply intake', 3, CURRENT_TIMESTAMP - INTERVAL '11 days'),
(54, 28, 'STOCK_OUT', 12, 60, 48, 'Dispatched to facilities maintenance team', 3, CURRENT_TIMESTAMP - INTERVAL '2 days');

SELECT setval('stock_transactions_id_seq', (SELECT MAX(id) FROM stock_transactions));

-- =============================================================================
-- VERIFICATION LOG SUMMARY
-- =============================================================================
DO $$
DECLARE
    v_users INT;
    v_categories INT;
    v_suppliers INT;
    v_customers INT;
    v_products INT;
    v_transactions INT;
BEGIN
    SELECT count(*) INTO v_users FROM app_users;
    SELECT count(*) INTO v_categories FROM categories;
    SELECT count(*) INTO v_suppliers FROM suppliers;
    SELECT count(*) INTO v_customers FROM customers;
    SELECT count(*) INTO v_products FROM products;
    SELECT count(*) INTO v_transactions FROM stock_transactions;

    RAISE NOTICE '==================================================';
    RAISE NOTICE 'DEMO DATA INITIALIZATION COMPLETED SUCCESSFULLY!';
    RAISE NOTICE 'Users:              %', v_users;
    RAISE NOTICE 'Categories:         %', v_categories;
    RAISE NOTICE 'Suppliers:          %', v_suppliers;
    RAISE NOTICE 'Customers:          %', v_customers;
    RAISE NOTICE 'Products:           %', v_products;
    RAISE NOTICE 'Stock Transactions: %', v_transactions;
    RAISE NOTICE '==================================================';
END $$;

# ==============================================================================
# Populate All Missing Product Images
# 1. Copies to MuMuSharedFolder\Pictures with readable product names
# 2. Copies to uploads\images\<uuid>.jpg
# 3. Updates database products table with image_url
# ==============================================================================

$brainDir = "C:\Users\M16\.gemini\antigravity-cli\brain\0a41c306-8a71-4ed5-873b-16fc83644840"
$mumuDir = "C:\Users\M16\OneDrive\Documents\MuMuSharedFolder\Pictures"
$uploadDir = "D:\SETEC\Year4\SpringBoot\Final\Folder Project\Inventory Management\uploads\images"

if (-not (Test-Path $mumuDir)) {
    New-Item -ItemType Directory -Path $mumuDir -Force | Out-Null
}
if (-not (Test-Path $uploadDir)) {
    New-Item -ItemType Directory -Path $uploadDir -Force | Out-Null
}

# Mapping: Product ID => @{ SourceFile, FriendlyName }
$productMap = @{
    2  = @{ Source = "logitech_mx_master_3s_1793554767212.jpg";    Name = "Logitech MX Master 3S Wireless Mouse.png" }
    7  = @{ Source = "angkor_puro_water_1793554859827.jpg";        Name = "Angkor Puro Natural Mineral Water 500ml.png" }
    8  = @{ Source = "sting_red_energy_1793554879092.jpg";          Name = "Sting Red Energy Drink 330ml (Case of 24).png" }
    9  = @{ Source = "anchor_uht_milk_1793554895807.jpg";          Name = "Anchor Full Cream UHT Milk 1L (Carton of 12).png" }
    10 = @{ Source = "mama_tomyum_noodles_1793554875102.jpg";      Name = "Mama Instant Noodles Tom Yum Shrimp 5-Pack.png" }
    11 = @{ Source = "ayam_brand_tuna_1793554886367.jpg";          Name = "Ayam Brand Tuna Flakes in Olive Oil 185g.png" }
    12 = @{ Source = "malys_angkor_jasmine_rice_1793554897215.jpg";Name = "Malys Angkor Premium Jasmine Rice 50kg Sack.png" }
    13 = @{ Source = "fresh_farm_eggs_tray_1793554907567.jpg";     Name = "Fresh Farm Brown Eggs Large Grade (Tray of 30).png" }
    14 = @{ Source = "double_a_copy_paper_1793554872335.jpg";      Name = "Double A Premium Copy Paper A4 80gsm (Box of 5 Reams).png" }
    15 = @{ Source = "deli_gel_pens_blue_1793554896340.jpg";       Name = "Deli Retractable Gel Pen 0.5mm Blue (Box of 12).png" }
    16 = @{ Source = "deli_mesh_desk_organizer_1793554914284.jpg";  Name = "Deli Mesh Metal 4-Tier Desk Document Organizer.png" }
    17 = @{ Source = "medical_face_mask_pack.jpg";                 Name = "Medical 4-Ply Surgical Face Mask (Pack of 50).png" }
    18 = @{ Source = "safeguard_hand_sanitizer.jpg";               Name = "SafeGuard Antibacterial Hand Gel Sanitizer 500ml.png" }
    19 = @{ Source = "isopropyl_alcohol_gallon.jpg";               Name = "Medical Grade 70% Isopropyl Alcohol 5-Liter Gallon.png" }
    20 = @{ Source = "total_cordless_drill_20v_1793554882440.jpg";  Name = "Total Cordless Brushless Impact Drill 20V Li-Ion.png" }
    21 = @{ Source = "heavy_duty_work_gloves_1793554894048.jpg";    Name = "Heavy-Duty Nitrile Coated Work Gloves (Pair).png" }
    23 = @{ Source = "ergonomic_mesh_office_chair_1793554906024.jpg"; Name = "Ergonomic High-Back Mesh Executive Office Chair.png" }
    24 = @{ Source = "metal_warehouse_storage_rack.jpg";           Name = "Heavy-Duty 4-Tier Metal Warehouse Storage Rack 200kg Shelf.png" }
    25 = @{ Source = "corrugated_shipping_box.jpg";                Name = "Corrugated Double-Wall Shipping Carton Box (40x30x30cm).png" }
    26 = @{ Source = "clear_packaging_tape.jpg";                   Name = "Heavy-Duty Acrylic Clear Packaging Tape (48mm x 100m).png" }
    27 = @{ Source = "euro_wooden_pallet.jpg";                     Name = "Standard Heat-Treated Euro Wooden Pallet (1200x800mm).png" }
    28 = @{ Source = "industrial_black_trash_bags.jpg";            Name = "Heavy-Duty Industrial Black Trash Bags 90x120cm (Roll).png" }
}

$dbName = if ($env:DB_NAME) { $env:DB_NAME } else { "inventory_management_db" }
$dbUser = if ($env:DB_USER) { $env:DB_USER } else { "postgres" }
$dbHost = if ($env:DB_HOST) { $env:DB_HOST } else { "localhost" }
$dbPort = if ($env:DB_PORT) { $env:DB_PORT } else { "5432" }
if (-not $env:PGPASSWORD) { $env:PGPASSWORD = "pgsql" }

Write-Host "Processing 22 product images..." -ForegroundColor Cyan

foreach ($id in ($productMap.Keys | Sort-Object)) {
    $info = $productMap[$id]
    $srcPath = Join-Path $brainDir $info.Source
    $mumuPath = Join-Path $mumuDir $info.Name

    if (-not (Test-Path $srcPath)) {
        Write-Warning "Source image not found for Product $($id) at $srcPath"
        continue
    }

    # 1. Copy to MuMu folder with friendly name
    Copy-Item -Path $srcPath -Destination $mumuPath -Force

    # 2. Copy to uploads\images with UUID
    $uuid = [System.Guid]::NewGuid().ToString()
    $ext = [System.IO.Path]::GetExtension($info.Source)
    $uploadFileName = "$uuid$ext"
    $uploadPath = Join-Path $uploadDir $uploadFileName
    Copy-Item -Path $srcPath -Destination $uploadPath -Force

    # 3. Update database
    $imageUrl = "http://localhost:8080/uploads/images/$uploadFileName"
    $sql = "UPDATE products SET image_url = '$imageUrl' WHERE id = $id;"
    psql -h $dbHost -p $dbPort -U $dbUser -d $dbName -c "$sql" | Out-Null

    Write-Host "Product $id ($($info.Name)): uploaded & mapped -> $imageUrl" -ForegroundColor Green
}

Write-Host "`nAll 22 product images successfully copied to MuMuSharedFolder and uploaded into database!" -ForegroundColor Green
